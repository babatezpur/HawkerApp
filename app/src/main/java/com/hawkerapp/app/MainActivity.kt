package com.hawkerapp.app

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.hawkerapp.app.R
import com.hawkerapp.app.database.HawkerDatabase
import com.hawkerapp.app.managers.HawkerManager
import com.hawkerapp.app.models.FCMData
import com.hawkerapp.app.network.RetrofitHelper
import com.hawkerapp.app.views.HawkerFormActivity
import com.hawkerapp.app.views.PreHawkerScreenActivity
import com.hawkerapp.app.views.PreUserScreenSplashActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeFirebaseAndSendToken()

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
                Log.d("MainActivity", "Sending token to Server")
                RetrofitHelper.sendToken(fcmData)
            }
        }
    }
}