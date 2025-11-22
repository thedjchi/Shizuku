package moe.shizuku.manager.starter

import androidx.lifecycle.asFlow
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import moe.shizuku.manager.application
import moe.shizuku.manager.utils.ShizukuStateMachine

object Starter {

    private val starterFile = File(application.applicationInfo.nativeLibraryDir, "libshizuku.so")

    val userCommand: String = starterFile.absolutePath

    val adbCommand = "adb shell $userCommand"

    val internalCommand = "$userCommand --apk=${application.applicationInfo.sourceDir}"

    private class BinderTimeoutException:
        Exception("Failed to receive binder within 10 seconds")

    suspend fun waitForBinder(log: ((String) -> Unit)? = null) {
        try {
            log?.invoke("\nWaiting for service...")
            withTimeout(10_000) {
                ShizukuStateMachine.asFlow()
                    .first { it == ShizukuStateMachine.State.RUNNING }
            }
            log?.invoke("Service started, this window will be automatically closed in 3 seconds")
        } catch (e: TimeoutCancellationException) {
            throw BinderTimeoutException()
        }
    }

}
