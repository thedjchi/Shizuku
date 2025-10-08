package moe.shizuku.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ShizukuDeathReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "rikka.shizuku.BINDER_DIED") return
        StartShizukuIntentHandler.handle(context, intent)
    }
}
