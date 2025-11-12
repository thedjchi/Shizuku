package moe.shizuku.manager.adb

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.pm.PackageManager
import android.content.Context
import android.provider.Settings
import android.widget.Toast
import java.io.EOFException
import java.net.SocketException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbClient
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.shizuku.Shizuku

object AdbStarter {
    suspend fun startAdb(context: Context, port: Int, log: ((String) -> Unit)? = null) {
        suspend fun AdbClient.runCommand(cmd: String) {
            command(cmd) { log?.invoke(String(it)) }
        }

        try {
            ShizukuStateMachine.setState(ShizukuStateMachine.State.STARTING)
            log?.invoke("Starting with wireless adb...")
        
            val key = runCatching { AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku") }
                .getOrElse {
                    if (it is CancellationException) throw it
                    else throw AdbKeyException(it)
                }

            var activePort = port
            val tcpMode = ShizukuSettings.getTcpMode()
            val tcpPort = ShizukuSettings.getTcpPort()
            if (tcpMode && activePort != tcpPort) {
                log?.invoke("\nConnecting on port $activePort...")

                AdbClient("127.0.0.1", activePort, key).use { client ->
                    client.connect()

                    log?.invoke("Successfully connected on port $activePort...")
                    log?.invoke("\nRestarting in TCP mode port: $activePort")

                    activePort = tcpPort
                    runCatching {
                        client.command("tcpip:$activePort")
                    }.onFailure { if (it !is EOFException && it !is SocketException) throw it } // Expected when ADB restarts in TCP mode
                }
            }
        
            log?.invoke("\nConnecting on port $activePort...")

            AdbClient("127.0.0.1", activePort, key).use { client ->
                var delayTime = 0L
                val maxAttempts = 5
                for (attempt in 1..maxAttempts) {
                    try {
                        delay(delayTime)
                        client.connect()
                        break
                    } catch (e: Exception) {
                        if (attempt == maxAttempts || e is CancellationException) {
                            log?.invoke("Failed to connect on port $activePort...")
                            throw e
                        }
                        delayTime += 1000
                    }
                }
                log?.invoke("\nSuccessfully connected on port $activePort...")
            
                client.runCommand("shell:${Starter.internalCommand}")
                ShizukuStateMachine.setState(ShizukuStateMachine.State.RUNNING)
            }
        } catch (e: Exception) {
            ShizukuStateMachine.setState(ShizukuStateMachine.State.STOPPED)
            throw e
        } finally {
            if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED)
                Settings.Global.putInt(context.contentResolver, "adb_wifi_enabled", 0)
        }
    }

    suspend fun stopTcp(context: Context, port: Int) { 
        runCatching {
            ShizukuStateMachine.setState(ShizukuStateMachine.State.STOPPING)
            val key = AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
            AdbClient("127.0.0.1", port, key).use { client ->
                client.connect()
                client.command("usb:")
            }
        }.onFailure {
            if (EnvironmentUtils.getAdbTcpPort() > 0) {
                ShizukuStateMachine.setState(ShizukuStateMachine.State.RUNNING)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.adb_error_stop_tcp), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}