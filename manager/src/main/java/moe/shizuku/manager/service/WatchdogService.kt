package moe.shizuku.manager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import moe.shizuku.manager.R
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class WatchdogService : Service() {
    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "shizuku-watchdog") }
    private val listener = Shizuku.OnBinderDeadListener {
        Log.i(TAG, "Listener called")
        executor.execute { onBinderDead() }
    }

    private var registered = false

    override fun onCreate() {
        super.onCreate()
        if (!registered) {
            try {
                Shizuku.addBinderDeadListener(listener)
                registered = true
            } catch (t: Throwable) {
                Toast.makeText(this, "Start watchdog failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            } else {
                0
            })
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "Stopping watchdog")
        if (registered) {
            try {
                Shizuku.removeBinderDeadListener(listener)
                registered = false
            } catch (t: Throwable) {
                Toast.makeText(this, "Remove watchdog failed", Toast.LENGTH_SHORT).show()
            }
        }
        shutdownExecutor()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onBinderDead() {
        Log.i(TAG, "Inside executor")
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, "Shizuku stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shutdownExecutor() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) executor.shutdownNow()
        } catch (_: InterruptedException) {
            executor.shutdownNow()
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "shizuku_watchdog"
        val channelName = "Shizuku Watchdog"

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Shizuku Watchdog")
            .setContentText("Monitoring binder health")
            .setSmallIcon(R.drawable.ic_system_icon)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "ShizukuWatchdog"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            context.startForegroundService(Intent(context, WatchdogService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WatchdogService::class.java))
        }
    }
}