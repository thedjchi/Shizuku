package moe.shizuku.manager.core.data.preferences

import moe.shizuku.manager.core.models.preferences.*

data class Preference<T>(
    val key: String,
    val default: T,
)

object Preferences {
    // -------------------------
    // BEHAVIOR
    // -------------------------

    val START_MODE =
        Preference<String?>(
            key = "startMode",
            default = StartMode.PC.name,
        )

    val START_ON_BOOT =
        Preference<Boolean>(
            key = "start_on_boot",
            default = false,
        )

    val WATCHDOG =
        Preference<Boolean>(
            key = "watchdog",
            default = false,
        )

    val TCP_MODE =
        Preference<Boolean>(
            key = "tcp_mode",
            default = true,
        )

    val TCP_PORT =
        Preference<Int>(
            key = "tcp_port",
            default = 5555,
        )

    val AUTO_DISABLE_USB_DEBUGGING =
        Preference<Boolean>(
            key = "auto_disable_usb_debugging",
            default = false,
        )

    // -------------------------
    // APPEARANCE
    // -------------------------

    val LANGUAGE =
        Preference<String?>(
            key = "language",
            default = "system",
        )

    val THEME =
        Preference<String?>(
            key = "theme",
            default = Theme.SYSTEM.name,
        )

    val AMOLED_BLACK =
        Preference<Boolean>(
            key = "amoled_black",
            default = false,
        )

    val DYNAMIC_COLOR =
        Preference<Boolean>(
            key = "dynamic_color",
            default = true,
        )

    // -------------------------
    // UPDATES
    // -------------------------

    val CHECK_FOR_UPDATES =
        Preference<Boolean>(
            key = "check_for_updates",
            default = true,
        )
    val UPDATE_CHANNEL =
        Preference<String?>(
            key = "update_channel",
            default = UpdateChannel.STABLE.name,
        )

    val LAST_PROMPTED_VERSION =
        Preference<String?>(
            key = "last_prompted_version",
            default = null,
        )

    // -------------------------
    // ADVANCED
    // -------------------------

    val LEGACY_PAIRING =
        Preference<Boolean>(
            key = "legacy_pairing",
            default = false,
        )

    // -------------------------
    // OTHER
    // -------------------------

    val AUTH_TOKEN =
        Preference<String?>(
            key = "auth_token",
            default = null,
        )
}
