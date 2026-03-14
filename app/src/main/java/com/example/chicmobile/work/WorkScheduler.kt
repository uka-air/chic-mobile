package com.example.chicmobile.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.chicmobile.config.AppConfig
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val PERIODIC_WORK_NAME = "periodic_file_upload"

    fun ensurePeriodicWork(context: Context) {
        val config = AppConfig.getInstance(context)
        val intervalMinutes = config.uploadIntervalMinutes.coerceAtLeast(15L)

        val periodicRequest = PeriodicWorkRequestBuilder<UploadWorker>(intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "manual_upload",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
