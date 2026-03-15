package com.example.chicmobile.network

import android.util.Log
import com.example.chicmobile.config.AppConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class UploadManager(private val config: AppConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun uploadFile(file: File): UploadResult {
        val baseUrl = config.serverBaseUrl.trimEnd('/')
        val endpoint = config.uploadEndpoint.trimStart('/')
        val url = "$baseUrl/$endpoint"

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            val message = "Invalid upload URL config: '$url'. Please configure server URL and endpoint."
            Log.e(TAG, message)
            return UploadResult.Failure(message)
        }

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("deviceId", config.deviceId)
            .addFormDataPart("siteId", config.siteId)
            .addFormDataPart("fileName", file.name)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            )
            .build()

        return try {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${config.authToken}")
                .post(body)
                .build()

            Log.d(TAG, "Uploading file=${file.name} to $url")
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                Log.d(TAG, "Server response code=${response.code} body=$responseBody")
                if (response.isSuccessful) {
                    UploadResult.Success
                } else if (response.code in 500..599 || response.code == 429) {
                    UploadResult.Retryable("Server temporary error: ${response.code}")
                } else {
                    UploadResult.Failure("Server rejected upload: ${response.code} $responseBody")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed due to network/IO issue", e)
            UploadResult.Retryable("Exception: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "UploadManager"
    }
}

sealed class UploadResult {
    data object Success : UploadResult()
    data class Retryable(val reason: String) : UploadResult()
    data class Failure(val reason: String) : UploadResult()
}
