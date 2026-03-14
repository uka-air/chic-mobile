package com.example.chicmobile.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chicmobile.config.AppConfig
import com.example.chicmobile.databinding.ActivitySettingsBinding
import com.example.chicmobile.work.WorkScheduler

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var config: AppConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        config = AppConfig.getInstance(this)

        val levels = listOf("DEBUG", "INFO", "WARN", "ERROR")
        binding.spnLogging.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, levels)

        loadValues()

        binding.btnSave.setOnClickListener {
            saveValues()
        }
    }

    private fun loadValues() = with(binding) {
        edtServerBaseUrl.setText(config.serverBaseUrl)
        edtUploadEndpoint.setText(config.uploadEndpoint)
        edtToken.setText(config.authToken)
        edtDeviceId.setText(config.deviceId)
        edtSiteId.setText(config.siteId)
        edtFolderPath.setText(config.folderPath)
        edtExtensionFilter.setText(config.extensionFilter)
        swAllowMetered.isChecked = config.allowMetered

        val levelIndex = (spnLogging.adapter as ArrayAdapter<String>).getPosition(config.loggingLevel)
        if (levelIndex >= 0) {
            spnLogging.setSelection(levelIndex)
        }
    }

    private fun saveValues() = with(binding) {
        config.serverBaseUrl = edtServerBaseUrl.text.toString()
        config.uploadEndpoint = edtUploadEndpoint.text.toString()
        config.authToken = edtToken.text.toString()
        config.deviceId = edtDeviceId.text.toString()
        config.siteId = edtSiteId.text.toString()
        config.folderPath = edtFolderPath.text.toString()
        config.extensionFilter = edtExtensionFilter.text.toString()
        config.allowMetered = swAllowMetered.isChecked
        config.loggingLevel = spnLogging.selectedItem.toString()

        if (!config.isConfigValid()) {
            Toast.makeText(this@SettingsActivity, "Please fill all required fields and use HTTPS URL", Toast.LENGTH_LONG).show()
            return
        }

        config.setupComplete = true
        WorkScheduler.ensurePeriodicWork(this@SettingsActivity)
        Toast.makeText(this@SettingsActivity, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
