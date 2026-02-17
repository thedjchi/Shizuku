package moe.shizuku.manager.home

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.utils.SettingsPage

class WadbEnableUsbDebuggingDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val dialog =
            MaterialAlertDialogBuilder(context)
                .setMessage(R.string.start_error_usb_debugging_disabled)
                .setPositiveButton(R.string.developer_options, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()

        dialog.setOnShowListener { onDialogShow(dialog) }
        return dialog
    }

    private fun onDialogShow(dialog: AlertDialog) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            dismissAllowingStateLoss()
            SettingsPage.Developer.HighlightUsbDebugging.launch(it.context)
        }
    }

    fun show(fragmentManager: FragmentManager) {
        if (fragmentManager.isStateSaved) return
        show(fragmentManager, javaClass.simpleName)
    }
}
