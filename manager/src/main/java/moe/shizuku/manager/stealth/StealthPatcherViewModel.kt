package moe.shizuku.manager.stealth

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.revanced.library.ApkUtils
import app.revanced.library.ApkUtils.applyTo
import app.revanced.patcher.InternalApi
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherConfig
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.loadPatchesFromDex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import moe.shizuku.manager.receiver.PackageInstallReceiver
import moe.shizuku.manager.receiver.PackageUninstallReceiver
import java.io.File
import java.io.InputStream

private const val TAG = "StealthViewModel"

class StealthPatcherViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val app: Application = getApplication()
    private val appContext = app.applicationContext

    private val _success = MutableLiveData<Boolean>()
    val success: LiveData<Boolean> = _success

    private val sb = StringBuilder()
    private val _log = MutableLiveData<String>()
    val log = _log as LiveData<String>

    private val workDir: File by lazy {
        createWorkDir()
    }

    override fun onCleared() {
        super.onCleared()
        cleanCacheDir()
    }

    fun patchAndInstall(
        packageName: String? = null,
        uninstallAfter: Boolean = true,
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                getApk()
                    .patch(packageName)
                    .sign()
            }.onFailure {
                _success.postValue(false)
                log(Log.getStackTraceString(it))
                return@launch
            }
        }
    }

    private fun getApk(): File =
        File(app.applicationInfo.sourceDir).inputStream().copyToDir(workDir, "base.apk").also {
            it.setReadable(true)
            it.setWritable(true)
        }

    private suspend fun File.patch(packageName: String? = null): File {
        log("Loading APK")

        val tmpDir = File(workDir, "revanced-temporary-files")
        tmpDir.mkdirs()

        val aaptPath = "${appContext.applicationInfo.nativeLibraryDir}/libaapt2.so"
        val patcherConfig =
            PatcherConfig(
                apkFile = this,
                aaptBinaryPath = aaptPath,
                temporaryFilesPath = tmpDir,
                frameworkFileDirectory = tmpDir.path,
            )
        val patcher = Patcher(patcherConfig)

        log("Loading patch")

        val patchFile = getPatchFile()
        val patches = loadPatchesFromDex(setOf(patchFile))

        val changePackageNamePatch =
            patches.find { it.name == "Change package name" }
                ?: throw IllegalStateException("\"Change package name\" patch not found")

        changePackageNamePatch.apply {
            options["packageName"] = packageName ?: app.packageName.appendRandomSuffix()
            options["updateProviders"] = true
        }

        log("Changing package name to ${changePackageNamePatch.options["packageName"]}")

        val patcherResult =
            patcher.use { patcher ->
                patcher += setOf(changePackageNamePatch)

                runBlocking {
                    patcher().collect { patchResult ->
                        patchResult.exception?.let { throw it }
                    }
                }

                log("Building new APK")

                @OptIn(InternalApi::class)
                patcher.get()
            }

        patcherResult.applyTo(this)
        return this
    }

    private fun File.sign(): File {
        log("Signing APK")
        val signedApk = this // TO-DO: MAKE NEW FILE

        // TO-DO GET KEYSTORE

        // ApkUtils.signApk(
        //     this,
        //     signedApk,
        //     "ReVanced",
        //     ApkUtils.KeyStoreDetails(
        //         keyStoreFile,
        //         keystorePassword,
        //         "alias",
        //         keystorePassword
        //     )
        // )

        log("Success")
        _success.postValue(true)
        return signedApk
    }

    private fun getPatchFile(): File {
        val assetName =
            app.assets.list("")?.firstOrNull { it.endsWith(".rvp") }
                ?: throw IllegalStateException("No .rvp patch file found in assets")

        val patchFile = appContext.assets.open(assetName).copyToDir(workDir, assetName)
        patchFile.setWritable(false)

        return patchFile
    }

    private fun createWorkDir(): File {
        val root = appContext.cacheDir
        val workDir =
            File.createTempFile("tmp-", "", root).apply {
                delete()
                mkdirs()
            }

        return workDir
    }

    private fun cleanCacheDir() {
        appContext.cacheDir.listFiles()?.forEach { file ->
            if ((file.isDirectory && file.name.startsWith("tmp-")) ||
                file.name.startsWith("APKTOOL")
            ) {
                file.deleteRecursively()
            }
        }
    }

    private fun InputStream.copyToDir(
        dir: File,
        filename: String,
    ): File {
        val tempFile = File(dir, filename)

        this.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }

    private fun String.appendRandomSuffix(): String {
        val letters = ('a'..'z')
        val chars = letters + ('0'..'9')

        val first = letters.random()
        val rest = (1..4).map { chars.random() }

        val randomSuffix = (listOf(first) + rest).joinToString("")

        return "$this.$randomSuffix"
    }

    private fun log(line: String) {
        sb.appendLine(line)
        _log.postValue(sb.toString())
    }

    private fun File.install() {
        log("Requesting install")
        val packageInstaller: PackageInstaller = appContext.packageManager.packageInstaller
        val sessionParams =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId: Int = packageInstaller.createSession(sessionParams)
        val session: PackageInstaller.Session = packageInstaller.openSession(sessionId)
        session.use { activeSession ->
            val sessionOutputStream = activeSession.openWrite(app.packageName, 0, -1)
            sessionOutputStream.use { outputStream ->
                this.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        val receiverIntent =
            Intent(appContext, PackageInstallReceiver::class.java).apply {
                action = "APP_INSTALL_ACTION"
            }
        val receiverPendingIntent =
            PendingIntent.getBroadcast(
                appContext,
                sessionId,
                receiverIntent,
                installerFlags,
            )
        session.commit(receiverPendingIntent.intentSender)
        session.close()
    }

    private fun uninstall(packageName: String) {
        log("Requesting uninstall")
        val packageInstaller: PackageInstaller = appContext.packageManager.packageInstaller
        val receiverIntent =
            Intent(appContext, PackageUninstallReceiver::class.java).apply {
                action = "APP_UNINSTALL_ACTION"
            }
        val receiverPendingIntent =
            PendingIntent.getBroadcast(appContext, 0, receiverIntent, installerFlags)
        packageInstaller.uninstall(packageName, receiverPendingIntent.intentSender)
    }

    private val installerFlags =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
}
