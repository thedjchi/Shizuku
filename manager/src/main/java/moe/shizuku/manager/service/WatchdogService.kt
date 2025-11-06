package moe.shizuku.manager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import moe.shizuku.manager.R
import moe.shizuku.manager.MainActivity
import moe.shizuku.manager.receiver.ShizukuReceiverStarter
import moe.shizuku.manager.utils.ShizukuStateMachine
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
                NOTIFICATION_ID_WATCHDOG,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(
                NOTIFICATION_ID_WATCHDOG,
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
                    showCrashNotification()
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
        val channelName = "Watchdog"

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.watchdog_running))
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showCrashNotification() {
        val channelId = CRASH_CHANNEL_ID
        val channelName = "Crash Reports"

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        nm.createNotificationChannel(channel)

        val learnMoreIntent = Intent(Intent.ACTION_VIEW).apply {
            setData(Uri.parse("https://github.com/thedjchi/Shizuku/wiki#shizuku-keeps-stopping-randomly"))
        }
        val learnMorePendingIntent = PendingIntent.getActivity(this, 0, learnMoreIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val disableIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, applicationContext.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, WatchdogService.CRASH_CHANNEL_ID)
        }
        val disablePendingIntent = PendingIntent.getActivity(this, 0, disableIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Shizuku crashed recently")
            .setContentText("Learn about potential fixes")
            .setSmallIcon(R.drawable.ic_system_icon)
            .addAction(0, "Learn More", learnMorePendingIntent)
            .addAction(0, "Turn Off Alerts", disablePendingIntent)
            .build()

        nm.notify(NOTIFICATION_ID_CRASH, notification)
    }

    companion object {
        private const val TAG = "ShizukuWatchdog"
        private const val NOTIFICATION_ID_WATCHDOG = 1001
        private const val NOTIFICATION_ID_CRASH = 1002
        const val CRASH_CHANNEL_ID = "crash_reports"

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