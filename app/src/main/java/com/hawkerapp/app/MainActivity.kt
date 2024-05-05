package com.hawkerapp.app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.hawkerapp.app.R
import com.hawkerapp.app.database.HawkerDatabase
import com.hawkerapp.app.views.HawkerFormActivity
import com.hawkerapp.app.views.PreHawkerScreenActivity
import com.hawkerapp.app.views.PreUserScreenSplashActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        HawkerDatabase.getInstance(this)
    }

    override fun onResume() {
        super.onResume()
        val btnHawker = findViewById<Button>(R.id.buttonHawker)
        val btnUser = findViewById<Button>(R.id.buttonUser)
        btnHawker.setOnClickListener {
            val intent = Intent(this, PreHawkerScreenActivity::class.java)
            startActivity(intent)
        }
        btnUser.setOnClickListener {
            val intent = Intent(this, PreUserScreenSplashActivity::class.java)
            startActivity(intent)
        }


    }
}