package moe.shizuku.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Process
import android.widget.Toast
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.utils.HeadlessLogger

class ProvisionAuthReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "${context.packageName}.PROVISION_AUTH") return

        HeadlessLogger.i("ProvisionAuth", "Auth token provisioning requested")

        val callingUid = Binder.getCallingUid()
        if (callingUid != Process.SHELL_UID && callingUid != Process.ROOT_UID) {
            HeadlessLogger.w("ProvisionAuth", "Caller not shell/root (uid=$callingUid), rejected")
            Toast.makeText(
                context,
                R.string.notification_auth_invalid_title,
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        val token = intent.getStringExtra("auth_token")
        if (token.isNullOrEmpty()) {
            HeadlessLogger.w("ProvisionAuth", "Missing auth token")
            Toast.makeText(
                context,
                R.string.notification_auth_missing_title,
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        ShizukuSettings.setAuthToken(token)
        HeadlessLogger.i("ProvisionAuth", "Auth token set (len=${token.length})")
        Toast.makeText(
            context,
            context.getString(R.string.home_automation_regenerate_token),
            Toast.LENGTH_SHORT,
        ).show()
    }
}
