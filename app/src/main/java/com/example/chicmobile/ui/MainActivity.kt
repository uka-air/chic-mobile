package com.example.chicmobile.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.chicmobile.config.AppConfig
import com.example.chicmobile.databinding.ActivityMainBinding
import com.example.chicmobile.file.FileScanner
import com.example.chicmobile.work.WorkScheduler
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var config: AppConfig

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val granted = grants.values.all { it }
            if (!granted) {
                Toast.makeText(this, "Storage permission is required to scan call recordings", Toast.LENGTH_LONG).show()
            }
            refreshStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        config = AppConfig.getInstance(this)

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnRunNow.setOnClickListener {
            if (!hasStoragePermission()) {
                requestStoragePermissionIfNeeded()
                binding.txtLastResult.text = "Grant storage permission and try again"
                return@setOnClickListener
            }
            WorkScheduler.runNow(this)
            binding.txtLastResult.text = "Manual upload queued"
        }

        if (!config.setupComplete) {
            startActivity(Intent(this, SettingsActivity::class.java).putExtra("force_setup", true))
        }

        requestStoragePermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val configReady = config.isConfigValid()
        val storageGranted = hasStoragePermission()
        binding.txtStatus.text = if (configReady && storageGranted) "Configured" else "Setup required"

        val lastRun = if (config.lastRunTime == 0L) {
            "Never"
        } else {
            DateFormat.getDateTimeInstance().format(Date(config.lastRunTime))
        }
        binding.txtLastRun.text = lastRun
        binding.txtLastResult.text = if (storageGranted) config.lastUploadResult else "Grant storage permission to scan files"

        val pending = if (storageGranted && config.folderPath.isNotBlank()) {
            FileScanner.scanEligibleFiles(config.folderPath, config.extensionFilter).size
        } else {
            0
        }
        binding.txtPending.text = pending.toString()
    }

    private fun hasStoragePermission(): Boolean {
        return requiredStoragePermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermissionIfNeeded() {
        val missing = requiredStoragePermissions().filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            storagePermissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun requiredStoragePermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}
