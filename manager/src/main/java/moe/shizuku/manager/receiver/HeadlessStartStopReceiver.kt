package moe.shizuku.manager.receiver

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbStarter
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
                    tryEnsureWirelessAdb(context)
                    val port = ShizukuSettings.getTcpPort()
                    HeadlessLogger.i("Start", "Starting via ADB on port $port")
                    AdbStarter.startDirect(context, port)
                    setResult(0, "STARTING", null)
                } else {
                    ShizukuReceiverStarter.start(context, forceStart = true)
                    setResult(0, "STARTING", null)
                }
            }
            ACTION_HEADLESS_STOP -> {
                HeadlessLogger.i("Stop", "Headless stop requested")
                if (!ShizukuStateMachine.isRunning()) {
                    setResult(2, "NOT_RUNNING", null)
                    return
                }
                ShizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
                runCatching { Shizuku.exit() }
                setResult(0, "STOPPING", null)
            }
        }
    }

    private fun tryEnsureWirelessAdb(context: Context) {
        if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
            HeadlessLogger.w("Start", "WRITE_SECURE_SETTINGS not granted")
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
        } catch (e: Exception) {
            HeadlessLogger.e("Start", "Failed to enable wireless ADB", e)
        }
    }

    companion object {
        val ACTION_HEADLESS_START = "${BuildConfig.APPLICATION_ID}.HEADLESS_START"
        val ACTION_HEADLESS_STOP = "${BuildConfig.APPLICATION_ID}.HEADLESS_STOP"
    }
}
