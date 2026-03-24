package com.example.chicmobile.work

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.chicmobile.config.AppConfig
import com.example.chicmobile.config.SyncHistoryEntry
import com.example.chicmobile.file.FileScanner
import com.example.chicmobile.network.UploadManager
import com.example.chicmobile.network.UploadResult

class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val config = AppConfig.getInstance(applicationContext)
        Log.d(TAG, "Worker started")

        if (!config.setupComplete || !config.isConfigValid()) {
            val message = "การตั้งค่ายังไม่ครบหรือไม่ถูกต้อง"
            Log.e(TAG, message)
            config.lastUploadResult = message
            config.appendSyncHistory(
                SyncHistoryEntry(
                    timestamp = System.currentTimeMillis(),
                    title = "บล็อกการอัปโหลดเบื้องหลัง",
                    details = message,
                    successCount = 0,
                    failureCount = 0,
                    totalCount = 0,
                )
            )
            return Result.success()
        }

        if (!config.allowMetered && isMeteredConnection()) {
            val message = "ตรวจพบเครือข่ายแบบคิดค่าบริการตามปริมาณข้อมูลและไม่ได้รับอนุญาตจากการตั้งค่า"
            Log.d(TAG, message)
            config.lastUploadResult = message
            config.appendSyncHistory(
                SyncHistoryEntry(
                    timestamp = System.currentTimeMillis(),
                    title = "เลื่อนการอัปโหลดเบื้องหลัง",
                    details = message,
                    successCount = 0,
                    failureCount = 0,
                    totalCount = 0,
                )
            )
            return Result.retry()
        }

        val scannedFiles = FileScanner.scanEligibleFiles(
            folderPath = config.folderPath,
            extensionFilter = config.extensionFilter,
            minAgeSeconds = 30
        )
        val alreadyUploadedFiles = scannedFiles.filter { config.hasUploadedFingerprint(fileFingerprint(it)) }
        val files = scannedFiles.filterNot { config.hasUploadedFingerprint(fileFingerprint(it)) }
        config.lastPendingCount = files.size

        Log.d(TAG, "Folder: ${config.folderPath}, scanned=${scannedFiles.size}, pending=${files.size}, alreadyUploaded=${alreadyUploadedFiles.size}")

        alreadyUploadedFiles.forEach { uploadedFile ->
            val fingerprint = fileFingerprint(uploadedFile)
            val deleted = uploadedFile.delete() || deleteViaSafTree(config.folderTreeUri, uploadedFile.name)
            if (deleted) {
                config.removeUploadedFingerprint(fingerprint)
                Log.d(TAG, "Deleted previously-uploaded local file: ${uploadedFile.name}")
            }
        }

        val uploadManager = UploadManager(config)
        var retryNeeded = false
        var successCount = 0
        var failureCount = 0

        files.forEach { sourceFile ->
            val fingerprint = fileFingerprint(sourceFile)
            Log.d(TAG, "Upload attempt for file=${sourceFile.name}")

            when (val result = uploadManager.uploadFile(sourceFile)) {
                UploadResult.Success -> {
                    config.markUploadedFingerprint(fingerprint)
                    val deleted = sourceFile.delete() || deleteViaSafTree(config.folderTreeUri, sourceFile.name)
                    if (deleted) {
                        Log.d(TAG, "Upload success + delete success for file=${sourceFile.name}")
                    } else {
                        Log.i(TAG, "Upload success; keeping local file due to storage restrictions: ${sourceFile.absolutePath}")
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

        val status = "สถานะ : สำเร็จ = $successCount, ล้มเหลว = $failureCount, ทั้งหมด = ${files.size}"
        val completedAt = System.currentTimeMillis()
        config.lastRunTime = System.currentTimeMillis()
        config.lastUploadResult = status
        config.appendSyncHistory(
            SyncHistoryEntry(
                timestamp = completedAt,
                title = if (failureCount == 0) "Background upload completed / อัปโหลดเบื้องหลังเสร็จสิ้น" else "Background upload completed with issues / อัปโหลดเบื้องหลังเสร็จสิ้นแต่มีปัญหา",
                details = status,
                successCount = successCount,
                failureCount = failureCount,
                totalCount = files.size,
            )
        )
        config.lastPendingCount = FileScanner.scanEligibleFiles(config.folderPath, config.extensionFilter)
            .count { !config.hasUploadedFingerprint(fileFingerprint(it)) }
        Log.d(TAG, "Worker finish: $status")

        return if (retryNeeded) {
            Log.d(TAG, "Retry requested due to transient failure")
            Result.retry()
        } else {
            Result.success()
        }
    }

    private fun deleteViaSafTree(treeUriString: String, fileName: String): Boolean {
        if (treeUriString.isBlank()) return false
        return try {
            val resolver = applicationContext.contentResolver
            val treeUri = Uri.parse(treeUriString)
            val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId)

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            )

            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val displayName = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    if (displayName == fileName && idIndex >= 0) {
                        val docId = cursor.getString(idIndex)
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        val deleted = DocumentsContract.deleteDocument(resolver, documentUri)
                        if (deleted) {
                            Log.d(TAG, "Deleted via SAF tree: $fileName")
                        }
                        return deleted
                    }
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "SAF delete failed for $fileName", e)
            false
        }
    }

    private fun fileFingerprint(file: java.io.File): String {
        return "${file.absolutePath}:${file.length()}:${file.lastModified()}"
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
