package moe.shizuku.manager.receiver

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.topjohnwu.superuser.Shell
import moe.shizuku.manager.R
import moe.shizuku.manager.AppConstants
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.ShizukuSettings.LaunchMethod
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.UserHandleCompat
import moe.shizuku.manager.worker.AdbStartWorker
import rikka.shizuku.Shizuku

object StartShizukuIntentHandler {

    private const val CHANNEL_ID = "AdbStartWorker"
    private const val NOTIFICATION_ID = 1447

    fun handle(context: Context, intent: Intent) {
        if (UserHandleCompat.myUserId() > 0 || Shizuku.pingBinder()) return

        if (ShizukuSettings.getLastLaunchMode() == LaunchMethod.ROOT) {
            rootStart(context)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            && ShizukuSettings.getLastLaunchMode() == LaunchMethod.ADB) {
                if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
                    val isWifiRequired = EnvironmentUtils.getAdbTcpPort().let { it > 0 }
                    AdbStartWorker.enqueue(context, isWifiRequired, NOTIFICATION_ID)
                    showStartupNotification(context, isWifiRequired)
                } else {
                    showPermissionErrorNotification(context)
                }
        } else {
            Log.w(AppConstants.TAG, "No support start on boot")
        }
    }

    fun showStartupNotification(context: Context, isWifiRequired: Boolean) {

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.wadb_notification_title),
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)

        val cancelIntent = Intent(context, BootCancelReceiver::class.java).apply {
            putExtra("notification_id", NOTIFICATION_ID)
        }
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val attemptNowIntent = Intent(context, BootAttemptReceiver::class.java).apply {
            putExtra("notification_id", NOTIFICATION_ID)
            putExtra("is_wifi_required", isWifiRequired)
        }
        val attemptNowPendingIntent = PendingIntent.getBroadcast(
            context, 0, attemptNowIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val restoreIntent = Intent(context, BootRestoreReceiver::class.java).apply {
            putExtra("is_wifi_required", isWifiRequired)
        }
        val restorePendingIntent = PendingIntent.getBroadcast(
            context, 0, restoreIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val wifiIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        val wifiPendingIntent = PendingIntent.getActivity(
            context, 0, wifiIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentTitle(context.getString(R.string.wadb_notification_title))
            .setContentText(context.getString(R.string.wadb_notification_content))
            .setOngoing(true)
            .setSilent(true)
            .addAction(R.drawable.ic_server_restart, context.getString(R.string.wadb_notification_attempt_now), attemptNowPendingIntent)
            .addAction(R.drawable.ic_close_24, context.getString(android.R.string.cancel), cancelPendingIntent)
            .setDeleteIntent(restorePendingIntent)
            .setContentIntent(wifiPendingIntent)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun rootStart(context: Context) {
        if (!Shell.getShell().isRoot) {
            //NotificationHelper.notify(context, AppConstants.NOTIFICATION_ID_STATUS, AppConstants.NOTIFICATION_CHANNEL_STATUS, R.string.notification_service_start_no_root)
            Shell.getCachedShell()?.close()
            return
        }

        Shell.cmd(Starter.internalCommand).exec()
    }

    private fun showPermissionErrorNotification(context: Context) {

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.wadb_notification_title),
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)

        val webpageIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/thedjchi/Shizuku/wiki#shizuku-isnt-starting-on-boot-for-me"))
        val pendingWebpageIntent = PendingIntent.getActivity(
            context, 0, webpageIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val msg = context.getString(R.string.wadb_permission_error_notification_content)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentTitle(context.getString(R.string.wadb_permission_error_notification_title))
            .setContentText(msg)
            .setSilent(true)
            .setContentIntent(pendingWebpageIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }
}