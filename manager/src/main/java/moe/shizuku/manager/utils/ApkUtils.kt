package moe.shizuku.manager.utils.ApkUtils

import android.util.Log
import com.reandroid.apk.AndroidFrameworks
import com.reandroid.apk.ApkModule
import com.reandroid.archive.ByteInputSource
import com.reandroid.arsc.chunk.TableBlock
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlDocument
import com.reandroid.arsc.chunk.xml.ResXmlElement
import com.reandroid.common.Namespace
import java.io.File
import moe.shizuku.manager.R
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

fun ApkModule.buildAndSign(outFile: File): File {
    Log.i(TAG, "Building new APK")
    val unsignedApk = File(workDir, "unsigned.apk")
    writeApk(unsignedApk)

    Log.i(TAG, "Signing APK")
    val key = ApkSigner.getOrCreateSigningKey(appContext.filesDir)
    ApkSigner.sign(unsignedApk, outFile, key)

    return outFile
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

    val outFile = File(workDir, "signed.apk")
    return module.buildAndSign(outFile)
}

fun createStubApk(pkgName: String): File {
    val outFile = File(appContext.filesDir, "stub.apk")

    val tableBlock = TableBlock()
    val manifest = AndroidManifestBlock()
    val dummyDex = ByteInputSource(ByteArray(0), "classes.dex")

    val module = ApkModule().apply {
        setTableBlock(tableBlock)
        setManifest(manifest)
        add(dummyDex)
    }

    val packageBlock = tableBlock.newPackage(0x7f, pkgName)
    val appName = packageBlock.getOrCreate("", "string", "app_name").apply {
        setValueAsString("${getAppLabel()} Stub")
    }
    val appIcon = packageBlock.getOrCreate("", "drawable", "ic_launcher").apply {
        setValueAsReference(R.drawable.ic_launcher)
    }

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

private fun getAppLabel(): String = app
    .applicationInfo
    .loadLabel(appContext.packageManager)
    .toString()

fun buildApkFilename(): String {
    val pm = appContext.packageManager
    val versionName = pm
        .getPackageInfo(app.packageName, 0)
        .versionName

    val safeLabel = getAppLabel()
        .lowercase()
        .replace("[^a-z0-9._-]".toRegex(), "-")

    return "$safeLabel-$versionName"
}
