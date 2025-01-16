package com.hawkerapp.app.views

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.hawkerapp.app.MainActivity
import com.hawkerapp.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Hide the action bar if it exists
        supportActionBar?.hide()

        // Use a coroutine to handle the delay
        lifecycleScope.launch {
            delay(3000) // 2 seconds delay

            // Start MainActivity and finish this activity
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))

            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

            finish()
        }
    }
}