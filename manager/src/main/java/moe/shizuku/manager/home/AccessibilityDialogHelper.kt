package moe.shizuku.manager.home

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.TypefaceSpan
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.adb.AdbPairingAccessibilityService
import moe.shizuku.manager.utils.SettingsHelper
import moe.shizuku.manager.utils.SettingsPage

fun Context.showAccessibilityDialog() {
    val hasWriteSecureSettings = (checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED)

    val installer = packageManager.getInstallerPackageName(packageName)
    val isInstalledByPlayOrAdb = (installer == "com.android.vending") || (installer == null)
    val hasAccessRestrictedSettings = isInstalledByPlayOrAdb || Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    if (isAccessibilityEnabled()) {
        showNavigateDialog()
    } else if (hasWriteSecureSettings) {
        if (enableAccessibilityService()) return
        showPermissionDialog()
    } else if (!hasAccessRestrictedSettings) {
        showPermissionDialog()
    } else {
        showEnableDialog()
    }
}

private fun Context.showPermissionDialog() {
    val permissionName = "ACCESS_RESTRICTED_SETTINGS"
    val permissionCommand = "adb shell cmd appops set $packageName $permissionName allow"
    val styledPermissionCommand =
        SpannableString(permissionCommand).apply {
            setSpan(TypefaceSpan("monospace"), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

    MaterialAlertDialogBuilder(this)
        .setTitle(android.R.string.dialog_alert_title)
        .setMessage(
            TextUtils.expandTemplate(
                getString(R.string.pairing_accessibility_permission),
                permissionName,
                styledPermissionCommand,
            ),
        ).setPositiveButton("Continue") { _, _ -> showEnableDialog() }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}

private fun Context.showEnableDialog() {
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.pairing_accessibility_required)
        .setMessage(R.string.pairing_accessibility_required_message)
        .setPositiveButton(R.string.enable) { _, _ ->
            SettingsPage.Accessibility.launch(this)
        }.setNegativeButton(android.R.string.cancel, null)
        .show()
}

private fun Context.showNavigateDialog() {
    val msg =
        buildString {
            append(getString(R.string.pairing_steps_intro))
            append("\n -")
            append(getString(R.string.pairing_tutorial_1))
            append("\n -")
            append(getString(R.string.pairing_tutorial_2))
            append("\n\n")
            append(getString(R.string.pairing_auto_detect))
        }
    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.pair)
        .setMessage(msg)
        .setPositiveButton(R.string.developer_options) { _, _ ->
            SettingsPage.Developer.HighlightWirelessDebugging.launch(this)
        }.setNegativeButton(android.R.string.cancel, null)
        .show()
}

private fun Context.getEnabledAccessibilityServices(): List<String>? {
    val enabledServices =
        Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
    return enabledServices?.split(":")
}

private fun Context.isAccessibilityEnabled(): Boolean {
    val accessibilityServiceName = "$packageName/${AdbPairingAccessibilityService::class.java.canonicalName}"
    return getEnabledAccessibilityServices()?.any { it.equals(accessibilityServiceName) } ?: false
}

private fun Context.enableAccessibilityService(): Boolean {
    if (isAccessibilityEnabled()) return true

    val accessibilityServiceName = "$packageName/${AdbPairingAccessibilityService::class.java.canonicalName}"
    val enabledServices = getEnabledAccessibilityServices()
    val newServices =
        if (enabledServices.isNullOrEmpty()) {
            accessibilityServiceName
        } else {
            enabledServices.joinToString(":") + ":$accessibilityServiceName"
        }

    Settings.Secure.putString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        newServices,
    )

    return isAccessibilityEnabled()
}
