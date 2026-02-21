package moe.shizuku.manager.utils

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import moe.shizuku.manager.R
import moe.shizuku.manager.core.extensions.*

object CustomTabsHelper {
    fun launchUrlOrCopy(
        context: Context,
        url: String,
    ) {
        val uri = Uri.parse(url)

        runCatching {
            launchCustomTab(context, uri)
        }.recoverCatching {
            launchUrl(context, uri)
        }.recoverCatching {
            context.toast(R.string.error_no_browser_app)
            context.copyToClipboard(url)
        }
    }

    private fun launchCustomTab(
        context: Context,
        uri: Uri,
    ) {
        val builder = CustomTabsIntent.Builder().setShowTitle(true)

        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val finalUri =
            uri
                .buildUpon()
                .apply {
                    if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
                        appendQueryParameter("night", "1")
                    }
                }.build()

        builder.build().launchUrl(context, finalUri)
    }

    private fun launchUrl(
        context: Context,
        uri: Uri,
    ) = context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}
