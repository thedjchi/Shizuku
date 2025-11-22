package moe.shizuku.manager.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.lifecycle.asFlow
import androidx.work.*
import java.io.EOFException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbMdns
import moe.shizuku.manager.adb.AdbStarter
import moe.shizuku.manager.receiver.ShizukuReceiverStarter
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.ShizukuStateMachine

class AdbStartWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        try {
            if (EnvironmentUtils.isWifiRequired()) {
                ShizukuReceiverStarter.showNotification(
                    applicationContext,
                    applicationContext.getString(R.string.wadb_notification_wifi_found)
                )
            }

            val cr = applicationContext.contentResolver

            Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
            Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)

            val tcpPort = EnvironmentUtils.getAdbTcpPort()
            if (tcpPort > 0 && !ShizukuSettings.getTcpMode()) {
                AdbStarter.stopTcp(applicationContext, tcpPort)
            }

            val port = tcpPort.takeIf { !EnvironmentUtils.isWifiRequired() } ?: withTimeout(15000) {
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
            Starter.waitForBinder()

            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(ShizukuReceiverStarter.NOTIFICATION_ID)

            return Result.success()
        } catch (e: Exception) {
            if (ShizukuStateMachine.update() == ShizukuStateMachine.State.RUNNING)
                return Result.success()

            if (e !is CancellationException && e !is EOFException)
                showErrorNotification(applicationContext, e)

            ShizukuReceiverStarter.showNotification(applicationContext)
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
            "&body=v" + Uri.encode(
                context.packageManager.getPackageInfo(context.packageName, 0).versionName + "\n\n" +
                e.stackTraceToString()
            )
        )

        val emailIntent = Intent(Intent.ACTION_SENDTO, uri)
        var msgNotif = ""
        if (emailIntent.resolveActivity(context.packageManager) != null) {
            val emailPendingIntent = PendingIntent.getActivity(
                context, 0, emailIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            nb.setContentIntent(emailPendingIntent)
            nb.setAutoCancel(true)
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
        fun enqueue(context: Context) {
            val cb = Constraints.Builder()
            if (EnvironmentUtils.isWifiRequired())
                cb.setRequiredNetworkType(NetworkType.UNMETERED)
            val constraints = cb.build()

            val request = OneTimeWorkRequestBuilder<AdbStartWorker>()
                .setConstraints(constraints)
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