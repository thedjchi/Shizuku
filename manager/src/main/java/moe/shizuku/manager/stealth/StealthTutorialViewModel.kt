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
import moe.shizuku.manager.utils.ApkUtils.*
import java.io.File

const val ORIGINAL_PACKAGE_NAME = "moe.shizuku.privileged.api"

sealed class UiState {
    data class Idle(
        val action: Action,
    ) : UiState()

    object Loading : UiState()

    data class Success(
        val apk: File,
        val apkType: ApkType
    ) : UiState()

    data class Error(
        val error: Exception,
    ) : UiState()
}

enum class Action {
    HIDE,
    UNHIDE,
    REHIDE
}

enum class ApkType {
    CLONE,
    STUB
}

class StealthTutorialViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val app: Application = getApplication()
    private val appContext = app.applicationContext

    private val _uiState = MutableLiveData<UiState>(UiState.Idle(Action.HIDE))
    val uiState: LiveData<UiState> = _uiState

    private var _packageName: String? = null

    private fun isShizukuHidden() =
        runCatching {
            appContext.packageManager.getPackageInfo(ORIGINAL_PACKAGE_NAME, 0)
        }.isFailure

    init {
        val action =
            if (isShizukuHidden()) Action.UNHIDE
            else if (app.packageName == ORIGINAL_PACKAGE_NAME) Action.HIDE
            else Action.REHIDE
        _uiState.value = UiState.Idle(action)
    }

    fun setPackageName(packageName: String? = null) {
        _packageName = packageName ?: app.packageName.appendRandomSuffix()
    }

    fun createApk(apkType: ApkType) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.postValue(UiState.Loading)

                val apk =
                    when (apkType) {
                        ApkType.CLONE -> {
                            val base = File(app.applicationInfo.sourceDir)
                            base.changePackageName(_packageName!!)
                        }
                        ApkType.STUB -> createStubApk(ORIGINAL_PACKAGE_NAME)
                    }

                _uiState.postValue(UiState.Success(apk, apkType))
            } catch (e: Exception) {
                _uiState.postValue(UiState.Error(e))
                Log.e("StealthTutorialViewModel", "Error changing package name", e)
            }
        }
    }

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
