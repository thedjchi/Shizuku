package moe.shizuku.manager.adb

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.shizuku.manager.MainActivity
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.home.HomeActivity
import rikka.core.ktx.unsafeLazy
import java.net.ConnectException

@TargetApi(Build.VERSION_CODES.R)
class AdbPairingService : Service() {
    companion object {
        const val NOTIFICATION_CHANNEL = "adb_pairing"
        const val NOTIFICATION_ID = 1

        private const val tag = "AdbPairingService"

        private const val replyRequestId = 1
        private const val stopRequestId = 2
        private const val retryRequestId = 3
        private const val launchRequestId = 4
        private const val startRequestId = 5
        private const val startAction = "start"
        private const val stopAction = "stop"
        private const val replyAction = "reply"
        private const val remoteInputResultKey = "paring_code"
        private const val portKey = "paring_code"

        fun startIntent(context: Context): Intent = Intent(context, AdbPairingService::class.java).setAction(startAction)

        private fun stopIntent(context: Context): Intent = Intent(context, AdbPairingService::class.java).setAction(stopAction)

        private fun replyIntent(
            context: Context,
            port: Int,
        ): Intent = Intent(context, AdbPairingService::class.java).setAction(replyAction).putExtra(portKey, port)
    }

    private var adbMdns: AdbMdns? = null

    private val observer =
        Observer<Int> { port ->
            Log.i(tag, "Pairing service port: $port")
            if (port <= 0) return@Observer

            // Since the service could be killed before user finishing input,
            // we need to put the port into Intent
            val notification = createInputNotification(port)

            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        }

    private var started = false

    override fun onCreate() {
        super.onCreate()

        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL,
                getString(R.string.pairing_notification_channel),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setSound(null, null)
                setShowBadge(false)
                setAllowBubbles(false)
            },
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val notification =
            when (intent?.action) {
                startAction -> {
                    onStart()
                }

                replyAction -> {
                    val code = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(remoteInputResultKey) ?: ""
                    val port = intent.getIntExtra(portKey, -1)
                    if (port != -1) {
                        onInput(code.toString(), port)
                    } else {
                        onStart()
                    }
                }

                stopAction -> {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    null
                }

                else -> {
                    return START_NOT_STICKY
                }
            }
        if (notification != null) {
            try {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST,
                )
            } catch (e: Throwable) {
                Log.e(tag, "startForeground failed", e)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    e is ForegroundServiceStartNotAllowedException
                ) {
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onTimeout(startId: Int) {
        Toast.makeText(this, R.string.pairing_timed_out, Toast.LENGTH_SHORT).show()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startSearch() {
        if (started) return
        started = true
        adbMdns = AdbMdns(this, AdbMdns.TLS_PAIRING, observer).apply { start() }
    }

    private fun stopSearch() {
        if (!started) return
        started = false
        adbMdns?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSearch()
    }

    private fun onStart(): Notification {
        startSearch()
        return searchingNotification
    }

    private fun onInput(
        code: String,
        port: Int,
    ): Notification {
        GlobalScope.launch(Dispatchers.IO) {
            val host = "127.0.0.1"

            val key =
                try {
                    AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
                } catch (e: Throwable) {
                    e.printStackTrace()
                    return@launch
                }

            AdbPairingClient(host, port, code, key)
                .runCatching {
                    start()
                }.onFailure {
                    handleResult(false, it)
                }.onSuccess {
                    handleResult(it, null)
                }
        }

        return workingNotification
    }

    private fun handleResult(
        success: Boolean,
        exception: Throwable?,
    ) {
        stopForeground(STOP_FOREGROUND_DETACH)

        val title: String
        val text: String?

        if (success) {
            Log.i(tag, "Pair succeed")

            title = getString(R.string.pairing_successful)
            text = getString(R.string.pairing_successful_message)

            stopSearch()
        } else {
            title = getString(R.string.pairing_failed)

            text =
                when (exception) {
                    is ConnectException -> {
                        getString(R.string.start_error_connection)
                    }

                    is AdbInvalidPairingCodeException -> {
                        getString(R.string.pairing_error_invalid_code)
                    }

                    is AdbKeyException -> {
                        getString(R.string.adb_error_key_store)
                    }

                    else -> {
                        exception?.let { Log.getStackTraceString(it) }
                    }
                }

            if (exception != null) {
                Log.w(tag, "Pair failed", exception)
            } else {
                Log.w(tag, "Pair failed")
            }
        }

        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            Notification
                .Builder(this, NOTIFICATION_CHANNEL)
                .setColor(getColor(R.color.notification))
                .setSmallIcon(R.drawable.ic_system_icon)
                .setContentTitle(title)
                .setContentText(text)
                .apply {
                    if (!success) {
                        addAction(retryNotificationAction)
                    } else {
                        setContentIntent(launchPendingIntent)
                        addAction(startNotificationAction)
                        setAutoCancel(true)
                    }
                }.build(),
        )
        stopSelf()
    }

    private val launchIntent by unsafeLazy {
        Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
        }
    }

