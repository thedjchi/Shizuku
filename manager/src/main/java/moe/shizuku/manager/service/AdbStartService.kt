package moe.shizuku.manager.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbClient
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.AdbMdns
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import moe.shizuku.manager.starter.Starter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AdbStartService : Service() {

    lateinit var notification: Notification
    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        @RequiresApi(Build.VERSION_CODES.R)
        override fun onAvailable(network: Network) { startShizuku(this@AdbStartService) }
    }

    override fun onBind(intent: Intent?): IBinder? { return null }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate() {
        super.onCreate()

        startForeground()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val cm = getSystemService(ConnectivityManager::class.java)
        cm.registerNetworkCallback(networkRequest, networkCallback)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == CANCEL_ACTION) { stopSelf(); return START_NOT_STICKY }
        if (intent?.action == RESTORE_ACTION) startForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        val cm = getSystemService(ConnectivityManager::class.java)
        cm.unregisterNetworkCallback(networkCallback)
        super.onDestroy()
    }

    @SuppressLint("StringFormatInvalid")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun startShizuku(context: Context) {

        val cr = context.contentResolver
        Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
        Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
        Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
        CoroutineScope(Dispatchers.IO).launch {
            val latch = CountDownLatch(1)
            val adbMdns = AdbMdns(context, AdbMdns.TLS_CONNECT) { port ->
                if (port <= 0) return@AdbMdns
                try {

                    val keystore = PreferenceAdbKeyStore(ShizukuSettings.getPreferences())
                    val key = AdbKey(keystore, "shizuku")
                    val client = AdbClient("127.0.0.1", port, key)

                    client.connect()
                    client.shellCommand(Starter.internalCommand, null)
                    client.close()

                    Settings.Global.putInt(cr, "adb_wifi_enabled", 0)

                    val toastMsg = context.getString(
                        R.string.home_status_service_is_running,
                        context.getString(R.string.app_name)
                    )
                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()

                    stopSelf()
                } catch (_: Exception) {}
                latch.countDown()
            }
            if (Settings.Global.getInt(cr, "adb_wifi_enabled", 0) == 1) {
                adbMdns.start()
                latch.await(5, TimeUnit.SECONDS)
                adbMdns.stop()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun startForeground() {

        notification = showNotification()

        ServiceCompat.startForeground(
            this,
            SERVICE_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else 0
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showNotification() : Notification {

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.wadb_notification_title),
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)

        val cancelIntent = Intent(this, AdbStartService::class.java).setAction(CANCEL_ACTION)
        val cancelPendingIntent = PendingIntent.getForegroundService(
            this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val restoreIntent = Intent(this, AdbStartService::class.java).setAction(RESTORE_ACTION)
        val restorePendingIntent = PendingIntent.getForegroundService(
            this, 0, restoreIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val wifiIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        val wifiPendingIntent = PendingIntent.getActivity(
            this, 0, wifiIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentTitle(getString(R.string.wadb_notification_title))
            .setContentText(getString(R.string.wadb_notification_content))
            .setOngoing(true)
            .setSilent(true)
            .addAction(R.drawable.ic_close_24, getString(android.R.string.cancel), cancelPendingIntent)
            .setDeleteIntent(restorePendingIntent)
            .setContentIntent(wifiPendingIntent)
            .build()
    }


    companion object {
        const val TAG = "AdbStartService"
        const val CHANNEL_ID = "AdbStartService"
        const val SERVICE_ID = 1447

        const val CANCEL_ACTION = "cancel_action"
        const val RESTORE_ACTION = "restore_action"
    }
}