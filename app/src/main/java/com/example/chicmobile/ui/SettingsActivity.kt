package com.example.chicmobile.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.chicmobile.R
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
            binding.txtFolderPermission.text = getString(R.string.saf_permission_granted, uri.toString())

            val resolvedPath = treeUriToPath(uri)
            if (resolvedPath != null) {
                config.folderPath = resolvedPath
                binding.txtFolderPath.text = getString(R.string.folder_path_template, resolvedPath)
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
        edtPhoneNumber.setText(config.phoneNumber)
        edtIntervalMinutes.setText(config.uploadIntervalMinutes.toString())
        edtUploadEndpoint.setText(config.uploadEndpoint)
        txtFolderPath.text = getString(R.string.folder_path_template, config.folderPath)
        txtFolderPermission.text = if (config.folderTreeUri.isNotBlank()) {
            getString(R.string.saf_permission_granted, config.folderTreeUri)
        } else {
            getString(R.string.no_saf_permission)
        }
    }

    private fun saveValues() = with(binding) {
        val interval = edtIntervalMinutes.text.toString().toLongOrNull()
        if (interval == null || interval < 15L) {
            Toast.makeText(this@SettingsActivity, getString(R.string.interval_minimum), Toast.LENGTH_LONG).show()
            return
        }

        if (config.folderTreeUri.isBlank()) {
            Toast.makeText(this@SettingsActivity, getString(R.string.pick_folder_once), Toast.LENGTH_LONG).show()
            return
        }

        if (edtUploadEndpoint.text.toString().isBlank()) {
            Toast.makeText(this@SettingsActivity, getString(R.string.upload_endpoint_required), Toast.LENGTH_LONG).show()
            return
        }

        config.phoneNumber = edtPhoneNumber.text.toString()
        config.uploadIntervalMinutes = interval
        config.uploadEndpoint = edtUploadEndpoint.text.toString()
        config.extensionFilter = "m4a"

        if (!config.isConfigValid()) {
            Toast.makeText(this@SettingsActivity, getString(R.string.pick_valid_folder), Toast.LENGTH_LONG).show()
            return
        }

        config.setupComplete = true
        WorkScheduler.ensurePeriodicWork(this@SettingsActivity)
        Toast.makeText(this@SettingsActivity, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
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
