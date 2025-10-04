package moe.shizuku.manager.adb

import android.content.Context
import android.provider.Settings
import java.io.EOFException
import java.net.SocketException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.ShizukuSettings.Keys.*
import moe.shizuku.manager.adb.AdbClient
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import moe.shizuku.manager.starter.Starter

object AdbStarter {
    suspend fun startAdb(context: Context, port: Int, log: ((String) -> Unit)? = null) {
        suspend fun AdbClient.runCommand(cmd: String) {
            command(cmd) { log?.invoke(String(it)) }
        }

        log?.invoke("Starting with wireless adb...\n")

        val prefs = ShizukuSettings.getPreferences()
        
        val key = runCatching { AdbKey(PreferenceAdbKeyStore(prefs), "shizuku") }
            .getOrElse {
                if (it is CancellationException) throw it
                else throw AdbKeyException(it)
            }

        var activePort = port
        val tcpMode = prefs.getBoolean(KEY_TCP_MODE, true)
        val tcpPort = prefs.getString(KEY_TCP_PORT, "5555")?.toIntOrNull() ?: 5555
        if (tcpMode && activePort != tcpPort) {
            log?.invoke("Connecting on port $activePort...")
            AdbClient("127.0.0.1", activePort, key).use { client ->
                client.connect()
                activePort = tcpPort
                log?.invoke("restarting in TCP mode port: $activePort")
                runCatching {
                    client.command("tcpip:$activePort")
                }.onFailure { if (it !is EOFException && it !is SocketException) throw it } // Expected when ADB restarts in TCP mode
            }

            Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 0)
        }  
        
        log?.invoke("Connecting on port $activePort...\n")
        AdbClient("127.0.0.1", activePort, key).use { client ->
            var delayTime = 1000L
            val maxAttempts = 5
            for (attempt in 1..maxAttempts) {
                try {
                    delay(delayTime)
                    client.connect()
                    break
                } catch (e: Exception) {
                    if (attempt == maxAttempts || e is CancellationException) throw e
                    delayTime += 1000
                }
            }
            client.runCommand("shell:${Starter.internalCommand}")
        }
    }
}