package com.example.chicmobile.network

import android.util.Log
import com.example.chicmobile.config.AppConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
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
        val rawAudioUrl = "${baseUrl}/${config.uploadEndpoint.trimStart('/')}"
        val presignUrl = "$baseUrl/api/v1/presigns"

        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            val message = "ค่า serverBaseUrl ไม่ถูกต้อง: '$baseUrl'"
            Log.e(TAG, message)
            return UploadResult.Failure(message)
        }

        if (!rawAudioUrl.startsWith("http://") && !rawAudioUrl.startsWith("https://")) {
            val message = "ค่าการตั้งค่า URL อัปโหลดไม่ถูกต้อง: '$rawAudioUrl'"
            Log.e(TAG, message)
            return UploadResult.Failure(message)
        }

        return try {
            val presign = fetchPresign(presignUrl) ?: return UploadResult.Retryable("ไม่สามารถขอ presign ได้")

            val uploadResult = uploadToPresignedUrl(presign.url, file)
            if (uploadResult != UploadResult.Success) {
                return uploadResult
            }

            notifyRawAudio(rawAudioUrl, presign.key)
        } catch (e: Exception) {
            Log.e(TAG, "Upload flow failed", e)
            UploadResult.Retryable("เกิดข้อผิดพลาด: ${e.message}")
        }
    }

    private fun fetchPresign(presignUrl: String): PresignResponse? {
        val body = "{}".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(presignUrl)
            .applyAuthHeader()
            .post(body)
            .build()

        Log.d(TAG, "Requesting presign at $presignUrl")
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            Log.d(TAG, "Presign response code=${response.code} body=$responseBody")

            if (!response.isSuccessful) {
                return null
            }

            val json = JSONObject(responseBody)
            val url = json.optString("url")
            val key = json.optString("key")
            if (url.isBlank() || key.isBlank()) {
                Log.e(TAG, "Presign response missing url/key")
                return null
            }
            return PresignResponse(url = url, key = key)
        }
    }

    private fun uploadToPresignedUrl(url: String, file: File): UploadResult {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return UploadResult.Failure("URL presigned ไม่ถูกต้อง: '$url'")
        }

        val request = Request.Builder()
            .url(url)
            .put(file.asRequestBody("application/octet-stream".toMediaTypeOrNull()))
            .build()

        Log.d(TAG, "Uploading file=${file.name} to presigned URL")
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            Log.d(TAG, "Presigned upload response code=${response.code} body=$responseBody")
            return when {
                response.isSuccessful -> UploadResult.Success
                response.code in 500..599 || response.code == 429 -> UploadResult.Retryable("การอัปโหลดผ่าน presigned URL ขัดข้องชั่วคราว: ${response.code}")
                else -> UploadResult.Failure("การอัปโหลดผ่าน presigned URL ถูกปฏิเสธ: ${response.code} $responseBody")
            }
        }
    }

    private fun notifyRawAudio(rawAudioUrl: String, key: String): UploadResult {
        val payload = JSONObject().put("key", key).toString()
        val body = payload.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(rawAudioUrl)
            .applyAuthHeader()
            .post(body)
            .build()

        Log.d(TAG, "Notifying raw_audios with key=$key")
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            Log.d(TAG, "raw_audios response code=${response.code} body=$responseBody")
            return when {
                response.isSuccessful -> UploadResult.Success
                response.code in 500..599 || response.code == 429 -> UploadResult.Retryable("raw_audios ขัดข้องชั่วคราว: ${response.code}")
                else -> UploadResult.Failure("raw_audios ถูกปฏิเสธ: ${response.code} $responseBody")
            }
        }
    }

    private fun Request.Builder.applyAuthHeader(): Request.Builder {
        if (config.authToken.isNotBlank()) {
            header("Authorization", "Bearer ${config.authToken}")
        }
        return this
    }

    private data class PresignResponse(val url: String, val key: String)

    companion object {
        private const val TAG = "UploadManager"
    }
}

sealed class UploadResult {
    data object Success : UploadResult()
    data class Retryable(val reason: String) : UploadResult()
    data class Failure(val reason: String) : UploadResult()
}
