package moe.shizuku.manager.app

import android.content.Context
import android.view.View
import com.google.android.material.snackbar.Snackbar
import moe.shizuku.manager.app.ThemeHelper
import moe.shizuku.manager.R

object SnackbarHelper {

    private var snackbar: Snackbar? = null

    fun show(
        context: Context,
        view: View,
        msg: String,
        duration: Int = Snackbar.LENGTH_SHORT,
        actionText: String? = null,
        action: (() -> Unit)? = null,
        onDismiss: ((event: Int) -> Unit)? = null
    ) {
        snackbar = Snackbar.make(view, msg, duration)
            .setDuration(duration)
        if (action != null) {
            snackbar!!.setAction(actionText ?: context.getString(R.string.snackbar_action_ok)) { action() }
        }
        if (onDismiss != null) {
            snackbar!!.addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    onDismiss(event)
                }
            })
        }
        ThemeHelper.applySnackbarTheme(context, snackbar!!)
        snackbar!!.show()
    }

    fun dismiss() {
        snackbar?.dismiss()
    }

}