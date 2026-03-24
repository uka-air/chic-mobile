package com.example.chicmobile.ui

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chicmobile.config.AppConfig
import com.example.chicmobile.config.SyncHistoryEntry
import com.example.chicmobile.databinding.ActivityHistorySyncBinding
import com.example.chicmobile.file.FileScanner
import com.example.chicmobile.work.WorkScheduler
import java.text.DateFormat
import java.util.Date

class HistorySyncActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorySyncBinding
    private lateinit var config: AppConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorySyncBinding.inflate(layoutInflater)
        setContentView(binding.root)

        config = AppConfig.getInstance(this)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSyncNow.setOnClickListener {
            if (!config.setupComplete || !config.isConfigValid()) {
                Toast.makeText(this, "Complete setup before background upload / ตั้งค่าก่อนอัปโหลดเบื้องหลัง", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            WorkScheduler.runNow(this)
            Toast.makeText(this, "Background upload queued / เพิ่มคิวอัปโหลดเบื้องหลังแล้ว", Toast.LENGTH_SHORT).show()
            refreshContent()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshContent()
    }

    private fun refreshContent() {
        val pending = if (config.folderPath.isNotBlank()) {
            FileScanner.scanEligibleFiles(config.folderPath, config.extensionFilter).size
        } else {
            0
        }
        val lastRun = if (config.lastRunTime == 0L) {
            "Never / ยังไม่เคย"
        } else {
            DateFormat.getDateTimeInstance().format(Date(config.lastRunTime))
        }

        binding.txtHistorySummary.text = buildString {
            append("This page shows background upload history from this device / หน้านี้แสดงประวัติการอัปโหลดเบื้องหลังของเครื่องนี้\n")
            append("Pending files / ไฟล์ที่รอส่ง: $pending\n")
            append("Last upload time / เวลาอัปโหลดล่าสุด: $lastRun\n")
            append("Last result / ผลลัพธ์ล่าสุด: ${config.lastUploadResult}")
        }

        renderHistory(config.getSyncHistory())
    }

    private fun renderHistory(entries: List<SyncHistoryEntry>) {
        binding.historyContainer.removeAllViews()

        if (entries.isEmpty()) {
            binding.historyContainer.addView(
                createHistoryTextView(
                    "No background upload history yet / ยังไม่มีประวัติการอัปโหลดเบื้องหลัง"
                )
            )
            return
        }

        entries.forEachIndexed { index, entry ->
            val formattedTime = DateFormat.getDateTimeInstance().format(Date(entry.timestamp))
            val statusLabel = when {
                entry.failureCount == 0 && entry.totalCount > 0 -> "Success / สำเร็จ"
                entry.failureCount > 0 && entry.successCount > 0 -> "Partial success / สำเร็จบางส่วน"
                entry.failureCount > 0 -> "Failed / ล้มเหลว"
                else -> "Info / ข้อมูล"
            }
            val text = buildString {
                append("Upload #${index + 1} / รายการอัปโหลดที่ ${index + 1}\n")
                append("Status / สถานะ: $statusLabel\n")
                append("Event / เหตุการณ์: ${entry.title}\n")
                append("Time / เวลา: $formattedTime\n")
                append("Files attempted / จำนวนไฟล์ที่พยายามส่ง: ${entry.totalCount}\n")
                append("Success / สำเร็จ: ${entry.successCount}\n")
                append("Failed / ล้มเหลว: ${entry.failureCount}\n")
                append("Details / รายละเอียด: ${entry.details}")
            }
            binding.historyContainer.addView(createHistoryTextView(text))
        }
    }

    private fun createHistoryTextView(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(getColor(android.R.color.white))
            textSize = 14f
            setPadding(0, 0, 0, 24)
        }
    }
}
