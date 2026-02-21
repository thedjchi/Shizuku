package moe.shizuku.manager.adb

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shizuku.manager.MainActivity
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.AdbPairingClient
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import moe.shizuku.manager.core.extensions.*
import moe.shizuku.manager.home.HomeActivity
import moe.shizuku.manager.utils.EnvironmentUtils
import java.net.ConnectException

class AdbPairingAccessibilityService : AccessibilityService() {
    var port: Int? = null
    var password: String? = null

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()

        if (!(EnvironmentUtils.isTelevision() && EnvironmentUtils.isTlsSupported())) {
            disableSelf()
            return
        }

        val intent =
            Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                )
                putExtra(HomeActivity.EXTRA_SHOW_PAIRING_DIALOG, true)
            }
        startActivity(intent)

        handler.postDelayed({
            toast(R.string.pairing_timed_out)
            disableSelf()
        }, 180_000)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (port != null && password != null) return

        if ((event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) == 0) return
        val text = event.source?.text ?: return

        val ipPortRegex = Regex("""(?:\d{1,3}\.){3}\d{1,3}:(\d{2,5})""")
        val passwordRegex = Regex("""\d{6}""")

        ipPortRegex
            .matchEntire(text)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?.let { port = it }
        passwordRegex
            .matchEntire(text)
            ?.value
            ?.let { password = it }

        if (port != null && password != null) {
            val port = port!!
            val password = password!!

            var toastMsg = getString(R.string.pairing_failed)
            GlobalScope.launch(Dispatchers.IO) {
                val context = this@AdbPairingAccessibilityService
                val host = "127.0.0.1"

                val key =
                    try {
                        AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
                    } catch (e: Throwable) {
                        toastMsg = getString(R.string.adb_error_key_store)
                        return@launch
                    }

                AdbPairingClient(host, port, password, key)
                    .runCatching {
                        start()
                    }.onFailure {
                        when (it) {
                            is ConnectException -> toastMsg = getString(R.string.start_error_connection)
                            is AdbInvalidPairingCodeException -> toastMsg = getString(R.string.pairing_error_invalid_code)
                            is AdbKeyException -> toastMsg = getString(R.string.adb_error_key_store)
                        }
                    }.onSuccess {
                        if (it) {
                            toastMsg =
                                "${getString(
                                    R.string.pairing_successful,
                                )}. ${getString(R.string.pairing_successful_message)}"

                            val intent =
                                Intent(this@AdbPairingAccessibilityService, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }
                            startActivity(intent)
                        }
                    }
                withContext(Dispatchers.Main) {
                    context.toast(toastMsg, long = true)
                }
                disableSelf()
            }
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        handler.removeCallbacksAndMessages(null)
        return super.onUnbind(intent)
    }
}
