package com.example.chicmobile.ui

import android.os.Bundle
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

        loadValues()

        binding.btnSave.setOnClickListener {
            saveValues()
        }
    }

    private fun loadValues() = with(binding) {
        edtFolderPath.setText(config.folderPath)
        edtIntervalMinutes.setText(config.uploadIntervalMinutes.toString())
    }

    private fun saveValues() = with(binding) {
        val interval = edtIntervalMinutes.text.toString().toLongOrNull()
        if (interval == null || interval < 15L) {
            Toast.makeText(this@SettingsActivity, "Interval must be at least 15 minutes", Toast.LENGTH_LONG).show()
            return
        }

        config.folderPath = edtFolderPath.text.toString()
        config.uploadIntervalMinutes = interval

        if (!config.isConfigValid()) {
            Toast.makeText(this@SettingsActivity, "Please enter a valid folder path", Toast.LENGTH_LONG).show()
            return
        }

        config.setupComplete = true
        WorkScheduler.ensurePeriodicWork(this@SettingsActivity)
        Toast.makeText(this@SettingsActivity, "Settings saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
