package moe.shizuku.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ManualStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val applicationId = context.packageName
        if (intent.action != "${applicationId}.START") return
        ShizukuReceiverStarter.start(context)
    }
}