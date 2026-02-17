package moe.shizuku.manager.legacy

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.MainActivity
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.utils.toHtml
import rikka.html.text.HtmlCompat

class LegacyIsNotSupportedActivity : AppActivity() {
    companion object {
        /**
         * Activity result: user denied request (only API pre-23).
         */
        private inline val RESULT_CANCELED get() = Activity.RESULT_CANCELED

        /**
         * Activity result: error, such as manager app itself not authorized.
         */
        private const val RESULT_ERROR = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callingComponent = callingActivity
        if (callingComponent == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val ai =
            try {
                packageManager.getApplicationInfo(callingComponent.packageName, PackageManager.GET_META_DATA)
            } catch (e: Throwable) {
                finish()
                return
            }

        val label =
            try {
                ai.loadLabel(packageManager)
            } catch (e: Exception) {
                ai.packageName
            }

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.legacy_unsupported, label))
            .setMessage(
                getString(R.string.legacy_unsupported_message, label).toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE),
            ).setPositiveButton(android.R.string.ok, null)
            .setOnDismissListener {
                setResult(RESULT_ERROR)
                finish()
            }.setCancelable(false)
            .show()
    }
}
