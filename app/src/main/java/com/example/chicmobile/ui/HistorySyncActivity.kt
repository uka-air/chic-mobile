package com.example.chicmobile.ui

import android.graphics.Typeface
import android.os.Bundle
import android.widget.TableRow
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
                Toast.makeText(this, "กรุณาตั้งค่าก่อน", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            WorkScheduler.runNow(this)
            Toast.makeText(this, "เพิ่มคิวอัปโหลดแล้ว", Toast.LENGTH_SHORT).show()
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
            "ยังไม่มีประวัติการอัปโหลด"
        } else {
            DateFormat.getDateTimeInstance().format(Date(config.lastRunTime))
        }

        binding.txtHistorySummary.text = buildString {
            append("ประวัติอัปโหลดของเครื่องนี้\n")
            append("รออัปโหลด: $pending\n")
            append("อัปโหลดล่าสุด: $lastRun")
        }

        renderHistoryTable(config.getSyncHistory())
    }

    private fun renderHistoryTable(entries: List<SyncHistoryEntry>) {
        binding.historyTable.removeAllViews()
        binding.historyTable.addView(
            createTableRow(
                columns = listOf("เวลา", "สถานะ", "ไฟล์", "สำเร็จ", "ล้มเหลว"),
                isHeader = true,
            )
        )

        if (entries.isEmpty()) {
            binding.historyTable.addView(
                createTableRow(
                    columns = listOf("ยังไม่มีประวัติ", "-", "-", "-", "-"),
                    isHeader = false,
                )
            )
            return
        }

        entries.forEach { entry ->
            val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(Date(entry.timestamp))
            val status = when {
                entry.failureCount == 0 && entry.totalCount > 0 -> "สำเร็จ"
                entry.failureCount > 0 && entry.successCount > 0 -> "บางส่วน"
                entry.failureCount > 0 -> "ล้มเหลว"
                else -> "-"
            }

            binding.historyTable.addView(
                createTableRow(
                    columns = listOf(
                        time,
                        status,
                        entry.totalCount.toString(),
                        entry.successCount.toString(),
                        entry.failureCount.toString(),
                    ),
                    isHeader = false,
                )
            )
        }
    }

    private fun createTableRow(columns: List<String>, isHeader: Boolean): TableRow {
        return TableRow(this).apply {
            columns.forEach { value ->
                addView(createCell(value, isHeader))
            }
        }
    }

    private fun createCell(value: String, isHeader: Boolean): TextView {
        return TextView(this).apply {
            text = value
            setTextColor(getColor(android.R.color.white))
            textSize = if (isHeader) 13f else 12f
            if (isHeader) {
                setTypeface(typeface, Typeface.BOLD)
            }
            setPadding(12, 8, 12, 8)
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)
        }
    }
}
