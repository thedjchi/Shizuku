package moe.shizuku.manager.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
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
    lateinit var cm: ConnectivityManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()

        cm = getSystemService(ConnectivityManager::class.java)

        if (checkWifiConnected()) {
            startShizuku(applicationContext)
            return
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (context == null) return

                if (checkWifiConnected()) {
                    startShizuku(context)
                    unregisterReceiver(this)
                }
            }
        }
        val intentFilter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)

        applicationContext.registerReceiver(receiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun startShizuku(context: Context) {
        Toast.makeText(context, R.string.notification_service_starting, Toast.LENGTH_SHORT).show()

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

    private fun checkWifiConnected() : Boolean {
        return cm.getNetworkCapabilities(cm.activeNetwork)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }
}