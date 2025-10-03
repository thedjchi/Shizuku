package moe.shizuku.manager.home

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.R

class WadbEnableUsbDebuggingDialogFragment :DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val dialog = MaterialAlertDialogBuilder(context)
                .setMessage(R.string.dialog_usb_debugging_not_enabled)
                .setPositiveButton(R.string.development_settings, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create()

        dialog.setOnShowListener { onDialogShow(dialog) }
        return dialog
    }

    private fun onDialogShow(dialog: AlertDialog) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra(":settings:fragment_args_key", "enable_adb")
            try {
                dismissAllowingStateLoss()
                it.context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
            }
        }
    }

    fun show(fragmentManager: FragmentManager) {
        if (fragmentManager.isStateSaved) return
        show(fragmentManager, javaClass.simpleName)
    }
}
