package com.example.chicmobile

import android.app.Application
import com.example.chicmobile.work.WorkScheduler

class UploadApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ensure periodic worker is always uniquely scheduled on startup/reboot.
        WorkScheduler.ensurePeriodicWork(this)
    }
}
