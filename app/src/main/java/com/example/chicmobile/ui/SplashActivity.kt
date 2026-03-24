package com.example.chicmobile.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.chicmobile.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())
    private val navigateToMain = Runnable {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handler.postDelayed(navigateToMain, SPLASH_DELAY_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacks(navigateToMain)
        super.onDestroy()
    }

    companion object {
        private const val SPLASH_DELAY_MS = 3_000L
    }
}
