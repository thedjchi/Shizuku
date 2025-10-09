package moe.shizuku.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.shizuku.manager.worker.AdbStartWorker

class NotifAttemptReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", -1)
        val isWifiRequired = intent.getBooleanExtra("is_wifi_required", true)
        AdbStartWorker.enqueue(context, isWifiRequired, notificationId)
    }
}
