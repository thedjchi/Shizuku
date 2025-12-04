package moe.shizuku.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.shizuku.manager.R
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.shizuku.Shizuku

class ManualStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val applicationId = context.packageName
        if (intent.action != "${applicationId}.STOP") return
        if (!ShizukuStateMachine.isRunning()) return

        ShizukuStateMachine.set(ShizukuStateMachine.State.STOPPING)
        runCatching { Shizuku.exit() }
    }
}