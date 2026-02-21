package moe.shizuku.manager.shell

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import rikka.shizuku.Shizuku

object ShellBinderRequestHandler {

    fun handleRequest(context: Context, intent: Intent) {
        val binder = intent.getBundleExtra("data")?.getBinder("binder") ?: return
        val shizukuBinder = Shizuku.getBinder()
        if (shizukuBinder == null) {
            Log.w("ShellBinderRequestHandler", "Binder not received or Shizuku service not running")
        }

        val data = Parcel.obtain()
        try {
            data.writeStrongBinder(shizukuBinder)
            data.writeString(context.applicationInfo.sourceDir)
            binder.transact(1, data, null, IBinder.FLAG_ONEWAY)
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            data.recycle()
        }
    }
    
}
