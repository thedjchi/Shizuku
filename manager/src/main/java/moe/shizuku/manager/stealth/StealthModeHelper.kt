package moe.shizuku.manager.stealth

import android.content.Context
import app.revanced.patcher.patch.Patch
import app.revanced.patcher.patch.loadPatchesFromJar
import java.io.File

object StealthModeHelper {
    fun getPatchFile(context: Context): File {
        val assetName = "patches.rvp"

        val tempFile =
            File.createTempFile(
                assetName.substringBeforeLast('.'),
                "." + assetName.substringAfterLast('.'),
                context.cacheDir,
            )

        context.assets.open(assetName).use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return tempFile
    }

    fun listAllPatches(context: Context) {
        val patchFile = setOf(getPatchFile(context))
        val patches = loadPatchesFromJar(patchFile)
        android.util.Log.i("StealthModeHelper", "First patch: ${patches.firstOrNull()?.name}")
    }

//     // fun patchAndInstall() {
//     //     getApk()
//     //         .patch()
//     //         .sign()
//     //         .install()
//     // }

    fun getApk(context: Context): File = File(context.applicationInfo.sourceDir)

//     // fun patch()

    private fun generateNewPackageName(original: String): String {
        val randomSuffix = (1000..9999).random()
        return "$original.$randomSuffix"
    }
}
