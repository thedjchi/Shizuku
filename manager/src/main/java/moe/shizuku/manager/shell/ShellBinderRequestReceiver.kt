package moe.shizuku.manager.shell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.shizuku.manager.shell.ShellBinderRequestHandler

class ShellBinderRequestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "rikka.shizuku.intent.action.REQUEST_BINDER") return
        ShellBinderRequestHandler.handleRequest(context, intent)
    }
}
