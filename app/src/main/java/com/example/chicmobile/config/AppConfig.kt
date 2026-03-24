package com.example.chicmobile.config

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedHashSet

class AppConfig private constructor(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val securePrefs by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Falling back to regular preferences for token storage", e)
            prefs
        }
    }

    var serverBaseUrl: String
        get() = prefs.getString(KEY_SERVER_BASE_URL, DEFAULT_SERVER_BASE_URL) ?: DEFAULT_SERVER_BASE_URL
        set(value) = prefs.edit().putString(KEY_SERVER_BASE_URL, value.trim()).apply()

    var uploadEndpoint: String
        get() = prefs.getString(KEY_UPLOAD_ENDPOINT, DEFAULT_UPLOAD_ENDPOINT) ?: DEFAULT_UPLOAD_ENDPOINT
        set(value) = prefs.edit().putString(KEY_UPLOAD_ENDPOINT, value.trim()).apply()

    var deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value.trim()).apply()

    var siteId: String
        get() = prefs.getString(KEY_SITE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SITE_ID, value.trim()).apply()

    var phoneNumber: String
        get() = prefs.getString(KEY_PHONE_NUMBER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PHONE_NUMBER, value.trim()).apply()

    var uploadIntervalMinutes: Long
        get() = prefs.getLong(KEY_UPLOAD_INTERVAL_MINUTES, 15L)
        set(value) = prefs.edit().putLong(KEY_UPLOAD_INTERVAL_MINUTES, value).apply()

    var folderPath: String
        get() = prefs.getString(KEY_FOLDER_PATH, DEFAULT_FOLDER_PATH) ?: DEFAULT_FOLDER_PATH
        set(value) = prefs.edit().putString(KEY_FOLDER_PATH, value.trim()).apply()

    var folderTreeUri: String
        get() = prefs.getString(KEY_FOLDER_TREE_URI, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FOLDER_TREE_URI, value.trim()).apply()

    var extensionFilter: String
        get() = prefs.getString(KEY_EXTENSION_FILTER, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EXTENSION_FILTER, value.trim()).apply()

    var allowMetered: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_METERED, false)
        set(value) = prefs.edit().putBoolean(KEY_ALLOW_METERED, value).apply()

    var loggingLevel: String
        get() = prefs.getString(KEY_LOGGING_LEVEL, "DEBUG") ?: "DEBUG"
        set(value) = prefs.edit().putString(KEY_LOGGING_LEVEL, value).apply()

    var authToken: String
        get() = securePrefs.getString(KEY_AUTH_TOKEN, "") ?: ""
        set(value) = securePrefs.edit().putString(KEY_AUTH_TOKEN, value.trim()).apply()

    var setupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETE, value).apply()

    var lastRunTime: Long
        get() = prefs.getLong(KEY_LAST_RUN_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_RUN_TIME, value).apply()

    var lastUploadResult: String
        get() = prefs.getString(KEY_LAST_UPLOAD_RESULT, "ยังไม่มีการอัปโหลด") ?: "ยังไม่มีการอัปโหลด"
        set(value) = prefs.edit().putString(KEY_LAST_UPLOAD_RESULT, value).apply()

    var lastPendingCount: Int
        get() = prefs.getInt(KEY_LAST_PENDING_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_PENDING_COUNT, value).apply()

    fun isConfigValid(): Boolean {
        return folderPath.isNotBlank() && uploadIntervalMinutes >= 15L
    }

    fun hasUploadedFingerprint(fingerprint: String): Boolean {
        if (fingerprint.isBlank()) return false
        return prefs.getStringSet(KEY_UPLOADED_FINGERPRINTS, emptySet())?.contains(fingerprint) == true
    }

    fun markUploadedFingerprint(fingerprint: String) {
        if (fingerprint.isBlank()) return
        val current = prefs.getStringSet(KEY_UPLOADED_FINGERPRINTS, emptySet()) ?: emptySet()
        val updated = LinkedHashSet(current)
        updated.add(fingerprint)

        while (updated.size > MAX_UPLOADED_FINGERPRINTS) {
            val it = updated.iterator()
            if (it.hasNext()) {
                it.next()
                it.remove()
            }
        }

        prefs.edit().putStringSet(KEY_UPLOADED_FINGERPRINTS, updated).apply()
    }

    fun removeUploadedFingerprint(fingerprint: String) {
        if (fingerprint.isBlank()) return
        val current = prefs.getStringSet(KEY_UPLOADED_FINGERPRINTS, emptySet()) ?: emptySet()
        if (!current.contains(fingerprint)) return
        val updated = LinkedHashSet(current)
        updated.remove(fingerprint)
        prefs.edit().putStringSet(KEY_UPLOADED_FINGERPRINTS, updated).apply()
    }

    fun getSyncHistory(): List<SyncHistoryEntry> {
        val raw = prefs.getString(KEY_SYNC_HISTORY, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        SyncHistoryEntry(
                            timestamp = item.optLong("timestamp"),
                            title = item.optString("title"),
                            details = item.optString("details"),
                            successCount = item.optInt("successCount"),
                            failureCount = item.optInt("failureCount"),
                            totalCount = item.optInt("totalCount"),
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse sync history", e)
            emptyList()
        }
    }

    fun appendSyncHistory(entry: SyncHistoryEntry) {
        val trimmed = ArrayList(getSyncHistory().take(MAX_SYNC_HISTORY_ITEMS - 1))
        trimmed.add(0, entry)

        val payload = JSONArray()
        trimmed.take(MAX_SYNC_HISTORY_ITEMS).forEach { item ->
            payload.put(
                JSONObject()
                    .put("timestamp", item.timestamp)
                    .put("title", item.title)
                    .put("details", item.details)
                    .put("successCount", item.successCount)
                    .put("failureCount", item.failureCount)
                    .put("totalCount", item.totalCount)
            )
        }

        prefs.edit().putString(KEY_SYNC_HISTORY, payload.toString()).apply()
    }

    companion object {
        private const val TAG = "AppConfig"
        private const val PREFS_NAME = "app_config"
        private const val SECURE_PREFS_NAME = "secure_app_config"

        private const val KEY_SERVER_BASE_URL = "server_base_url"
        private const val KEY_UPLOAD_ENDPOINT = "upload_endpoint"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_SITE_ID = "site_id"
        private const val KEY_PHONE_NUMBER = "phone_number"
        private const val KEY_UPLOAD_INTERVAL_MINUTES = "upload_interval_minutes"
        private const val KEY_FOLDER_PATH = "folder_path"
        private const val KEY_FOLDER_TREE_URI = "folder_tree_uri"
        private const val KEY_EXTENSION_FILTER = "extension_filter"
        private const val KEY_ALLOW_METERED = "allow_metered"
        private const val KEY_LOGGING_LEVEL = "logging_level"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_LAST_RUN_TIME = "last_run_time"
        private const val KEY_LAST_UPLOAD_RESULT = "last_upload_result"
        private const val KEY_LAST_PENDING_COUNT = "last_pending_count"
        private const val KEY_UPLOADED_FINGERPRINTS = "uploaded_fingerprints"
        private const val KEY_SYNC_HISTORY = "sync_history"
        private const val MAX_UPLOADED_FINGERPRINTS = 2000
        private const val MAX_SYNC_HISTORY_ITEMS = 20
        private const val DEFAULT_FOLDER_PATH = "/storage/emulated/0/Recordings/Record/Call"
        private const val DEFAULT_SERVER_BASE_URL = "https://chic-conversation-analyzer.onrender.com"
        private const val DEFAULT_UPLOAD_ENDPOINT = "api/v1/raw_audios"

        @Volatile
        private var instance: AppConfig? = null

        fun getInstance(context: Context): AppConfig {
            return instance ?: synchronized(this) {
                instance ?: AppConfig(context.applicationContext).also { instance = it }
            }
        }
    }
}

data class SyncHistoryEntry(
    val timestamp: Long,
    val title: String,
    val details: String,
    val successCount: Int,
    val failureCount: Int,
    val totalCount: Int,
)
