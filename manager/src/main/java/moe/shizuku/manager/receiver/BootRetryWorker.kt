package moe.shizuku.manager.receiver

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import moe.shizuku.manager.utils.HeadlessLogger
import moe.shizuku.manager.utils.ShizukuStateMachine

class BootRetryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val VERIFY_DELAY_MS = 3000L
    }

    override suspend fun doWork(): Result {
        if (runAttemptCount > MAX_ATTEMPTS) {
            HeadlessLogger.w("BootRetry", "Max attempts ($MAX_ATTEMPTS) reached, giving up")
            return Result.success()
        }

        if (ShizukuStateMachine.isRunning()) {
            HeadlessLogger.i("BootRetry", "Shizuku already running (attempt $runAttemptCount)")
            return Result.success()
        }

        HeadlessLogger.i("BootRetry", "Retrying Shizuku start (attempt $runAttemptCount/$MAX_ATTEMPTS)")
        ShizukuReceiverStarter.start(applicationContext)

        delay(VERIFY_DELAY_MS)
        if (ShizukuStateMachine.isRunning()) {
            HeadlessLogger.i("BootRetry", "Start succeeded (attempt $runAttemptCount)")
            return Result.success()
        }

        HeadlessLogger.w("BootRetry", "Start not yet running, will retry (attempt $runAttemptCount)")
        return Result.retry()
    }
}
