package moe.shizuku.manager.stealth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.shizuku.manager.R
import moe.shizuku.manager.utils.ApkUtils.changePackageName
import moe.shizuku.manager.utils.ApkUtils.workDir
import java.io.File

const val ORIGINAL_PACKAGE_NAME = "moe.shizuku.privileged.api"

sealed class UiState {
    data class Idle(
        val isShizukuHidden: Boolean,
    ) : UiState()

    object Loading : UiState()

    data class Success(
        val apk: File
    ) : UiState()

    data class Error(
        val error: Exception,
    ) : UiState()
}

class StealthTutorialViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val app: Application = getApplication()
    private val appContext = app.applicationContext

    private val _uiState = MutableLiveData<UiState>(UiState.Idle(false))
    val uiState: LiveData<UiState> = _uiState

    private var _packageName: String? = null

    private fun isShizukuHidden() =
        runCatching {
            appContext.packageManager.getPackageInfo(ORIGINAL_PACKAGE_NAME, 0)
        }.isFailure

    init {
        _uiState.value = UiState.Idle(isShizukuHidden())
    }

    fun setPackageName(packageName: String? = null) {
        _packageName = packageName ?: app.packageName.appendRandomSuffix()
    }

    fun createClone() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.postValue(UiState.Loading)

                val apk = File(app.applicationInfo.sourceDir)
                val newApk = apk.changePackageName(_packageName!!)

                _uiState.postValue(UiState.Success(newApk))
            } catch (e: Exception) {
                _uiState.postValue(UiState.Error(e))
                Log.e("StealthTutorialViewModel", "Error changing package name", e)
            }
        }
    }

    // TO-DO: NEW FUNCTION TO CREATE "SHELL" PACKAGE WITH NO LAUNCHER ICON ON UNHIDE

    override fun onCleared() {
        super.onCleared()
        workDir.deleteRecursively()
    }
}

fun String.validatePackageName(): Int? =
    when {
        this.isBlank() -> null

        !this.matches(Regex("^[a-zA-Z0-9.]+$")) ->
            R.string.stealth_error_invalid_characters

        this.split('.').any { it.firstOrNull()?.isDigit() == true } ->
            R.string.stealth_error_segment_starts_with_number

        this.split('.').size < 2 ->
            R.string.stealth_error_needs_two_segments

        this.endsWith('.') ->
            R.string.stealth_error_ends_with_period

        else -> null
    }

private fun String.appendRandomSuffix(): String {
    val letters = ('a'..'z')
    val chars = letters + ('0'..'9')

    val first = letters.random()
    val rest = (1..4).map { chars.random() }

    val randomSuffix = (listOf(first) + rest).joinToString("")

    return "$this.$randomSuffix"
}
