package com.hawkerapp.app.views

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.hawkerapp.app.R
import com.hawkerapp.app.managers.HawkerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreHawkerScreenActivity : AppCompatActivity() {
    private lateinit var buttonSignUp: Button
    private lateinit var buttonLogin: Button
    private var hawkerManager : HawkerManager? = null

    private var activeHawkerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pre_hawker_screen)
        hawkerManager = HawkerManager(this)

        buttonSignUp = findViewById(R.id.buttonSignUp)
        buttonLogin = findViewById(R.id.buttonSignIn)

        CoroutineScope(Dispatchers.IO).launch {
            activeHawkerId = hawkerManager?.getActiveHawkerId()
        }

        buttonSignUp.setOnClickListener {
            startActivity(Intent(this, HawkerFormActivity::class.java))
        }

        buttonLogin.setOnClickListener {
            if (activeHawkerId != null) {
                Toast.makeText(this, "Welcome back activeId: $activeHawkerId", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HawkerViewActivity::class.java))
            } else {
                Toast.makeText(this, "Please sign up first", Toast.LENGTH_SHORT).show()
            }
        }
    }
}