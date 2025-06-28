package moe.shizuku.manager.service

import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
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
    lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                Toast.makeText(applicationContext, R.string.notification_service_starting, Toast.LENGTH_SHORT).show()

                val cr = applicationContext.contentResolver
                Settings.Global.putInt(cr, "adb_wifi_enabled", 1)
                Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
                Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
                CoroutineScope(Dispatchers.IO).launch {
                    val latch = CountDownLatch(1)
                    val adbMdns = AdbMdns(applicationContext, AdbMdns.TLS_CONNECT) { port ->
                        if (port <= 0) return@AdbMdns
                        try {

                            val keystore = PreferenceAdbKeyStore(ShizukuSettings.getPreferences())
                            val key = AdbKey(keystore, "shizuku")
                            val client = AdbClient("127.0.0.1", port, key)
                            client.connect()
                            client.shellCommand(Starter.internalCommand, null)
                            client.close()

                            Settings.Global.putInt(cr, "adb_wifi_enabled", 0)

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
        }

        val cm = getSystemService(ConnectivityManager::class.java)
        cm.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        val cm = getSystemService(ConnectivityManager::class.java)
        cm.unregisterNetworkCallback(networkCallback)

        super.onDestroy()
    }
}