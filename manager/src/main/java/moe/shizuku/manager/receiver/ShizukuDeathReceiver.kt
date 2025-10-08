package moe.shizuku.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class ShizukuDeathReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "rikka.shizuku.BINDER_DIED") return
        Toast.makeText(context.applicationContext, "Shizuku stopped", Toast.LENGTH_SHORT).show()
        StartShizukuIntentHandler.handle(context, intent)
    }
}
