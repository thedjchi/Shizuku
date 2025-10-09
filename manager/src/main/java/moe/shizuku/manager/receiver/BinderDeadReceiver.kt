package moe.shizuku.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.shizuku.manager.utils.ShizukuStateMachine

class BinderDeadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "rikka.shizuku.BINDER_DEAD") return

        val currentState = ShizukuStateMachine.getState()
        if (currentState == ShizukuStateMachine.State.STOPPING) {
            ShizukuStateMachine.setState(ShizukuStateMachine.State.STOPPED)
        } else if (currentState == ShizukuStateMachine.State.RUNNING) {
            ShizukuStateMachine.setState(ShizukuStateMachine.State.CRASHED)
            ShizukuReceiverStarter.start(context, intent)
        } else return
    }
}
