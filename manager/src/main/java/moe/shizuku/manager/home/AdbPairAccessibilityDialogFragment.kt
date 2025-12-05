package moe.shizuku.manager.home

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbPairingAccessibilityService
import moe.shizuku.manager.utils.SettingsHelper
import moe.shizuku.manager.utils.SettingsPage

class AdbPairAccessibilityDialogFragment: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val accessibilityServiceName = "${context.packageName}/${AdbPairingAccessibilityService::class.java.canonicalName}"
        
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val isAccessibilityEnabled = enabledServices
            ?.split(":")
            ?.any { it.equals(accessibilityServiceName) } ?: false

        return if (!isAccessibilityEnabled) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.dialog_adb_pairing_title)
                .setMessage(R.string.dialog_adb_pairing_accessibility_enable)
                .setPositiveButton(R.string.enable) { _, _ ->
                    SettingsPage.Accessibility.launch(context)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        } else {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.dialog_adb_pairing_title)
                .setMessage(R.string.dialog_adb_pairing_accessibility_navigate)
                .setPositiveButton(R.string.development_settings) { _, _ ->
                    SettingsPage.Developer.HighlightWirelessDebugging.launch(context)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        }
    }

    fun show(fragmentManager: FragmentManager) {
        if (fragmentManager.isStateSaved) return
        show(fragmentManager, javaClass.simpleName)
    }
}
