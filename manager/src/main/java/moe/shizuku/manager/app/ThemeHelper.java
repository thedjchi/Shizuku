package moe.shizuku.manager.app;

import android.content.Context;
import android.os.Build;

import androidx.annotation.StyleRes;

import moe.shizuku.manager.R;
import moe.shizuku.manager.ShizukuSettings;
import moe.shizuku.manager.utils.EnvironmentUtils;
import rikka.core.util.ResourceUtils;

public class ThemeHelper {

    private static final String THEME_DEFAULT = "DEFAULT";
    private static final String THEME_BLACK = "BLACK";

    public static boolean isBlackNightTheme(Context context) {
        return ShizukuSettings.getPreferences().getBoolean(ShizukuSettings.Keys.KEY_BLACK_NIGHT_THEME, EnvironmentUtils.isWatch(context));
    }

    public static boolean isUsingSystemColor() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ShizukuSettings.getPreferences().getBoolean(ShizukuSettings.Keys.KEY_USE_SYSTEM_COLOR, true);
    }

    public static String getTheme(Context context) {
        if (isBlackNightTheme(context)
                && ResourceUtils.isNightMode(context.getResources().getConfiguration()))
            return THEME_BLACK;

        return ShizukuSettings.getPreferences().getString(ShizukuSettings.Keys.KEY_LIGHT_THEME, THEME_DEFAULT);
    }

    @StyleRes
    public static int getThemeStyleRes(Context context) {
        switch (getTheme(context)) {
            case THEME_BLACK:
                return R.style.ThemeOverlay_Black;
            case THEME_DEFAULT:
            default:
                return R.style.ThemeOverlay;
        }
    }
}
