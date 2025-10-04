package moe.shizuku.manager.home

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemProperties
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.EOFException
import java.net.SocketException
import java.net.Inet4Address
import moe.shizuku.manager.Helps
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.ShizukuSettings.Keys.*
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbClient
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import moe.shizuku.manager.adb.AdbPairingTutorialActivity
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.databinding.HomeStartWirelessAdbBinding
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.starter.StarterActivity
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.utils.EnvironmentUtils
import rikka.core.content.asActivity
import rikka.html.text.HtmlCompat
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator

class StartWirelessAdbViewHolder(binding: HomeStartWirelessAdbBinding, root: View, private val scope: CoroutineScope) :
    BaseViewHolder<Any?>(root) {

    companion object {
        fun creator (scope: CoroutineScope): Creator<Any> {
            return Creator { inflater: LayoutInflater, parent: ViewGroup? ->
                val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
                val inner = HomeStartWirelessAdbBinding.inflate(inflater, outer.root, true)
                StartWirelessAdbViewHolder(inner, outer.root, scope)
            }
        }
    }

    init {
        binding.button1.setOnClickListener { v: View ->
            onAdbClicked(v.context, scope)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.button3.setOnClickListener { v: View ->
                CustomTabsHelper.launchUrlOrCopy(v.context, Helps.ADB_ANDROID11.get())
            }
            binding.button2.setOnClickListener { v: View ->
                onPairClicked(v.context)
            }
            binding.text1.movementMethod = LinkMovementMethod.getInstance()
            binding.text1.text = context.getString(R.string.home_wireless_adb_description)
                .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
        } else {
            binding.text1.text = context.getString(R.string.home_wireless_adb_description_pre_11)
                .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
            binding.button2.isVisible = false
            binding.button3.isVisible = false
        }
    }

    override fun onBind(payloads: MutableList<Any>) {
        super.onBind(payloads)
    }

    private fun onAdbClicked(context: Context, scope: CoroutineScope) {
        if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED)
            Settings.Global.putInt(context.contentResolver, Settings.Global.ADB_ENABLED, 1)
        
        val adbEnabled = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0)

        val tcpPort = EnvironmentUtils.getAdbTcpPort()
        val tcpMode = ShizukuSettings.getTcpMode()

        // If ADB is not listening to a TCP port and the device doesn't support TLS, inform the user
        if (tcpPort <= 0 && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            WadbNotEnabledDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
        // If USB debugging is off and Shizuku couldn't toggle it, prompt the user to toggle it manually
        } else if (adbEnabled == 0) {
            WadbEnableUsbDebuggingDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
        // If ADB IS NOT listening to a TCP port but the device supports TLS, start mDns discovery
        } else if (tcpPort <= 0) {
            AdbDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
        // If ADB IS listening to a TCP port but the user wants to close it and use TLS instead, close the TCP port and start mDns discovery
        } else if (!tcpMode) {
            stopTcp(context, scope, tcpPort)
            AdbDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
        // Otherwise ADB IS listening to a TCP port and the user wants to keep it open. Start Shizuku via TCP
        } else {
            val intent = Intent(context, StarterActivity::class.java).apply {
                    putExtra(StarterActivity.EXTRA_PORT, tcpPort)
                }
            context.startActivity(intent)
        }
    }

    private fun stopTcp(context: Context, scope: CoroutineScope, port: Int) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val key = AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
                AdbClient("127.0.0.1", port, key).use { client ->
                    client.connect()  
                    client.command("usb:")
                }
            }.onFailure {
                if (it !is EOFException && it !is SocketException) { // Expected when ADB restarts
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.adb_error_stop_tcp), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onPairClicked(context: Context) {
        val legacyPairingOverride = ShizukuSettings.getLegacyPairing()
        if ((context.display?.displayId ?: -1) > 0 || legacyPairingOverride) {
            // Running in a multi-display environment (e.g., Windows Subsystem for Android),
            // pairing dialog can be displayed simultaneously with Shizuku.
            // Input from notification is harder to use under this situation.
            AdbPairDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
        } else {
            context.startActivity(Intent(context, AdbPairingTutorialActivity::class.java))
        }
    }
}
