package moe.shizuku.manager.management

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.shizuku.manager.authorization.AuthorizationManager
import rikka.lifecycle.Resource

class AppsViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>().applicationContext

    private val _packages = MutableLiveData<Resource<List<PackageInfo>>>()
    val packages = _packages as LiveData<Resource<List<PackageInfo>>>

    private val _grantedCount = MutableLiveData<Resource<Int>>()
    val grantedCount = _grantedCount as LiveData<Resource<Int>>

    fun load(onlyCount: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list: MutableList<PackageInfo> = ArrayList()
                var count = 0
                for (pi in AuthorizationManager.getPackages(
                    exclude = listOf(appContext.packageName)
                )) {
                    list.add(pi)
                    if (AuthorizationManager.granted(pi.packageName, pi.applicationInfo!!.uid)) count++
                }
                if (!onlyCount) _packages.postValue(Resource.success(list))
                _grantedCount.postValue(Resource.success(count))
            } catch (e: CancellationException) {

            } catch (e: Throwable) {
                _packages.postValue(Resource.error(e, null))
                _grantedCount.postValue(Resource.error(e, 0))
            }
        }
    }
    
}
