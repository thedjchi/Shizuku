package moe.shizuku.manager.shell.legacy

import android.os.Bundle
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.shell.ShellBinderRequestHandler

class ShellBinderRequestActivity : AppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.action != "rikka.shizuku.intent.action.REQUEST_BINDER") return
        ShellBinderRequestHandler.handleRequest(this, intent)
        
        finish()
    }
}