    private val launchPendingIntent by unsafeLazy {
        PendingIntent.getActivity(
            this,
            launchRequestId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private val startNotificationAction by unsafeLazy {
        val startIntent =
            Intent(launchIntent)
                .putExtra(HomeActivity.EXTRA_START_SERVICE_VIA_WADB, true)

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                startRequestId,
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        Notification.Action
            .Builder(
                null,
                getString(R.string.start),
                pendingIntent,
            ).build()
    }

    private val stopNotificationAction by unsafeLazy {
        val pendingIntent =
            PendingIntent.getService(
                this,
                stopRequestId,
                stopIntent(this),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    0
                },
            )

        Notification.Action
            .Builder(
                null,
                getString(R.string.pairing_stop_searching),
                pendingIntent,
            ).build()
    }

    private val retryNotificationAction by unsafeLazy {
        val pendingIntent =
            PendingIntent.getService(
                this,
                retryRequestId,
                startIntent(this),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_IMMUTABLE
                } else {
                    0
                },
            )

        Notification.Action
            .Builder(
                null,
                getString(R.string.retry),
                pendingIntent,
            ).build()
    }

    private val replyNotificationAction by unsafeLazy {
        val remoteInput =
            RemoteInput.Builder(remoteInputResultKey).run {
                setLabel(getString(R.string.pairing_enter_code))
                build()
            }

        val pendingIntent =
            PendingIntent.getForegroundService(
                this,
                replyRequestId,
                replyIntent(this, -1),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                },
            )

        Notification.Action
            .Builder(
                null,
                getString(R.string.pairing_enter_code),
                pendingIntent,
            ).addRemoteInput(remoteInput)
            .build()
    }

    private fun replyNotificationAction(port: Int): Notification.Action {
        // Ensure pending intent is created
        val action = replyNotificationAction

        PendingIntent.getForegroundService(
            this,
            replyRequestId,
            replyIntent(this, port),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            },
        )

        return action
    }

    private val searchingNotification by unsafeLazy {
        Notification
            .Builder(this, NOTIFICATION_CHANNEL)
            .setColor(getColor(R.color.notification))
            .setSmallIcon(R.drawable.ic_system_icon)
            .setContentTitle(getString(R.string.pairing_searching))
            .addAction(stopNotificationAction)
            .build()
    }

    private fun createInputNotification(port: Int): Notification =
        Notification
            .Builder(this, NOTIFICATION_CHANNEL)
            .setColor(getColor(R.color.notification))
            .setContentTitle(getString(R.string.pairing_service_found))
            .setSmallIcon(R.drawable.ic_system_icon)
            .addAction(replyNotificationAction(port))
            .build()

    private val workingNotification by unsafeLazy {
        Notification
            .Builder(this, NOTIFICATION_CHANNEL)
            .setColor(getColor(R.color.notification))
            .setContentTitle(getString(R.string.pairing_in_progress))
            .setSmallIcon(R.drawable.ic_system_icon)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
