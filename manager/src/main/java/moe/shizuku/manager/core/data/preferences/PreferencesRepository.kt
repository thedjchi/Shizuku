package moe.shizuku.manager.core.data.preferences

import android.content.SharedPreferences
import moe.shizuku.manager.core.models.preferences.*

object PreferencesRepository {
    private val prefs = PreferencesDataSource
    
    // -------------------------
    // GETTERS
    // -------------------------

    fun getStartMode(): StartMode =
        fromValueOrDefault<StartMode>(
            prefs.get(Preferences.START_MODE),
        )

    fun getStartOnBoot(): Boolean = prefs.get(Preferences.START_ON_BOOT)

    fun getWatchdog(): Boolean = prefs.get(Preferences.WATCHDOG)

    fun getTcpMode(): Boolean = prefs.get(Preferences.TCP_MODE)

    fun getTcpPort(): Int = prefs.get(Preferences.TCP_PORT)

    fun getAutoDisableUsbDebugging(): Boolean = prefs.get(Preferences.AUTO_DISABLE_USB_DEBUGGING)

    fun getLanguage(): String? = prefs.get(Preferences.LANGUAGE)

    fun getTheme(): Theme =
        fromValueOrDefault<Theme>(
            prefs.get(Preferences.THEME),
        )

    fun getAmoledBlack(): Boolean = prefs.get(Preferences.AMOLED_BLACK)

    fun getDynamicColor(): Boolean = prefs.get(Preferences.DYNAMIC_COLOR)

    fun getCheckForUpdates(): Boolean = prefs.get(Preferences.CHECK_FOR_UPDATES)

    fun getUpdateChannel(): UpdateChannel =
        fromValueOrDefault<UpdateChannel>(
            prefs.get(Preferences.UPDATE_CHANNEL),
        )

    fun getLastPromptedVersion(): String? = prefs.get(Preferences.LAST_PROMPTED_VERSION)

    fun getLegacyPairing(): Boolean = prefs.get(Preferences.LEGACY_PAIRING)

    fun getAuthToken(): String? = prefs.get(Preferences.AUTH_TOKEN)

    // Generic reverse lookup function for enums
    private inline fun <reified T> fromValueOrDefault(value: Int): T
    where T : Enum<T>, T : IntEnum {
        val entries = enumValues<T>()
        return entries.find { it.value == value }
            ?: entries.first().default as T
    }

    // -------------------------
    // SETTERS
    // -------------------------

    fun setStartMode(mode: StartMode) = prefs.set(Preferences.START_MODE, mode.value)

    fun setStartOnBoot(value: Boolean) = prefs.set(Preferences.START_ON_BOOT, value)

    fun setWatchdog(value: Boolean) = prefs.set(Preferences.WATCHDOG, value)

    fun setTcpMode(value: Boolean) = prefs.set(Preferences.TCP_MODE, value)

    fun setTcpPort(value: Int) = prefs.set(Preferences.TCP_PORT, value)

    fun setAutoDisableUsbDebugging(value: Boolean) = prefs.set(Preferences.AUTO_DISABLE_USB_DEBUGGING, value)

    fun setLanguage(value: String?) = prefs.set(Preferences.LANGUAGE, value)

    fun setTheme(theme: Theme) = prefs.set(Preferences.THEME, theme.value)

    fun setAmoledBlack(value: Boolean) = prefs.set(Preferences.AMOLED_BLACK, value)

    fun setDynamicColor(value: Boolean) = prefs.set(Preferences.DYNAMIC_COLOR, value)

    fun setCheckForUpdates(value: Boolean) = prefs.set(Preferences.CHECK_FOR_UPDATES, value)

    fun setUpdateChannel(channel: UpdateChannel) = prefs.set(Preferences.UPDATE_CHANNEL, channel.value)

    fun setLastPromptedVersion(value: String?) = prefs.set(Preferences.LAST_PROMPTED_VERSION, value)

    fun setLegacyPairing(value: Boolean) = prefs.set(Preferences.LEGACY_PAIRING, value)

    fun setAuthToken(value: String?) = prefs.set(Preferences.AUTH_TOKEN, value)
}
