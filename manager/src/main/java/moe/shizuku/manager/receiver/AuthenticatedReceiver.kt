package moe.shizuku.manager.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Process
import androidx.core.app.NotificationCompat
import moe.shizuku.manager.MainActivity
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings

abstract class AuthenticatedReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "auth_errors"
        private const val CHANNEL_NAME = "Authentication Errors"
        private const val NOTIFICATION_ID = 1450
        private const val PERMISSION_START_STOP_SERVER = "moe.shizuku.manager.permission.START_STOP_SERVER"
    }

    final override fun onReceive(context: Context, intent: Intent) {
        val authToken = intent.getStringExtra("auth")
        val expectedToken = ShizukuSettings.getAuthToken()

        if (isPrivilegedCaller(context)) {
            onAuthenticated(context, intent)
        } else if (authToken.isNullOrEmpty()) {
            context.notify(
                R.string.notification_auth_missing_title,
                R.string.notification_auth_missing_message
            )
        } else if (authToken != expectedToken) {
            context.notify(
                R.string.notification_auth_invalid_title,
                R.string.notification_auth_invalid_message
            )
        } else {
            onAuthenticated(context, intent)
        }
    }

    private fun isPrivilegedCaller(context: Context): Boolean {
        val callingUid = Binder.getCallingUid()
        return callingUid == Process.SHELL_UID ||
                callingUid == Process.ROOT_UID ||
                context.checkCallingPermission(PERMISSION_START_STOP_SERVER) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun Context.notify(title: Int, message: Int) {
        val titleStr = getString(title)
        val messageStr = getString(message)

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or 
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        val launchPendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(titleStr)
            .setContentText(messageStr)
            .setContentIntent(launchPendingIntent)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_system_icon)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    abstract fun onAuthenticated(context: Context, intent: Intent)
}