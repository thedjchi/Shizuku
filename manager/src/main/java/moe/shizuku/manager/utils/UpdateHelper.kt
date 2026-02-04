package moe.shizuku.manager.utils.UpdateHelper

import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.ShizukuApplication
import moe.shizuku.manager.utils.ApkUtils.installPackage
import okhttp3.OkHttpClient
import okhttp3.Request

private val appContext = ShizukuApplication.appContext
private val client = OkHttpClient()
private val gson = Gson()

data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val commit: Int,
) : Comparable<Version> {
    override fun compareTo(other: Version): Int =
        compareValuesBy(
            this,
            other,
            { it.major },
            { it.minor },
            { it.patch },
            { it.commit },
        )

    companion object {
        fun parse(tag: String): Version? {
            val regex = Regex("""v(\d+)\.(\d+)\.(\d+)\.r(\d+)""")
            val match = regex.find(tag) ?: return null
            val (major, minor, patch, commit) = match.destructured
            return Version(
                major.toInt(),
                minor.toInt(),
                patch.toInt(),
                commit.toInt(),
            )
        }
    }
}

enum class UpdateChannel {
    STABLE,
    BETA
}

data class Release(
    val version: Version,
    val filename: String,
    val url: String,
)

data class GitHubRelease(
    val tag_name: String,
    val prerelease: Boolean,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val name: String,
    val browser_download_url: String
)

private lateinit var latestRelease: Release

suspend fun checkAndInstallUpdates() {
    if (isUpdateAvailable()) {
        update()
    } else {
        Toast.makeText(appContext, "The latest version is already installed", Toast.LENGTH_LONG).show()
    }
}

suspend fun isUpdateAvailable(): Boolean {
    try {
        val latest = fetchLatestRelease().version ?: return false
        val current = Version.parse(BuildConfig.VERSION_NAME) ?: return false
        return latest > current
    } catch (e: Exception) {
        Toast.makeText(appContext, "Unable to check for updates", Toast.LENGTH_LONG).show()
        return false
    }
}

suspend fun update() {
    if (!::latestRelease.isInitialized && !isUpdateAvailable()) return

    Toast.makeText(appContext, "Downloading update...", Toast.LENGTH_LONG).show()
    val apk = latestRelease.download()

    if (apk == null) {
        Toast.makeText(appContext, "Failed to download update", Toast.LENGTH_LONG).show()
        return
    }

    appContext.installPackage(apk)
}

private suspend fun fetchLatestRelease(): Release =
    withContext(Dispatchers.IO) {
        val updateChannel = UpdateChannel.STABLE

        val url = "https://api.github.com/repos/thedjchi/Shizuku/releases"
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Couldn't fetch releases")

        val releases = gson.fromJson(body, Array<GitHubRelease>::class.java).toList()
        val filtered =
            if (updateChannel == UpdateChannel.BETA) releases
            else releases.filter { !it.prerelease }

        filtered
            .mapNotNull { release ->
                val version = Version.parse(release.tag_name)
                    ?: return@mapNotNull null
                val asset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                    ?: return@mapNotNull null

                Release(
                    version = version,
                    filename = asset.name,
                    url = asset.browser_download_url
                )
            }
            .maxByOrNull { it.version }
            ?.also { latestRelease = it }
            ?: throw Exception("No valid releases found")
    }

private suspend fun Release.download(): File =
    withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()

        val apkFile = File(appContext.cacheDir, filename)
        apkFile.outputStream().use { out ->
            response.body?.byteStream()?.copyTo(out)
        }

        apkFile
    }
