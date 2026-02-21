// package moe.shizuku.manager.home

// import android.Manifest.permission.WRITE_SECURE_SETTINGS
// import android.content.Context
// import android.content.Intent
// import android.content.pm.PackageManager
// import android.os.Build
// import android.provider.Settings
// import android.text.method.LinkMovementMethod
// import android.view.LayoutInflater
// import android.view.View
// import android.view.ViewGroup
// import androidx.annotation.RequiresApi
// import androidx.core.view.isVisible
// import androidx.fragment.app.FragmentActivity
// import androidx.work.WorkManager
// import kotlinx.coroutines.launch
// import kotlinx.coroutines.CoroutineScope
// import kotlinx.coroutines.Dispatchers
// import moe.shizuku.manager.ShizukuSettings
// import moe.shizuku.manager.R
// import moe.shizuku.manager.adb.AdbPairingTutorialActivity
// import moe.shizuku.manager.adb.AdbStarter
// import moe.shizuku.manager.databinding.HomeItemContainerBinding
// import moe.shizuku.manager.databinding.HomeStartWirelessAdbBinding
// import moe.shizuku.manager.home.showAccessibilityDialog
// import moe.shizuku.manager.utils.toHtml
// import moe.shizuku.manager.receiver.NotifCancelReceiver
// import moe.shizuku.manager.starter.StarterActivity
// import moe.shizuku.manager.utils.EnvironmentUtils
// import moe.shizuku.manager.utils.ShizukuStateMachine
// import rikka.core.content.asActivity
// import rikka.html.text.HtmlCompat
// import rikka.recyclerview.BaseViewHolder
// import rikka.recyclerview.BaseViewHolder.Creator

// class StartWirelessAdbViewHolder(binding: HomeStartWirelessAdbBinding, root: View, private val scope: CoroutineScope) :
//     BaseViewHolder<Any?>(root) {

//     companion object {
//         fun creator (scope: CoroutineScope): Creator<Any> {
//             return Creator { inflater: LayoutInflater, parent: ViewGroup? ->
//                 val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
//                 val inner = HomeStartWirelessAdbBinding.inflate(inflater, outer.root, true)
//                 StartWirelessAdbViewHolder(inner, outer.root, scope)
//             }
//         }

//         fun start (context: Context, scope: CoroutineScope) {
//             context.sendBroadcast(Intent(context, NotifCancelReceiver::class.java))

//             val cr = context.contentResolver
//             if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
//                 Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
//                 Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
//             }

//             val adbEnabled = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0)
//             if (adbEnabled == 0) {
//                 WadbEnableUsbDebuggingDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
//                 return
//             }

//             val tcpPort = EnvironmentUtils.getAdbTcpPort()
//             val tcpMode = ShizukuSettings.getTcpMode()

//             // If ADB is NOT listening to a TCP port and the device doesn't support TLS, inform the user
//             if (tcpPort <= 0 && !EnvironmentUtils.isTlsSupported()) {
//                 WadbNotEnabledDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
//             // If ADB IS NOT listening to a TCP port but the device supports TLS, start mDns discovery
//             } else if (tcpPort <= 0) {
//                 AdbDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
//             // If ADB IS listening to a TCP port but the user wants to close it and use TLS instead, close the TCP port and start mDns discovery
//             } else if (!tcpMode) {
//                 scope.launch {
//                     AdbStarter.stopTcp(context, tcpPort)
//                 }
//                 AdbDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
//             // Otherwise ADB IS listening to a TCP port and the user wants to keep it open. Start Shizuku via TCP
//             } else {
//                 val intent = Intent(context, StarterActivity::class.java).apply {
//                     putExtra(StarterActivity.EXTRA_PORT, tcpPort)
//                 }
//                 context.startActivity(intent)
//             }
//         }
//     }

//     init {
//         binding.button1.setOnClickListener { v: View ->
//             start(v.context, scope)
//         }

//         if (EnvironmentUtils.isTlsSupported()) {
//             binding.button2.setOnClickListener { v: View ->
//                 onPairClicked(v.context)
//             }
//             binding.text1.movementMethod = LinkMovementMethod.getInstance()
//             binding.text1.text = context.getString(R.string.home_wireless_adb_description)
//                 .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
//         } else {
//             binding.text1.text = context.getString(R.string.home_wireless_adb_description_pre_11)
//                 .toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE)
//             binding.button2.isVisible = false
//             binding.button3.isVisible = false
//         }
//     }

//     override fun onBind(payloads: MutableList<Any>) {
//         super.onBind(payloads)
//     }

//     @RequiresApi(Build.VERSION_CODES.R)
//     private fun onPairClicked(context: Context) {
//         if (EnvironmentUtils.isTelevision()) {
//             context.showAccessibilityDialog()
//         } else if ((context.display?.displayId ?: -1) > 0 || ShizukuSettings.getLegacyPairing()) {
//             // Running in a multi-display environment (e.g., Windows Subsystem for Android),
//             // pairing dialog can be displayed simultaneously with Shizuku.
//             // Input from notification is harder to use under this situation.
//             AdbPairDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
//         } else {
//             context.startActivity(Intent(context, AdbPairingTutorialActivity::class.java))
//         }
//     }
// }
