package moe.shizuku.manager.stealth

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import app.revanced.library.ApkUtils
import app.revanced.library.ApkUtils.applyTo
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherConfig
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.loadPatchesFromDex
import java.io.File
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import moe.shizuku.manager.receiver.PackageInstallReceiver
import moe.shizuku.manager.receiver.PackageUninstallReceiver

private const val TAG = "StealthViewModel"

class StealthPatcherViewModel(application: Application) : AndroidViewModel(application) {

    private val app: Application = getApplication()
    private val appContext = app.applicationContext

    suspend fun patchAndInstall() {
        getApk()
            .patch()
            .sign()
            .install()
    }

    private fun getApk(): File = File(app.applicationInfo.sourceDir)

    private suspend fun File.patch(): File {
        val patchFile = getPatchFile()
        val patches = loadPatchesFromDex(setOf(patchFile))
        val changePackageNamePatch = patches.find { it.name == "Change package name" }

        if (changePackageNamePatch == null) throw IllegalStateException("Change package name patch not found")

        val patcherConfig = PatcherConfig(apkFile = this)
        val patcherResult =
            Patcher(patcherConfig).use { patcher ->
                patcher += setOf(changePackageNamePatch)

                runBlocking {
                    patcher().collect { patchResult ->
                        if (patchResult.exception != null) {
                            Log.e(TAG, "${patchResult.patch} failed", patchResult.exception)
                        } else {
                            Log.i(TAG, "${patchResult.patch} succeeded")
                        }
                    }
                }

                patcher.get()
            }

        patcherResult.applyTo(this)
        return this
    }

    private fun File.sign(): File {
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

        return signedApk
    }

    private fun File.install() {
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
        val receiverIntent = Intent(appContext, PackageInstallReceiver::class.java).apply {
            action = "APP_INSTALL_ACTION"
        }
        val receiverPendingIntent = PendingIntent.getBroadcast(
            appContext,
            sessionId,
            receiverIntent,
            installerFlags
        )
        session.commit(receiverPendingIntent.intentSender)
        session.close()
    }

    private fun uninstall(packageName: String) {
        val packageInstaller: PackageInstaller = appContext.packageManager.packageInstaller
        val receiverIntent = Intent(appContext, PackageUninstallReceiver::class.java).apply {
            action = "APP_UNINSTALL_ACTION"
        }
        val receiverPendingIntent =
            PendingIntent.getBroadcast(appContext, 0, receiverIntent, installerFlags)
        packageInstaller.uninstall(packageName, receiverPendingIntent.intentSender)
    }

    private fun getPatchFile(): File {
        val assetName =
            app.assets.list("")?.firstOrNull { it.endsWith(".rvp") }
                ?: throw IllegalStateException("No .rvp patch file found in assets")

        val patchFile =
            File.createTempFile(
                assetName.substringBeforeLast('.'),
                "." + assetName.substringAfterLast('.'),
                appContext.cacheDir,
            )

        appContext.assets.open(assetName).use { input ->
            patchFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        patchFile.setWritable(false)

        return patchFile
    }

    private fun generateNewPackageName(original: String): String {
        val randomSuffix = (1000..9999).random()
        return "$original.$randomSuffix"
    }

    private val installerFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }

}