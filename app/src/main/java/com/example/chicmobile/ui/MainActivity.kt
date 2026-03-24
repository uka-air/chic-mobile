package com.example.chicmobile.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.chicmobile.R
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
                Toast.makeText(this, getString(R.string.storage_permission_required), Toast.LENGTH_LONG).show()
            }
            refreshStatus()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        config = AppConfig.getInstance(this)

        binding.btnSettings.setOnClickListener {
            promptForSettingsPasscode()
        }

        binding.btnHistorySync.setOnClickListener {
            startActivity(Intent(this, HistorySyncActivity::class.java))
        }

        binding.btnRunNow.setOnClickListener {
            if (!hasStoragePermission()) {
                requestStoragePermissionIfNeeded()
                binding.txtLastResult.text = getString(R.string.grant_permission_try_again)
                return@setOnClickListener
            }
            WorkScheduler.runNow(this)
            binding.txtLastResult.text = getString(R.string.manual_upload_queued)
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

    private fun promptForSettingsPasscode() {
        val horizontalMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            24f,
            resources.displayMetrics,
        ).toInt()
        val topMargin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            8f,
            resources.displayMetrics,
        ).toInt()
        val input = EditText(this).apply {
            hint = "กรุณาใส่รหัส"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setBackgroundResource(R.drawable.bg_textbox_solid_border)
            setPadding(12, 8, 12, 8)
            setTextColor(Color.BLACK)
            setHintTextColor(Color.DKGRAY)
        }
        val inputContainer = FrameLayout(this).apply {
            addView(
                input,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    leftMargin = horizontalMargin
                    rightMargin = horizontalMargin
                    this.topMargin = topMargin
                },
            )
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("การเข้าถึงถูกจำกัด")
            .setMessage("กรุณาใส่รหัส")
            .setView(inputContainer)
            .setPositiveButton("ปลดล็อคสำเร็จ") { _, _ ->
                if (input.text.toString() == SETTINGS_PASSCODE) {
                    startActivity(Intent(this, SettingsActivity::class.java))
                } else {
                    Toast.makeText(this, "ใส่รหัสผิด", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("ยกเลิก", null)
            .show()

        dialog.window?.decorView?.setBackgroundColor(Color.WHITE)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }

    private fun refreshStatus() {
        val configReady = config.isConfigValid()
        val storageGranted = hasStoragePermission()
        binding.txtStatus.text = if (configReady && storageGranted) {
            getString(R.string.status_configured)
        } else {
            getString(R.string.status_setup_required)
        }

        val lastRun = if (config.lastRunTime == 0L) {
            getString(R.string.never)
        } else {
            DateFormat.getDateTimeInstance().format(Date(config.lastRunTime))
        }
        binding.txtLastRun.text = lastRun
        binding.txtLastResult.text = if (storageGranted) {
            config.lastUploadResult
        } else {
            getString(R.string.grant_permission_to_scan_files)
        }

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

    companion object {
        private const val SETTINGS_PASSCODE = "0101"
    }
}
