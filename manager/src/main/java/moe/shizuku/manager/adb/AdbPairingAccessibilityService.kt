package moe.shizuku.manager.adb

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.provider.Settings
import android.content.ActivityNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shizuku.manager.R
import moe.shizuku.manager.MainActivity
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.AdbPairingClient
import moe.shizuku.manager.home.HomeActivity
import moe.shizuku.manager.utils.EnvironmentUtils
import java.net.ConnectException

class AdbPairingAccessibilityService : AccessibilityService() {

    var port: Int? = null
    var password: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        if (!(EnvironmentUtils.isTelevision() && EnvironmentUtils.isTlsSupported())) {
            Toast.makeText(this, "This service is only supported on TV devices.", Toast.LENGTH_SHORT).show()
            disableSelf()
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(HomeActivity.EXTRA_SHOW_PAIRING_DIALOG, true)
        }
        startActivity(intent)

        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(this, "Shizuku pairing service timed out.", Toast.LENGTH_LONG).show()
            disableSelf()
        }, 60_000)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (port != null && password != null) return

        if ((event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) == 0) return
        val text = event.source?.text ?: return

        val ipPortRegex = Regex("""(?:\d{1,3}\.){3}\d{1,3}:(\d{2,5})""")
        val passwordRegex = Regex("""\d{6}""")

        ipPortRegex.matchEntire(text)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?.let { port = it }
        passwordRegex.matchEntire(text)
            ?.value
            ?.let { password = it }

        if (port != null && password != null) {
            val port = port!!
            val password = password!!

            var toastMsg = getString(R.string.notification_adb_pairing_failed_title)
            GlobalScope.launch(Dispatchers.IO) {
                val host = "127.0.0.1"

                val key = try {
                    AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
                } catch (e: Throwable) {
                    toastMsg = getString(R.string.adb_error_key_store)
                    return@launch
                }

                AdbPairingClient(host, port, password, key).runCatching {
                    start()
                }.onFailure {
                    when (it) {
                        is ConnectException -> toastMsg = getString(R.string.cannot_connect_port)
                        is AdbInvalidPairingCodeException -> toastMsg = getString(R.string.paring_code_is_wrong)
                        is AdbKeyException -> toastMsg = getString(R.string.adb_error_key_store)
                    }
                }.onSuccess {
                    if (it) {
                        toastMsg = "${getString(R.string.notification_adb_pairing_succeed_title)}. ${getString(R.string.notification_adb_pairing_succeed_text)}"
                   
                        val intent = Intent(this@AdbPairingAccessibilityService, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(intent)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdbPairingAccessibilityService, toastMsg, Toast.LENGTH_LONG).show()
                }
                disableSelf()
            }
        }
    }

    override fun onInterrupt() {}

}