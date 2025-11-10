package moe.shizuku.manager

import android.app.Application
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.topjohnwu.superuser.Shell
import moe.shizuku.manager.ktx.logd
import moe.shizuku.manager.service.WatchdogService
import moe.shizuku.manager.utils.ShizukuStateMachine
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.core.util.BuildUtils.atLeast30
import rikka.material.app.LocaleDelegate
import rikka.shizuku.Shizuku

lateinit var application: ShizukuApplication

class ShizukuApplication : Application() {

    companion object {

        init {
            logd("ShizukuApplication", "init")

            Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_REDIRECT_STDERR))
            if (Build.VERSION.SDK_INT >= 28) {
                HiddenApiBypass.setHiddenApiExemptions("")
            }
            if (atLeast30) {
                System.loadLibrary("adb")
            }
        }

        lateinit var appContext: Context
            private set

    }

    private fun init(context: Context) {
        ShizukuSettings.initialize(context)
        LocaleDelegate.defaultLocale = ShizukuSettings.getLocale()
        AppCompatDelegate.setDefaultNightMode(ShizukuSettings.getNightMode())

        ShizukuStateMachine.init()
        if(ShizukuSettings.getWatchdog() && !ShizukuSettings.isWatchdogRunning()) {
            try {
                WatchdogService.start(context)
            } catch (e: Exception) {
                if (e !is ForegroundServiceStartNotAllowedException)
                    Log.e("ShizukuApplication", "Failed to start WatchdogService: ${e.message}" )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        application = this
        appContext = applicationContext
        init(this)
    }

}
