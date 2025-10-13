package moe.shizuku.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ManualStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "moe.shizuku.privileged.api.START") return
        ShizukuReceiverStarter.start(context)
    }
}