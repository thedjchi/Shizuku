package moe.shizuku.manager.utils.ApkUtils

import android.util.Log
import com.reandroid.apk.ApkModule
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlDocument
import com.reandroid.arsc.chunk.xml.ResXmlElement
import com.reandroid.common.Namespace
import java.io.File
import moe.shizuku.manager.ShizukuApplication
import moe.shizuku.manager.utils.ApkSigner

private const val TAG = "ApkUtils"

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
    val providers = manifest
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

    Log.i(TAG, "Building new APK")
    val unsignedApk = File(workDir, "unsigned.apk")
    module.writeApk(unsignedApk)

    Log.i(TAG, "Signing APK")
    val signedApk = File(workDir, "signed.apk")
    val key = ApkSigner.getOrCreateSigningKey(appContext.filesDir)
    ApkSigner.sign(unsignedApk, signedApk, key)

    return signedApk
}

fun buildApkFilename(): String {
    val pm = appContext.packageManager
    val label = app
        .applicationInfo
        .loadLabel(pm)
        .toString()
    val versionName = pm
        .getPackageInfo(app.packageName, 0)
        .versionName

    val safeLabel = label
        .lowercase()
        .replace("[^a-z0-9._-]".toRegex(), "-")

    return "$safeLabel-$versionName"
}
