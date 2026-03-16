package com.example.chicmobile.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.chicmobile.config.AppConfig
import com.example.chicmobile.file.FileScanner
import com.example.chicmobile.network.UploadManager
import com.example.chicmobile.network.UploadResult
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val config = AppConfig.getInstance(applicationContext)
        Log.d(TAG, "Worker started")

        if (!config.setupComplete || !config.isConfigValid()) {
            val message = "Setup incomplete or invalid config"
            Log.e(TAG, message)
            config.lastUploadResult = message
            return Result.success()
        }

        if (!config.allowMetered && isMeteredConnection()) {
            val message = "Metered network detected and disallowed by settings"
            Log.d(TAG, message)
            config.lastUploadResult = message
            return Result.retry()
        }

        val files = FileScanner.scanEligibleFiles(
            folderPath = config.folderPath,
            extensionFilter = config.extensionFilter,
            minAgeSeconds = 30
        )
        config.lastPendingCount = files.size

        Log.d(TAG, "Folder: ${config.folderPath}, found files=${files.size}")

        val uploadManager = UploadManager(config)
        var retryNeeded = false
        var successCount = 0
        var failureCount = 0

        files.forEach { sourceFile ->
            Log.d(TAG, "Upload attempt for file=${sourceFile.name}")

            when (val result = uploadManager.uploadFile(sourceFile)) {
                UploadResult.Success -> {
                    if (sourceFile.delete()) {
                        Log.d(TAG, "Upload success + delete success for file=${sourceFile.name}")
                    } else {
                        Log.w(TAG, "Upload succeeded but delete failed (likely scoped storage restriction): ${sourceFile.absolutePath}")
                    }
                    successCount++
                }

                is UploadResult.Retryable -> {
                    retryNeeded = true
                    failureCount++
                    Log.e(TAG, "Upload retryable failure for ${sourceFile.name}: ${result.reason}")
                }

                is UploadResult.Failure -> {
                    failureCount++
                    Log.e(TAG, "Upload permanent failure for ${sourceFile.name}: ${result.reason}")
                }
            }
        }

        val status = "Finished: success=$successCount, failures=$failureCount, total=${files.size}"
        config.lastRunTime = System.currentTimeMillis()
        config.lastUploadResult = status
        config.lastPendingCount = FileScanner.scanEligibleFiles(config.folderPath, config.extensionFilter).size
        Log.d(TAG, "Worker finish: $status")

        return if (retryNeeded) {
            Log.d(TAG, "Retry requested due to transient failure")
            Result.retry()
        } else {
            Result.success()
        }
    }

    private fun isMeteredConnection(): Boolean {
        val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return true
        val caps = cm.getNetworkCapabilities(network) ?: return true
        val unmetered = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        return !unmetered
    }

    companion object {
        private const val TAG = "UploadWorker"
    }
}
