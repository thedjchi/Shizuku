package moe.shizuku.manager.receiver

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.HeadlessLogger
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.shizuku.Shizuku

class HeadlessStartStopReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_HEADLESS_START -> {
                HeadlessLogger.i("Start", "Headless start requested (version ${BuildConfig.VERSION_NAME})")
                val launchMode = ShizukuSettings.getLastLaunchMode()
                if (launchMode == ShizukuSettings.LaunchMethod.ADB || launchMode == ShizukuSettings.LaunchMethod.UNKNOWN) {
                    HeadlessLogger.i("Start", "Launch mode=${launchMode}, attempting ADB start")
                    tryEnsureWirelessAdb(context)
                    val port = ShizukuSettings.getTcpPort()
                    HeadlessLogger.i("Start", "Starting via ADB on port $port")
                    AdbStarter.startDirect(context, port)
                    setResult(0, "STARTING", null)
                } else {
                    HeadlessLogger.i("Start", "Launch mode=${launchMode}, delegating to ShizukuReceiverStarter")
                    ShizukuReceiverStarter.start(context, forceStart = true)
                    setResult(0, "STARTING", null)
                }
            }
            ACTION_HEADLESS_STOP -> {
                HeadlessLogger.i("Stop", "Headless stop requested")
                if (!ShizukuStateMachine.isRunning()) {
                    HeadlessLogger.w("Stop", "Server not running, nothing to stop")
                    setResult(2, "NOT_RUNNING", null)
                    return
                }
                ShizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
                runCatching {
                    Shizuku.exit()
                    HeadlessLogger.i("Stop", "Server stop command sent")
                }.onFailure { e ->
                    HeadlessLogger.e("Stop", "Failed to send stop command", e)
                }
                setResult(0, "STOPPING", null)
            }
            ACTION_HEADLESS_STATUS -> {
                val state = ShizukuStateMachine.get()
                val stateLabel = state.name
                val binderAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

                val adbTcpPort = EnvironmentUtils.getAdbTcpPort()
                val adbWifi = runCatching {
                    Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0)
                }.getOrDefault(0)
                val adbUsb = runCatching {
                    Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0)
                }.getOrDefault(0)

                val adbParts = mutableListOf<String>()
                if (adbUsb != 0) adbParts.add("USB:on")
                if (adbWifi != 0) adbParts.add("WiFi:${if (adbTcpPort > 0) adbTcpPort else "?"}")
                if (adbParts.isEmpty()) adbParts.add("off")
                val adbSummary = adbParts.joinToString(" ")

                val summary = "$stateLabel (binder=$binderAlive, ADB: $adbSummary, v${BuildConfig.VERSION_NAME})"
                val logPath = HeadlessLogger.getLogPath() ?: "unavailable"

                val extras = Bundle().apply {
                    putString("state", stateLabel)
                    putBoolean("binder_alive", binderAlive)
                    putInt("adb_tcp_port", adbTcpPort)
                    putInt("adb_wifi_enabled", adbWifi)
                    putInt("adb_enabled", adbUsb)
                    putString("version_name", BuildConfig.VERSION_NAME)
                    putInt("version_code", BuildConfig.VERSION_CODE)
                    putString("log_path", logPath)
                }

                HeadlessLogger.i("Status", summary)
                setResult(state.ordinal, summary, extras)
            }
        }
    }

    private fun tryEnsureWirelessAdb(context: Context) {
        if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            HeadlessLogger.w("Start", "WRITE_SECURE_SETTINGS not granted, cannot enable wireless ADB")
            return
        }
        try {
            val cr = context.contentResolver
            if (Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0) == 0) {
                Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
                HeadlessLogger.i("Start", "Enabled USB ADB")
            }
            if (Settings.Global.getInt(cr, "adb_wifi_enabled", 0) == 0) {
                Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
                HeadlessLogger.i("Start", "Enabled wireless ADB")
            }
        } catch (e: SecurityException) {
            HeadlessLogger.w("Start", "WRITE_SECURE_SETTINGS denied")
        } catch (e: Exception) {
            HeadlessLogger.e("Start", "Failed to enable wireless ADB", e)
        }
    }

    companion object {
        val ACTION_HEADLESS_START = "${BuildConfig.APPLICATION_ID}.HEADLESS_START"
        val ACTION_HEADLESS_STOP = "${BuildConfig.APPLICATION_ID}.HEADLESS_STOP"
        val ACTION_HEADLESS_STATUS = "${BuildConfig.APPLICATION_ID}.HEADLESS_STATUS"
    }
}
