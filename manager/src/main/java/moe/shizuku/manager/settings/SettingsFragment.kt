package moe.shizuku.manager.settings

import android.content.pm.PackageManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import androidx.preference.Preference.SummaryProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CancellableContinuation
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.ShizukuSettings.Keys.*
import moe.shizuku.manager.app.SnackbarHelper
import moe.shizuku.manager.app.ThemeHelper
import moe.shizuku.manager.ktx.isComponentEnabled
import moe.shizuku.manager.ktx.setComponentEnabled
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.receiver.BootCompleteReceiver
import moe.shizuku.manager.receiver.ShizukuReceiverStarter
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.ShizukuStateMachine
import rikka.core.util.ResourceUtils
import rikka.html.text.HtmlCompat
import rikka.material.app.LocaleDelegate
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.shizuku.manager.ShizukuLocales
import rikka.widget.borderview.BorderRecyclerView
import java.util.*

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var startOnBootPreference: TwoStatePreference
    private lateinit var watchdogPreference: TwoStatePreference
    private lateinit var tcpModePreference: TwoStatePreference
    private lateinit var tcpPortPreference: EditTextPreference
    private lateinit var tcpLearnMorePreference: Preference
    private lateinit var languagePreference: ListPreference
    private lateinit var translationPreference: Preference
    private lateinit var translationContributorsPreference: Preference
    private lateinit var nightModePreference: IntegerSimpleMenuPreference
    private lateinit var blackNightThemePreference: TwoStatePreference
    private lateinit var useSystemColorPreference: TwoStatePreference
    private lateinit var helpPreference: Preference
    private lateinit var reportBugPreference: Preference

    private lateinit var batteryOptimizationListener: ActivityResultLauncher<Intent>
    private var batteryOptimizationContinuation: CancellableContinuation<Boolean>? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()

        preferenceManager.setStorageDeviceProtected()
        preferenceManager.sharedPreferencesName = ShizukuSettings.NAME
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        setPreferencesFromResource(R.xml.settings, null)

        startOnBootPreference = findPreference(KEY_START_ON_BOOT)!!
        watchdogPreference = findPreference(KEY_WATCHDOG)!!
        tcpModePreference = findPreference(KEY_TCP_MODE)!!
        tcpPortPreference = findPreference(KEY_TCP_PORT)!!
        tcpLearnMorePreference = findPreference(KEY_TCP_LEARN_MORE)!!
        languagePreference = findPreference(KEY_LANGUAGE)!!
        translationPreference = findPreference(KEY_TRANSLATION)!!
        translationContributorsPreference = findPreference(KEY_TRANSLATION_CONTRIBUTORS)!!
        nightModePreference = findPreference(KEY_NIGHT_MODE)!!
        blackNightThemePreference = findPreference(KEY_BLACK_NIGHT_THEME)!!
        useSystemColorPreference = findPreference(KEY_USE_SYSTEM_COLOR)!!
        helpPreference = findPreference(KEY_HELP)!!
        reportBugPreference = findPreference(KEY_REPORT_BUG)!!

        batteryOptimizationListener = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val accepted = ShizukuSettings.isIgnoringBatteryOptimizations(requireContext())
            batteryOptimizationContinuation?.resume(accepted)
        }

        startOnBootPreference.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || EnvironmentUtils.isRooted()) {
                isChecked = ShizukuSettings.getStartOnBoot(context)

                setOnPreferenceChangeListener { _, newValue ->
                    if (newValue is Boolean) {
                        val doToggle = {
                            shouldToggleBatterySensitiveSetting(newValue) { result ->
                                if (result) {
                                    ShizukuSettings.setStartOnBoot(context, newValue)
                                    isChecked = newValue
                                }
                            }
                        }

                        // https://r.android.com/2128832
                        if (newValue && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            MaterialAlertDialogBuilder(context)
                                .setTitle(android.R.string.dialog_alert_title)
                                .setMessage(R.string.settings_start_on_boot_bug)
                                .setPositiveButton(android.R.string.ok) { _, _ -> doToggle() }
                                .setNegativeButton(android.R.string.cancel) { _, _ -> isChecked = !newValue }
                                .show()
                        } else { doToggle() }
                        return@setOnPreferenceChangeListener false
                    } else false
                }
            } else {
                isEnabled = false
                isChecked = false
                summary = context.getString(R.string.settings_start_on_boot_summary)
            }
        }

        watchdogPreference.apply {
            isChecked = ShizukuSettings.isWatchdogRunning()

            setOnPreferenceChangeListener { _, newValue ->
                if (newValue is Boolean) {
                    val shouldToggle = shouldToggleBatterySensitiveSetting(newValue) { result ->
                        if (result) {
                            ShizukuSettings.setWatchdog(context, newValue)
                            isChecked = newValue
                        }
                    }
                    return@setOnPreferenceChangeListener shouldToggle
                } else false
            }
        }

        tcpModePreference.apply {
            if (EnvironmentUtils.isTlsSupported()) {
                summary = context.getString(R.string.settings_tcp_mode_summary)
                setOnPreferenceChangeListener { _, newValue ->
                    if (newValue is Boolean) {
                        if (!ShizukuStateMachine.isRunning()) return@setOnPreferenceChangeListener true
                        else MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.settings_tcp_mode_dialog_title)
                            .setMessage(HtmlCompat.fromHtml(
                                context.getString(R.string.settings_tcp_mode_dialog_message)
                            ))
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                ShizukuSettings.setTcpMode(newValue)
                                isChecked = newValue
                                tcpPortPreference.isVisible = newValue
                                ShizukuReceiverStarter.start(context)
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                    false
                }
            } else if (EnvironmentUtils.isTelevision()) {
                isEnabled = false
                isChecked = true
            } else {
                isVisible = false
            }
        }

        tcpPortPreference.apply {
            isVisible = tcpModePreference.isVisible && tcpModePreference.isChecked

            setOnBindEditTextListener { editText ->
                editText.hint = context.getString(R.string.settings_tcp_port_hint)
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.setSelection(editText.text.length)
            }

            summaryProvider = SummaryProvider<EditTextPreference> { pref ->
                val text = pref.text
                if (text.isNullOrEmpty()) context.getString(R.string.settings_tcp_port_deafult) else text
            }

            setOnPreferenceChangeListener { _, newValue ->
                val port = (newValue as? String)?.toIntOrNull()
                if (port == null || port in 1..65535) {
                    true
                } else {
                    SnackbarHelper.show(context, requireView(), context.getString(R.string.snackbar_invalid_port))
                    false
                }
            }
        }

        tcpLearnMorePreference.apply {
            isVisible = tcpModePreference.isVisible

            val tintColor = TypedValue()
            context.theme.resolveAttribute(R.attr.colorOnSurfaceVariant, tintColor, true)
            icon?.mutate()?.setTint(tintColor.data)

            setOnPreferenceClickListener {
                CustomTabsHelper.launchUrlOrCopy(context, context.getString(R.string.automation_apps_url))
                true
            }
        }

        languagePreference.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                val locale: Locale = if ("SYSTEM" == newValue) {
                    LocaleDelegate.systemLocale
                } else {
                    Locale.forLanguageTag(newValue)
                }
                LocaleDelegate.defaultLocale = locale
                activity?.recreate()
            }
            true
        }

        setupLocalePreference(languagePreference)

        nightModePreference.apply {
            value = ShizukuSettings.getNightMode()
            setOnPreferenceChangeListener { _, value ->
                if (value is Int) {
                    if (ShizukuSettings.getNightMode() != value) {
                        AppCompatDelegate.setDefaultNightMode(value)
                        activity?.recreate()
                    }
                }
                true
            }
        }

        blackNightThemePreference.apply {
            if (ShizukuSettings.getNightMode() != AppCompatDelegate.MODE_NIGHT_NO) {
                isChecked = ThemeHelper.isBlackNightTheme(context)
                setOnPreferenceChangeListener { _, _ ->
                    if (ResourceUtils.isNightMode(context.resources.configuration))
                        activity?.recreate()
                    true
                }
            } else isVisible = false
        }

        useSystemColorPreference.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                isChecked = ThemeHelper.isUsingSystemColor()
                setOnPreferenceChangeListener { _, value ->
                    if (value is Boolean) {
                        if (ThemeHelper.isUsingSystemColor() != value)
                            activity?.recreate()
                    }
                    true
                }
            } else isVisible = false
        }

        translationPreference.apply {
            summary = context.getString(R.string.settings_translation_summary, context.getString(R.string.app_name))
            setOnPreferenceClickListener {
                CustomTabsHelper.launchUrlOrCopy(context, context.getString(R.string.translation_url))
                true
            }
        }

        translationContributorsPreference.apply {
            val contributors = context.getString(R.string.translation_contributors).toHtml().toString()
            if (contributors.isNotBlank()) {
                summary = contributors
            } else isVisible = false
        }

        helpPreference.setOnPreferenceClickListener {
            CustomTabsHelper.launchUrlOrCopy(context, context.getString(R.string.help_url))
            true
        }

        reportBugPreference.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(
                "mailto:" + context.getString(R.string.support_email) + 
                "?body=v" + Uri.encode(context.packageManager.getPackageInfo(context.packageName, 0).versionName)
            ))
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, context.getString(R.string.toast_no_email_app), Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            KEY_TCP_MODE -> {
                val newValue = ShizukuSettings.getTcpMode()
                tcpModePreference.isChecked = newValue
            }
        }
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState) as BorderRecyclerView
        recyclerView.fixEdgeEffect()
        recyclerView.addEdgeSpacing(bottom = 8f, unit = TypedValue.COMPLEX_UNIT_DIP)

        val lp = recyclerView.layoutParams
        if (lp is FrameLayout.LayoutParams) {
            lp.rightMargin = recyclerView.context.resources.getDimension(R.dimen.rd_activity_horizontal_margin).toInt()
            lp.leftMargin = lp.rightMargin
        }

        return recyclerView
    }

    private fun shouldToggleBatterySensitiveSetting (
        newValue: Boolean,
        onResult: (Boolean) -> Unit
    ): Boolean {
        val context = requireContext()
        if (!newValue || ShizukuSettings.isIgnoringBatteryOptimizations(context) || EnvironmentUtils.isTelevision()) {
            onResult(true)
            return true
        }
            
        lifecycleScope.launch {
            val result = suspendCancellableCoroutine<Boolean> { continuation ->
                batteryOptimizationContinuation = continuation
                SnackbarHelper.show(
                    context,
                    requireView(),
                    msg = context.getString(R.string.snackbar_battery_optimization_settings),
                    duration = 6000,
                    actionText = context.getString(R.string.snackbar_action_fix),
                    action = { ShizukuSettings.requestIgnoreBatteryOptimizations(context, batteryOptimizationListener) },
                    onDismiss = { event ->
                        if (event != Snackbar.Callback.DISMISS_EVENT_ACTION && continuation.isActive)
                            continuation.resume(false)
                    }
                )
            }
            onResult(result)
        }
        return false
    }

    private fun setupLocalePreference(languagePreference: ListPreference) {
        val localeTags = ShizukuLocales.LOCALES
        val displayLocaleTags = ShizukuLocales.DISPLAY_LOCALES

        languagePreference.entries = displayLocaleTags
        languagePreference.entryValues = localeTags

        val currentLocaleTag = languagePreference.value
        val currentLocaleIndex = localeTags.indexOf(currentLocaleTag)
        val currentLocale = ShizukuSettings.getLocale()
        val localizedLocales = mutableListOf<CharSequence>()

        for ((index, displayLocale) in displayLocaleTags.withIndex()) {
            if (index == 0) {
                localizedLocales.add(getString(R.string.follow_system))
                continue
            }

            val locale = Locale.forLanguageTag(displayLocale.toString())
            val localeName = if (!TextUtils.isEmpty(locale.script))
                locale.getDisplayScript(locale)
            else
                locale.getDisplayName(locale)

            val localizedLocaleName = if (!TextUtils.isEmpty(locale.script))
                locale.getDisplayScript(currentLocale)
            else
                locale.getDisplayName(currentLocale)

            localizedLocales.add(
                if (index != currentLocaleIndex) {
                    "$localeName<br><small>$localizedLocaleName<small>".toHtml()
                } else {
                    localizedLocaleName
                }
            )
        }

        languagePreference.entries = localizedLocales.toTypedArray()

        languagePreference.summary = when {
            TextUtils.isEmpty(currentLocaleTag) || "SYSTEM" == currentLocaleTag -> {
                getString(R.string.follow_system)
            }
            currentLocaleIndex != -1 -> {
                val localizedLocale = localizedLocales[currentLocaleIndex]
                val newLineIndex = localizedLocale.indexOf('\n')
                if (newLineIndex == -1) {
                    localizedLocale.toString()
                } else {
                    localizedLocale.subSequence(0, newLineIndex).toString()
                }
            }
            else -> {
                ""
            }
        }
    }
}
