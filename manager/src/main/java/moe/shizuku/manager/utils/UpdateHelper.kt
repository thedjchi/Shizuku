package moe.shizuku.manager.utils

import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuApplication
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.utils.ApkUtils.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

object UpdateHelper {
    private val app = ShizukuApplication.application
    private val appContext = ShizukuApplication.appContext

    private val client = OkHttpClient()
    private val gson = Gson()

    data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val commit: Int = 0,
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

        override fun toString(): String =
            if (commit == 0)  "$major.$minor.$patch" else "$major.$minor.$patch.r$commit"

        companion object {
            fun parse(tag: String): Version? {
                val regex = Regex("""(\d+)\.(\d+)\.(\d+)(?:\.r(\d+))?""")
                val match = regex.find(tag) ?: return null
                val (major, minor, patch, commit) = match.destructured
                return Version(
                    major.toInt(),
                    minor.toInt(),
                    patch.toInt(),
                    commit.toIntOrNull() ?: 0
                )
            }
        }
    }

    data class Release(
        val version: Version,
        val filename: String,
        val url: String,
        val digest: String
    )

    data class GitHubRelease(
        val tag_name: String,
        val prerelease: Boolean,
        val assets: List<GitHubAsset>
    )

    data class GitHubAsset(
        val name: String,
        val browser_download_url: String,
        val digest: String
    )

    private lateinit var latestRelease: Release

    suspend fun checkAndInstallUpdates() {
        if (isUpdateAvailable()) {
            update()
        } else {
            Toast
                .makeText(
                    appContext,
                    appContext.getString(R.string.update_latest_installed),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    fun isCheckForUpdatesEnabled(): Boolean = ShizukuSettings.getUpdateMode() != ShizukuSettings.UpdateMode.OFF

    suspend fun isNewUpdateAvailable(): Boolean {
        val lastPromptedVersion =
            Version.parse(ShizukuSettings.getLastPromptedVersion())
                ?: Version.parse(getVersionName())
                ?: return false
        return if (isUpdateAvailable()) latestRelease.version > lastPromptedVersion else false
    }

    suspend fun isUpdateAvailable(): Boolean {
        try {
            val latest = fetchLatestRelease().version ?: return false
            val current = Version.parse(getVersionName()) ?: return false
            return latest > current
        } catch (e: Exception) {
            Toast
                .makeText(
                    appContext,
                    appContext.getString(R.string.update_check_failed),
                    Toast.LENGTH_SHORT,
                ).show()
            return false
        }
    }

    fun updateLastPromptedVersion() = ShizukuSettings.setLastPromptedVersion(latestRelease.version.toString())

    suspend fun update() {
        if (!::latestRelease.isInitialized && !isUpdateAvailable()) return

        Toast
            .makeText(
                appContext,
                appContext.getString(R.string.update_downloading),
                Toast.LENGTH_SHORT,
            ).show()

        val apk =
            latestRelease.download()?.run {
                val pm = appContext.packageManager
                val apkPackageName = pm.getPackageArchiveInfo(
                    this.path, 0
                )?.packageName
                if (app.packageName != apkPackageName) {
                    try {
                        android.util.Log.d("UpdateHelper", "Changing package name from $apkPackageName to ${app.packageName}")
                        changePackageName(app.packageName)
                    } catch (e: Exception) {
                        Toast
                            .makeText(
                                appContext,
                                appContext.getString(R.string.update_failed),
                                Toast.LENGTH_SHORT,
                            ).show()
                        return@update
                    }
                } else {
                    this
                }
            }
        if (apk == null) {
            Toast
                .makeText(
                    appContext,
                    appContext.getString(R.string.update_download_failed),
                    Toast.LENGTH_SHORT,
                ).show()
            return
        }

        appContext.installPackage(apk) { isSuccess, _ ->
            val toastMsg =
                if (isSuccess) appContext.getString(R.string.update_success)
                else appContext.getString(R.string.update_failed)
            Toast.makeText(appContext, toastMsg, Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun fetchLatestRelease(): Release =
        withContext(Dispatchers.IO) {
            val url = "https://api.github.com/repos/thedjchi/Shizuku/releases"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Couldn't fetch releases")

            val releases = gson.fromJson(body, Array<GitHubRelease>::class.java).toList()
            val filtered =
                if (ShizukuSettings.getUpdateMode() == ShizukuSettings.UpdateMode.BETA) {
                    releases
                } else {
                    releases.filter { !it.prerelease }
                }

            filtered
                .mapNotNull { release ->
                    val version =
                        Version.parse(release.tag_name)
                            ?: return@mapNotNull null
                    val asset =
                        release.assets.firstOrNull { it.name.endsWith(".apk") }
                            ?: return@mapNotNull null

                    Release(
                        version = version,
                        filename = asset.name,
                        url = asset.browser_download_url,
                        digest = asset.digest
                    )
                }.maxByOrNull { it.version }
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

            val downloadedDigest = "sha256:" + apkFile.sha256()
            if (downloadedDigest != digest)
                throw SecurityException("Digest of downloaded file does not match the one reported by GitHub")

            apkFile
        }

    fun File.sha256(): String {
        val bytes = readBytes()
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

}
