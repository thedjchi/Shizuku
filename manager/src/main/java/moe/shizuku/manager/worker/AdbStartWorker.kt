package moe.shizuku.manager.worker

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.Constraints
import androidx.work.workDataOf
import androidx.work.NetworkType
import androidx.work.ListenableWorker.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CompletableDeferred
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbClient
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.AdbMdns
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import moe.shizuku.manager.starter.Starter

class AdbStartWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val cr = applicationContext.contentResolver
            Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
            Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
            Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)

            val shouldCloseFlow = CompletableDeferred<Unit>()

            val portFlow = callbackFlow {
                val adbMdns = AdbMdns(applicationContext, AdbMdns.TLS_CONNECT) { port ->
                    if (port > 0) trySend(port)
                }

                adbMdns.start()

                shouldCloseFlow.await()
                adbMdns.stop()
                close()
            }

            portFlow.collect { port ->
                val keystore = PreferenceAdbKeyStore(ShizukuSettings.getPreferences())
                val key = AdbKey(keystore, "shizuku")
                val client = AdbClient("127.0.0.1", port, key)

                client.connect()
                client.tcpipCommand()
                client.close()

                Settings.Global.putInt(cr, "adb_wifi_enabled", 0)

                delay(1000)

                val tcpipClient = AdbClient("127.0.0.1", 5555, key)

                var delayTime = 1000L
                var connected = false
                repeat(3) {
                    try {
                        tcpipClient.connect()
                        connected = true
                        return@repeat
                    } catch(e: Exception) {
                        delay(delayTime)
                        delayTime *=2
                    }
                }

                if (!connected) throw Exception("Failed to connect over TCP after 3 attempts")

                tcpipClient.shellCommand(Starter.internalCommand, null)
                tcpipClient.close()

                shouldCloseFlow.complete(Unit)
            }

            val toastMsg = applicationContext.getString(
                R.string.home_status_service_is_running,
                applicationContext.getString(R.string.app_name)
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, toastMsg, Toast.LENGTH_SHORT).show()
            }

            val notificationId = inputData.getInt("notification_id", -1)
            if (notificationId != -1) {
                val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(notificationId)
            }

            Result.success()
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, R.string.notification_service_start_failed, Toast.LENGTH_SHORT).show()
            }
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context, notificationId: Int) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()

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
    }
}