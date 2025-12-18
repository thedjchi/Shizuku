package moe.shizuku.manager.home

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbPairingAccessibilityService
import moe.shizuku.manager.ktx.toHtml
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

        val installer = context.packageManager.getInstallerPackageName(context.packageName)
        val isPlayOrAdbInstall = (installer == "com.android.vending") || (installer == null)
        val hasEnabledAccessibilityBefore = ShizukuSettings.getHasEnabledAccessibilityBefore()
        val canAccessRestrictedSettings = isPlayOrAdbInstall || hasEnabledAccessibilityBefore

        val permissionName = "ACCESS_RESTRICTED_SETTINGS"
        val permissionCommand = "<p><font face=\"monospace\">adb shell cmd appops set ${context.packageName} ${permissionName} allow</font></p>"

        return if (isAccessibilityEnabled) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.dialog_adb_pairing_title)
                .setMessage(R.string.dialog_adb_pairing_accessibility_navigate)
                .setPositiveButton(R.string.development_settings) { _, _ ->
                    SettingsPage.Developer.HighlightWirelessDebugging.launch(context)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        } else if (canAccessRestrictedSettings) {
            buildEnableAccessibilityDialog(context)
        } else {
            MaterialAlertDialogBuilder(context)
                .setTitle(android.R.string.dialog_alert_title)
                .setMessage(context.getString(R.string.dialog_adb_pairing_accessibility_permission, permissionName, permissionCommand)
                    .toHtml())
                .setPositiveButton("Continue") { _, _ ->
                    buildEnableAccessibilityDialog(context).show()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        }
    }

    private fun buildEnableAccessibilityDialog(context: Context): Dialog {
        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_adb_pairing_title)
            .setMessage(R.string.dialog_adb_pairing_accessibility_enable)
            .setPositiveButton(R.string.enable) { _, _ ->
                SettingsPage.Accessibility.launch(context)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    fun show(fragmentManager: FragmentManager) {
        if (fragmentManager.isStateSaved) return
        show(fragmentManager, javaClass.simpleName)
    }
}
