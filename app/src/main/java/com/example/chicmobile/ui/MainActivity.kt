package com.example.chicmobile.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.chicmobile.config.AppConfig
import com.example.chicmobile.databinding.ActivityMainBinding
import com.example.chicmobile.file.FileScanner
import com.example.chicmobile.work.WorkScheduler
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var config: AppConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        config = AppConfig.getInstance(this)

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnRunNow.setOnClickListener {
            WorkScheduler.runNow(this)
            binding.txtLastResult.text = "Manual upload queued"
        }

        if (!config.setupComplete) {
            startActivity(Intent(this, SettingsActivity::class.java).putExtra("force_setup", true))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        binding.txtStatus.text = if (config.isConfigValid()) "Configured" else "Setup required"

        val lastRun = if (config.lastRunTime == 0L) {
            "Never"
        } else {
            DateFormat.getDateTimeInstance().format(Date(config.lastRunTime))
        }
        binding.txtLastRun.text = lastRun
        binding.txtLastResult.text = config.lastUploadResult

        val pending = if (config.folderPath.isNotBlank()) {
            FileScanner.scanEligibleFiles(config.folderPath, config.extensionFilter).size
        } else {
            0
        }
        binding.txtPending.text = pending.toString()
    }
}
