package com.hawkerapp.app.views

import com.hawkerapp.app.models.HawkerInfo
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import com.hawkerapp.app.R
import com.hawkerapp.app.network.RetrofitHelper
import com.hawkerapp.app.repositories.HawkerInfoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PreUserScreenSplashActivity : AppCompatActivity() {
    private lateinit var progressBar: ProgressBar
    private lateinit var hawkerInfoRepository : HawkerInfoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hawkerInfoRepository = HawkerInfoRepository(context = this)
        startFetchingData()

        setContentView(R.layout.activity_pre_user_screen_splash)
        progressBar = findViewById(R.id.progressBar_cyclic)

        progressBar.visibility = ProgressBar.VISIBLE

    }

    private fun startFetchingData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitHelper.fetchHawkersData {
                    Log.d("PreUserScreenSplashActivity", "Hawkers fetched: $it")
                    this.launch {
                        hawkerInfoRepository.insertHawkerInfos(it)
                    }
                    if (it.isNotEmpty()) {
                        moveToUserView(it)
                }

                }

            } catch (e: Exception) {
                Log.e("PreUserScreenSplashActivity", "Error fetching data", e)
            }
        }
    }

    private fun moveToUserView(hawkers: List<HawkerInfo>) {
        progressBar.visibility = ProgressBar.GONE

        val intent = Intent(this, UserViewActivity::class.java)
        intent.putExtra("hawkers", hawkers.toTypedArray())
        Log.d("PreUserScreenSplashActivity", "Moving to UserViewActivity: ${hawkers[0]}, ${hawkers[1]}")
        startActivity(intent)
        finish() // Finish current activity to prevent going back to it when pressing back button
    }
}