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
import androidx.core.app.NotificationCompat
import moe.shizuku.manager.R
import moe.shizuku.manager.utils.ShizukuStateMachine
import moe.shizuku.manager.receiver.ShizukuReceiverStarter
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class WatchdogService : Service() {

    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "shizuku-watchdog") }

    private val listener = Shizuku.OnBinderDeadListener {
        executor.execute { onBinderDead() }
    }

    override fun onCreate() {
        super.onCreate()
        if (isRunning.compareAndSet(false, true)) {
            try {
                Shizuku.addBinderDeadListener(listener)
            } catch (t: Throwable) {
                Log.w(TAG, "Start watchdog failed", t)
                isRunning.set(false)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                buildNotification()
            )
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (isRunning.compareAndSet(true, false)) {
            try {
                Shizuku.removeBinderDeadListener(listener)
            } catch (t: Throwable) {
                Log.w(TAG, "Stop watchdog failed", t)
                isRunning.set(true)
            }
        }
        shutdownExecutor()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onBinderDead() {
        Handler(Looper.getMainLooper()).post {
            val currentState = ShizukuStateMachine.getState()
            when (currentState) {
                ShizukuStateMachine.State.STOPPING -> {
                    ShizukuStateMachine.setState(ShizukuStateMachine.State.STOPPED)
                }
                ShizukuStateMachine.State.RUNNING -> {
                    ShizukuStateMachine.setState(ShizukuStateMachine.State.CRASHED)
                    ShizukuReceiverStarter.start(applicationContext)
                }
                else -> return@post
            }
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
            .setContentTitle("Shizuku watchdog is running")
            .setSmallIcon(R.drawable.ic_system_icon)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "ShizukuWatchdog"
        private const val NOTIFICATION_ID = 1001

        private val isRunning = AtomicBoolean(false)

        @JvmStatic
        fun start(context: Context) {
            context.startForegroundService(Intent(context, WatchdogService::class.java))
        }

        @JvmStatic
        fun stop(context: Context) {
            context.stopService(Intent(context, WatchdogService::class.java))
        }

        @JvmStatic
        fun isRunning(): Boolean = isRunning.get()
    }
}