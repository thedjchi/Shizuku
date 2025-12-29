package moe.shizuku.manager.stealth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import moe.shizuku.manager.R

const val ORIGINAL_PACKAGE_NAME = "moe.shizuku.privileged.api"

sealed class UiState {
    data object Hide : UiState()
    data object Unhide : UiState()
}

fun String.validatePackageName(): Int? = when {
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

class StealthTutorialViewModel(application: Application) : AndroidViewModel(application) {
    private val app: Application = getApplication()
    private val appContext = app.applicationContext

    private fun isShizukuHidden() =
        runCatching {
            appContext.packageManager.getPackageInfo(ORIGINAL_PACKAGE_NAME, 0)
        }.isFailure

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    init {
        _uiState.value =
            if (isShizukuHidden()) UiState.Unhide else UiState.Hide
    }

    fun onHide() {
        // TO-DO: START PATCHER ACTIVITY
    }

    fun onUnhide(temporary: Boolean) {
        // TO-DO: START PATCHER ACTIVITY
    }
}