package com.example.chicmobile.config

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

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
        get() = prefs.getString(KEY_SERVER_BASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SERVER_BASE_URL, value.trim()).apply()

    var uploadEndpoint: String
        get() = prefs.getString(KEY_UPLOAD_ENDPOINT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_UPLOAD_ENDPOINT, value.trim()).apply()

    var deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value.trim()).apply()

    var siteId: String
        get() = prefs.getString(KEY_SITE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SITE_ID, value.trim()).apply()

    var uploadIntervalMinutes: Long
        get() = prefs.getLong(KEY_UPLOAD_INTERVAL_MINUTES, 15L)
        set(value) = prefs.edit().putLong(KEY_UPLOAD_INTERVAL_MINUTES, value).apply()

    var folderPath: String
        get() = prefs.getString(KEY_FOLDER_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FOLDER_PATH, value.trim()).apply()

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
        get() = prefs.getString(KEY_LAST_UPLOAD_RESULT, "No uploads yet") ?: "No uploads yet"
        set(value) = prefs.edit().putString(KEY_LAST_UPLOAD_RESULT, value).apply()

    var lastPendingCount: Int
        get() = prefs.getInt(KEY_LAST_PENDING_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_PENDING_COUNT, value).apply()

    fun isConfigValid(): Boolean {
        return siteId.isNotBlank() && uploadIntervalMinutes >= 15L
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
        private const val KEY_UPLOAD_INTERVAL_MINUTES = "upload_interval_minutes"
        private const val KEY_FOLDER_PATH = "folder_path"
        private const val KEY_EXTENSION_FILTER = "extension_filter"
        private const val KEY_ALLOW_METERED = "allow_metered"
        private const val KEY_LOGGING_LEVEL = "logging_level"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_LAST_RUN_TIME = "last_run_time"
        private const val KEY_LAST_UPLOAD_RESULT = "last_upload_result"
        private const val KEY_LAST_PENDING_COUNT = "last_pending_count"

        @Volatile
        private var instance: AppConfig? = null

        fun getInstance(context: Context): AppConfig {
            return instance ?: synchronized(this) {
                instance ?: AppConfig(context.applicationContext).also { instance = it }
            }
        }
    }
}
