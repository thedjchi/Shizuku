package moe.shizuku.manager.starter

import androidx.lifecycle.asFlow
import java.io.File
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import moe.shizuku.manager.ShizukuApplication
import moe.shizuku.manager.utils.ShizukuStateMachine

private val app = ShizukuApplication.application

object Starter {

    private val starterFile = File(app.applicationInfo.nativeLibraryDir, "libshizuku.so")

    val userCommand: String = starterFile.absolutePath
    val adbCommand = "adb shell $userCommand"
    val internalCommand = "$userCommand --apk=${app.applicationInfo.sourceDir}"

    val serviceStartedMessage = "Service started, this window will be automatically closed in 3 seconds"

    suspend fun waitForBinder(log: ((String) -> Unit)? = null) {
        try {
            log?.invoke("\nWaiting for service. This may take up to 1 minute...")
            withTimeout(60_000) {
                ShizukuStateMachine.asFlow()
                    .first { it == ShizukuStateMachine.State.RUNNING }
            }
            log?.invoke(serviceStartedMessage)
        } catch (e: TimeoutCancellationException) {
            throw TimeoutException("Failed to receive binder within 1 minute")
        }
    }

}
