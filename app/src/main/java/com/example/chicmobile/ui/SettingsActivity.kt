package com.example.chicmobile.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.chicmobile.config.AppConfig
import com.example.chicmobile.databinding.ActivitySettingsBinding
import com.example.chicmobile.work.WorkScheduler

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var config: AppConfig

    private val folderPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@registerForActivityResult

            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)

            config.folderTreeUri = uri.toString()
            binding.txtFolderPermission.text = "SAF permission granted: $uri"

            val resolvedPath = treeUriToPath(uri)
            if (resolvedPath != null) {
                config.folderPath = resolvedPath
                binding.txtFolderPath.text = "Folder path: $resolvedPath"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        config = AppConfig.getInstance(this)

        loadValues()

        binding.btnPickFolder.setOnClickListener {
            folderPickerLauncher.launch(null)
        }

        binding.btnSave.setOnClickListener {
            saveValues()
        }
    }

    private fun loadValues() = with(binding) {
        edtIntervalMinutes.setText(config.uploadIntervalMinutes.toString())
        txtFolderPath.text = "Folder path: ${config.folderPath}"
        txtFolderPermission.text = if (config.folderTreeUri.isNotBlank()) {
            "SAF permission granted: ${config.folderTreeUri}"
        } else {
            "No SAF folder permission granted"
        }
    }

    private fun saveValues() = with(binding) {
        val interval = edtIntervalMinutes.text.toString().toLongOrNull()
        if (interval == null || interval < 15L) {
            Toast.makeText(this@SettingsActivity, "Interval must be at least 15 minutes", Toast.LENGTH_LONG).show()
            return
        }

        if (config.folderTreeUri.isBlank()) {
            Toast.makeText(this@SettingsActivity, "Please pick folder once for scan/delete permission", Toast.LENGTH_LONG).show()
            return
        }

        config.uploadIntervalMinutes = interval
        config.extensionFilter = "m4a"

        if (!config.isConfigValid()) {
            Toast.makeText(this@SettingsActivity, "Please pick a valid folder", Toast.LENGTH_LONG).show()
            return
        }

        config.setupComplete = true
        WorkScheduler.ensurePeriodicWork(this@SettingsActivity)
        Toast.makeText(this@SettingsActivity, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun treeUriToPath(uri: Uri): String? {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            if (docId.startsWith("primary:")) {
                "/storage/emulated/0/" + docId.removePrefix("primary:")
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
