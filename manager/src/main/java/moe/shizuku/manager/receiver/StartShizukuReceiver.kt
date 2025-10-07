package moe.shizuku.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StartShizukuReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "moe.shizuku.privileged.api.START")
            StartShizukuIntentHandler.handle(context, intent, isWifiRequired = false)
    }
}