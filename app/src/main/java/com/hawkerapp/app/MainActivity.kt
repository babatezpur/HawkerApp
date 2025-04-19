package com.hawkerapp.app

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.hawkerapp.app.R
import com.hawkerapp.app.database.HawkerDatabase
import com.hawkerapp.app.managers.HawkerManager
import com.hawkerapp.app.models.FCMData
import com.hawkerapp.app.network.RetrofitHelper
import com.hawkerapp.app.views.HawkerFormActivity
import com.hawkerapp.app.views.HawkerOtpActivity
import com.hawkerapp.app.views.HawkerViewActivity
import com.hawkerapp.app.views.PreHawkerScreenActivity
import com.hawkerapp.app.views.PreUserScreenSplashActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var hawkerManager : HawkerManager? = null
    private var activeHawkerId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hawkerManager = HawkerManager(this)

        CoroutineScope(Dispatchers.IO).launch {
            activeHawkerId = hawkerManager?.getActiveHawkerId()
        }

        initializeFirebaseAndSendToken()

        HawkerDatabase.getInstance(this)
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.IO).launch {
            activeHawkerId = hawkerManager?.getActiveHawkerId()
        }
        val btnHawker = findViewById<CardView>(R.id.cardViewHawker)
        val btnUser = findViewById<CardView>(R.id.cardViewShopper)
        btnHawker.setOnClickListener {
            if (activeHawkerId != null) {
                Toast.makeText(this, "Welcome back activeId: $activeHawkerId", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HawkerViewActivity::class.java))
            } else {
                Toast.makeText(this, "No user is logged in. Please sign up/log in", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HawkerOtpActivity::class.java)
                startActivity(intent)
            }

        }
        btnUser.setOnClickListener {
            val intent = Intent(this, PreUserScreenSplashActivity::class.java)
            startActivity(intent)
        }


    }

    private fun initializeFirebaseAndSendToken() {
        FirebaseApp.initializeApp(this)

        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            Log.d("MainActivity", "Token received: $it")
            sendTokenToServer(this, it)
        }
    }

    fun sendTokenToServer(context: Context, token: String){
        CoroutineScope(Dispatchers.IO).launch {
            val currentHawkerId = HawkerManager(context).getActiveHawkerId()
            if (currentHawkerId!= null){
                val fcmData = FCMData(token, currentHawkerId)
                Log.d("MainActivity", "Sending token to Server: $token")
                RetrofitHelper.sendToken(fcmData)
            }
        }
    }
}