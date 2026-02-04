package moe.shizuku.manager.utils.ApkUtils

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import com.reandroid.apk.AndroidFrameworks
import com.reandroid.apk.ApkModule
import com.reandroid.archive.ByteInputSource
import com.reandroid.arsc.chunk.TableBlock
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlDocument
import com.reandroid.arsc.chunk.xml.ResXmlElement
import com.reandroid.common.Namespace
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuApplication
import moe.shizuku.manager.utils.ApkSigner
import java.io.File

private const val TAG = "ApkUtils"

const val ORIGINAL_PACKAGE_NAME = "moe.shizuku.privileged.api"

private val app = ShizukuApplication.application
private val appContext = ShizukuApplication.appContext

val workDir by lazy {
    File(appContext.cacheDir, "patcher").also {
        it.deleteRecursively()
        it.mkdirs()
    }
}

fun File.changePackageName(newPkgName: String): File {
    Log.i(TAG, "Loading APK")
    val module = ApkModule.loadApkFile(this)
    val manifest = module.androidManifest

    Log.i(TAG, "Changing package name")
    val oldPkgName = manifest.packageName
    manifest.packageName = newPkgName

    Log.i(TAG, "Updating provider authorities")
    val providers =
        manifest
            .getApplicationElement()
            .getElements("provider")
    for (provider in providers) {
        val attr = provider.searchAttribute(Namespace.URI_ANDROID, "authorities")
        val auth = attr?.valueAsString ?: continue

        if (auth.startsWith(oldPkgName)) {
            val newAuth = auth.replace(oldPkgName, newPkgName)
            attr.setValueAsString(newAuth)
        }
    }

    val outFile = File(workDir, "signed.apk")
    return module.buildAndSign(outFile)
}

fun createStubApk(pkgName: String): File {
    Log.i(TAG, "Initializing APK framework")
    val outFile = File(appContext.filesDir, "stub.apk")

    val tableBlock = TableBlock()
    val manifest = AndroidManifestBlock()
    val dummyDex = ByteInputSource(ByteArray(0), "classes.dex")

    val module =
        ApkModule().apply {
            setTableBlock(tableBlock)
            setManifest(manifest)
            add(dummyDex)
        }

    Log.i(TAG, "Adding APK resources")
    val packageBlock = tableBlock.newPackage(0x7f, pkgName)
    val appName =
        packageBlock.getOrCreate("", "string", "app_name").apply {
            setValueAsString("${getAppLabel()} Stub")
        }
    val appIcon =
        packageBlock.getOrCreate("", "drawable", "ic_launcher").apply {
            setValueAsReference(R.drawable.ic_launcher)
        }

    Log.i(TAG, "Creating manifest")
    manifest.apply {
        setPackageName(pkgName)
        setVersionCode(1)
        setVersionName("1.0.0")
        setApplicationLabel(appName.getResourceId())
        setIconResourceId(appIcon.getResourceId())
        setTargetSdkVersion(app.applicationInfo.targetSdkVersion)
        setMinSdkVersion(app.applicationInfo.minSdkVersion)
    }

    return module.buildAndSign(outFile)
}

private fun ApkModule.buildAndSign(outFile: File): File {
    Log.i(TAG, "Building new APK")
    val unsignedApk = File(workDir, "unsigned.apk")
    writeApk(unsignedApk)

    Log.i(TAG, "Signing APK")
    val key = ApkSigner.getOrCreateSigningKey(appContext.filesDir)
    ApkSigner.sign(unsignedApk, outFile, key)

    return outFile
}

fun getAppLabel(): String =
    app
        .applicationInfo
        .loadLabel(appContext.packageManager)
        .toString()

fun getVersionName(): String =
    appContext
        .packageManager
        .getPackageInfo(app.packageName, 0)
        .versionName
        .orEmpty()

fun buildApkFilename(): String {
    val safeLabel =
        getAppLabel()
            .lowercase()
            .replace("[^a-z0-9._-]".toRegex(), "-")

    return "$safeLabel-${getVersionName()}"
}

fun Context.installPackage(
    apk: File,
    cb: ((Boolean, String?) -> Unit)? = null
) {
    val installer = appContext.packageManager.packageInstaller

    val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
    val sessionId = installer.createSession(sessionParams)
    val session = installer.openSession(sessionId)

    apk.inputStream().use { input ->
        session.openWrite("base.apk", 0, apk.length()).use { output ->
            input.copyTo(output)
            session.fsync(output)
        }
    }

    val pendingIntent = createInstallerPendingIntent(sessionId, cb)

    session.commit(pendingIntent.intentSender)
    session.close()
}

fun Context.uninstallPackage(
    pkgName: String,
    cb: ((Boolean, String?) -> Unit)? = null,
) {
    val installer = appContext.packageManager.packageInstaller
    val pendingIntent = createInstallerPendingIntent(0, cb)
    installer.uninstall(pkgName, pendingIntent.intentSender)
}

private var callback: ((Boolean, String?) -> Unit)? = null

val installerReceiver =
    object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    val confirmationIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                    if (confirmationIntent != null) {
                        context.startActivity(confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }

                else -> {
                    val isSuccess = (status == PackageInstaller.STATUS_SUCCESS)
                    callback?.invoke(isSuccess, msg)
                    context.unregisterReceiver(this)
                }
            }
        }
    }

private fun Context.createInstallerPendingIntent(
    sessionId: Int,
    cb: ((Boolean, String?) -> Unit)? = null
): PendingIntent {
    callback = cb

    val installerAction = "${app.packageName}.INSTALLER_RESULT"
    val filter =
        IntentFilter().apply {
            addAction(installerAction)
        }
    registerReceiver(installerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)

    val callbackIntent =
        Intent(installerAction).apply {
            setPackage(app.packageName)
        }

    val flags =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

    return PendingIntent.getBroadcast(
        this,
        sessionId,
        callbackIntent,
        flags,
    )
}
