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
                Toast.makeText(this, "Complete setup before syncing history / ตั้งค่าก่อนซิงก์ประวัติ", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            WorkScheduler.runNow(this)
            Toast.makeText(this, "History sync queued / เพิ่มงานซิงก์ประวัติแล้ว", Toast.LENGTH_SHORT).show()
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
        binding.txtHistorySummary.text = buildString {
            append("Ready to sync call history to the server / พร้อมซิงก์ประวัติการโทรขึ้นเซิร์ฟเวอร์\n")
            append("Pending files / ไฟล์ที่รอส่ง: $pending\n")
            append("Last result / ผลลัพธ์ล่าสุด: ${config.lastUploadResult}")
        }

        renderHistory(config.getSyncHistory())
    }

    private fun renderHistory(entries: List<SyncHistoryEntry>) {
        binding.historyContainer.removeAllViews()

        if (entries.isEmpty()) {
            binding.historyContainer.addView(createHistoryTextView("No history sync attempts yet / ยังไม่มีประวัติการซิงก์"))
            return
        }

        entries.forEach { entry ->
            val formattedTime = DateFormat.getDateTimeInstance().format(Date(entry.timestamp))
            val text = buildString {
                append(entry.title)
                append("\n")
                append(formattedTime)
                append("\n")
                append(entry.details)
                append("\n")
                append("Uploaded / อัปโหลดแล้ว ${entry.successCount} จาก ${entry.totalCount}; failures / ล้มเหลว ${entry.failureCount}")
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
