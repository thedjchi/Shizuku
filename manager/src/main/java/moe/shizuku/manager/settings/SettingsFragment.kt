package moe.shizuku.manager.settings

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.*
import androidx.preference.Preference.SummaryProvider
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.ShizukuSettings.Keys.*
import moe.shizuku.manager.app.ThemeHelper
import moe.shizuku.manager.ktx.isComponentEnabled
import moe.shizuku.manager.ktx.setComponentEnabled
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.receiver.BootCompleteReceiver
import moe.shizuku.manager.utils.CustomTabsHelper
import moe.shizuku.manager.utils.EnvironmentUtils
import rikka.core.util.ResourceUtils
import rikka.material.app.LocaleDelegate
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.shizuku.manager.ShizukuLocales
import rikka.widget.borderview.BorderRecyclerView
import java.util.*

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = requireContext()

        preferenceManager.setStorageDeviceProtected()
        preferenceManager.sharedPreferencesName = ShizukuSettings.NAME
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        setPreferencesFromResource(R.xml.settings, null)

        val startOnBootPreference: TwoStatePreference = findPreference(KEY_START_ON_BOOT)!!
        val tcpModePreference: TwoStatePreference = findPreference(KEY_TCP_MODE)!!
        val tcpPortPreference: EditTextPreference = findPreference(KEY_TCP_PORT)!!
        val tcpLearnMorePreference: Preference = findPreference(KEY_TCP_LEARN_MORE)!!
        val languagePreference: ListPreference = findPreference(KEY_LANGUAGE)!!
        val translationPreference: Preference = findPreference(KEY_TRANSLATION)!!
        val translationContributorsPreference: Preference = findPreference(KEY_TRANSLATION_CONTRIBUTORS)!!
        val nightModePreference: IntegerSimpleMenuPreference = findPreference(KEY_NIGHT_MODE)!!
        val blackNightThemePreference: TwoStatePreference = findPreference(KEY_BLACK_NIGHT_THEME)!!
        val useSystemColorPreference: TwoStatePreference = findPreference(KEY_USE_SYSTEM_COLOR)!!

        val prefs = ShizukuSettings.getPreferences()

        startOnBootPreference.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || EnvironmentUtils.isRooted()) {
                isEnabled = true

                val bootCompleteReceiver = ComponentName(context.packageName, BootCompleteReceiver::class.java.name)
                isChecked = context.packageManager.isComponentEnabled(bootCompleteReceiver)

                setOnPreferenceChangeListener { _, newValue ->
                    if (newValue is Boolean) {
                        context.packageManager.setComponentEnabled(bootCompleteReceiver, newValue)
                        context.packageManager.isComponentEnabled(bootCompleteReceiver) == newValue
                    } else false
                }

                summary = ""
            } else {
                isEnabled = false
                isChecked = false
                summary = context.getString(R.string.settings_start_on_boot_summary)         
            }
        }

        tcpModePreference.setOnPreferenceChangeListener { _, newValue ->
            if (newValue is Boolean)  {
                tcpPortPreference.isVisible = newValue        
                true
            } else false
        }

        tcpPortPreference.apply {
            setOnBindEditTextListener { editText ->
                editText.hint = context.getString(R.string.settings_tcp_port_hint)
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.setSelection(editText.text.length)
            }
            summaryProvider = SummaryProvider<EditTextPreference> { pref ->
                val text = pref.text
                if (text.isNullOrEmpty()) "5555" else text
            }
        }

        tcpLearnMorePreference.apply {
            val tintColor = TypedValue()
            context.theme.resolveAttribute(R.attr.colorOnSurfaceVariant, tintColor, true)
            icon!!.mutate().setTint(tintColor.data)

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
