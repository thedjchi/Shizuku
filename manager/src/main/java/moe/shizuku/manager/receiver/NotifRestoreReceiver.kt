package moe.shizuku.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotifRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isWifiRequired = intent.getBooleanExtra("is_wifi_required", true)
        ShizukuReceiverStarter.showNotification(context, isWifiRequired)
    }
}