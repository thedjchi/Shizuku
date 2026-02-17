package moe.shizuku.manager.home

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.shizuku.manager.BuildConfig
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.model.ServiceStatus
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.Logger.LOGGER
import moe.shizuku.manager.utils.SettingsHelper
import moe.shizuku.manager.utils.ShizukuStateMachine
import moe.shizuku.manager.utils.ShizukuSystemApis
import rikka.lifecycle.Resource
import rikka.shizuku.Shizuku

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Context = getApplication<Application>().applicationContext

    private val _serviceStatus = MutableLiveData<Resource<ServiceStatus>>()
    val serviceStatus = _serviceStatus as LiveData<Resource<ServiceStatus>>

    private val _shouldShowBatteryOptimizationSnackbar = MutableLiveData<Boolean>(false)
    val shouldShowBatteryOptimizationSnackbar: LiveData<Boolean> = _shouldShowBatteryOptimizationSnackbar

    private val _shouldShowRebootDialog = MutableLiveData<Boolean>(false)
    val shouldShowRebootDialog: LiveData<Boolean> = _shouldShowRebootDialog

    private val _shouldShowUninstallDialog = MutableLiveData<Boolean>(false)
    val shouldShowUninstallDialog: LiveData<Boolean> = _shouldShowUninstallDialog

    private val shizukuPermissionGroup  = "moe.shizuku.manager.permission-group.API"
    private val shizukuPermission = "moe.shizuku.manager.permission.API_V23";

    private fun load(): ServiceStatus {
        // In certain cases when user re-installs Shizuku with different package name (e.g., when using stealth mode), the system doesn't recognize the Shizuku permission.
        // As a result, all permission operations (check/grant/revoke) will fail.
        // This is fixed by rebooting the device.
        // Run getPermissionGroupInfo() to trigger the exception. Then catch it and show a dialog prompting the user to reboot their device.
        try {
            val permissionGroup = appContext.packageManager.getPermissionGroupInfo(shizukuPermissionGroup, 0)
            val permission = appContext.packageManager.getPermissionInfo(shizukuPermission, 0)
            if (permission.packageName != appContext.packageName) {
                _shouldShowUninstallDialog.postValue(true)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            _shouldShowRebootDialog.postValue(true)
        }
        
        if (Shizuku.isPreV11() || (Shizuku.getVersion() == 11 && Shizuku.getServerPatchVersion() < 3)) {
            // disable authorized apps
        }

        if (!ShizukuStateMachine.isRunning()) {
            return ServiceStatus()
        }

        val uid = Shizuku.getUid()
        val apiVersion = Shizuku.getVersion()
        val patchVersion = Shizuku.getServerPatchVersion().let { if (it < 0) 0 else it }
        val seContext = if (apiVersion >= 6) {
            try {
                Shizuku.getSELinuxContext()
            } catch (tr: Throwable) {
                LOGGER.w(tr, "getSELinuxContext")
                null
            }
        } else null
        val permissionTest =
            Shizuku.checkRemotePermission("android.permission.GRANT_RUNTIME_PERMISSIONS") == PackageManager.PERMISSION_GRANTED

        // Before a526d6bb, server will not exit on uninstall, manager installed later will get not permission
        // Run a random remote transaction here, report no permission as not running
        ShizukuSystemApis.checkPermission(shizukuPermission, appContext.packageName, 0)
        return ServiceStatus(uid, apiVersion, patchVersion, seContext, permissionTest)
    }

    fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val status = load()
                _serviceStatus.postValue(Resource.success(status))
            } catch (e: CancellationException) {

            } catch (e: Throwable) {
                _serviceStatus.postValue(Resource.error(e, ServiceStatus()))
            }
        }
    }

    fun checkBatteryOptimization() {
        if (EnvironmentUtils.isTelevision()) return
        if (!ShizukuSettings.getStartOnBoot(appContext) && !ShizukuSettings.getWatchdog()) return
        _shouldShowBatteryOptimizationSnackbar.postValue(
            !SettingsHelper.isIgnoringBatteryOptimizations(appContext)
        )
    }

}
