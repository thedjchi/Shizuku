package moe.shizuku.manager.home

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.R

class WadbNotEnabledDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        return MaterialAlertDialogBuilder(context)
            .setMessage(R.string.start_error_wireless_debugging_disabled)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    fun show(fragmentManager: FragmentManager) {
        if (fragmentManager.isStateSaved) return
        show(fragmentManager, javaClass.simpleName)
    }
}
