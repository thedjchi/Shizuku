package moe.shizuku.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.shizuku.Shizuku

class ManualStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "moe.shizuku.privileged.api.STOP") return
        try {
            ShizukuStateMachine.setState(ShizukuStateMachine.State.STOPPING)
            Shizuku.exit()
        } catch (e: Exception) {
            if(!Shizuku.pingBinder()) {
                ShizukuStateMachine.setState(ShizukuStateMachine.State.STOPPED)
            } else {
                ShizukuStateMachine.setState(ShizukuStateMachine.State.RUNNING)
                Toast.makeText(context, "Failed to stop Shizuku: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}