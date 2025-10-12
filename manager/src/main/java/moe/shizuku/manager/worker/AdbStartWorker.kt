package moe.shizuku.manager.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.io.EOFException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbMdns
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.utils.EnvironmentUtils

class AdbStartWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        try {
            val cr = applicationContext.contentResolver

            Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
            Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)

            val port = EnvironmentUtils.getAdbTcpPort().takeIf { it > 0 } ?: withTimeout(15000) {
                callbackFlow {
                    Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
                    val adbMdns = AdbMdns(applicationContext, AdbMdns.TLS_CONNECT) { port ->
                        if (port > 0) trySend(port)
                    }
                    adbMdns.start()
                    awaitClose {
                        adbMdns.stop()
                    }
                }.first()
            }
            AdbStarter.startAdb(applicationContext, port)

            val notificationId = inputData.getInt("notification_id", -1)
            if (notificationId != -1) {
                val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(notificationId)
            }

            return Result.success()
        } catch (e: Exception) {
            if (e !is CancellationException && e !is EOFException)
                showErrorNotification(applicationContext, e)
            return Result.retry()
        }
    }

    private fun showErrorNotification(context: Context, e: Exception) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.wadb_notification_title),
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)

        val nb = NotificationCompat.Builder(context, CHANNEL_ID)

        val uri = Uri.parse(
            "mailto:" + context.getString(R.string.support_email) +
            "?subject=" + Uri.encode("Error while starting on boot") +
            "&body=" + Uri.encode(e.stackTraceToString())
        )

        val emailIntent = Intent(Intent.ACTION_SENDTO, uri)
        var msgNotif = ""
        if (emailIntent.resolveActivity(context.packageManager) != null) {
            val emailPendingIntent = PendingIntent.getActivity(
                context, 0, emailIntent, PendingIntent.FLAG_IMMUTABLE
            )
            nb.setContentIntent(emailPendingIntent)
            msgNotif = "$e. ${context.getString(R.string.wadb_error_notify_dev)}"
        } else {
            msgNotif = "$e. ${context.getString(R.string.wadb_error_send_email)} ${context.getString(R.string.support_email)}"
        }

        val notification = nb
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentTitle(context.getString(R.string.wadb_error_title))
            .setContentText(msgNotif)
            .setSilent(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msgNotif))
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        fun enqueue(context: Context, isWifiRequired: Boolean, notificationId: Int) {
            val cb = Constraints.Builder()
            if (isWifiRequired)
                cb.setRequiredNetworkType(NetworkType.UNMETERED)
            val constraints = cb.build()

            val inputData = workDataOf("notification_id" to notificationId)

            val request = OneTimeWorkRequestBuilder<AdbStartWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "adb_start_worker",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
        const val CHANNEL_ID = "AdbStartWorker"
        const val NOTIFICATION_ID = 1448
    }
}