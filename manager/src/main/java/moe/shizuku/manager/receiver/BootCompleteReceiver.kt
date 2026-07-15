package moe.shizuku.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.service.WatchdogService
import moe.shizuku.manager.utils.HeadlessLogger
import java.util.concurrent.TimeUnit

class BootCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        HeadlessLogger.i("Boot", "Boot completed, starting Shizuku")
        ShizukuReceiverStarter.start(context)
        if (ShizukuSettings.getWatchdog()) WatchdogService.start(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_ROAMING)
            .build()

        val retry = OneTimeWorkRequestBuilder<BootRetryWorker>()
            .setConstraints(constraints)
            .setInitialDelay(10, TimeUnit.SECONDS)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10, TimeUnit.SECONDS
            )
            .addTag("boot_retry")
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("boot_retry", ExistingWorkPolicy.REPLACE, retry)

        HeadlessLogger.i("Boot", "Boot retry scheduled (10s initial, EXP backoff, max 5 attempts)")
    }
}
