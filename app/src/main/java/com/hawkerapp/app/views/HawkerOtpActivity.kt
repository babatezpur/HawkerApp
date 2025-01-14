package com.hawkerapp.app.views

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hawkerapp.app.R
import com.hawkerapp.app.SMSReceiver
import com.hawkerapp.app.managers.HawkerManager
import com.hawkerapp.app.models.HawkerFormData
import com.hawkerapp.app.models.OtpVerifyRequest
import com.hawkerapp.app.network.RetrofitHelper
import com.hawkerapp.app.store.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HawkerOtpActivity : AppCompatActivity() {
    private lateinit var phoneEditText: EditText
    private lateinit var otpEditText: EditText
    private lateinit var requestOtpButton: Button
    private lateinit var verifyOtpButton: Button
    private val PERMISSION_REQUEST_CODE = 123
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var hawkerManager: HawkerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hawker_otp)

        checkSMSPermissions()
        setupSMSReceiver()
        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        loadingSpinner = findViewById(R.id.loading_spinner)
        loadingSpinner.visibility = View.GONE

        phoneEditText = findViewById(R.id.phone_edit_text)
        otpEditText = findViewById(R.id.otp_edit_text)
        requestOtpButton = findViewById(R.id.request_otp_button)
        verifyOtpButton = findViewById(R.id.verify_otp_button)

        // Initially hide OTP related views
        otpEditText.visibility = View.GONE
        verifyOtpButton.visibility = View.GONE
    }

    private fun checkSMSPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
                ),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun setupSMSReceiver() {
        SMSReceiver.otpListener = { otp ->
            runOnUiThread {
                otpEditText.setText(otp)
                // Optionally auto-verify the OTP
                // verifyOtpButton.performClick()
            }
        }
    }

    private fun setupClickListeners() {
        requestOtpButton.setOnClickListener {
            val phoneNumber = phoneEditText.text.toString()
            if (phoneNumber.isEmpty() || phoneNumber.length != 10) {
                Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            RetrofitHelper.requestOtp(phoneNumber) { success ->
                runOnUiThread {
                    if (success) {
                        otpEditText.visibility = View.VISIBLE
                        verifyOtpButton.visibility = View.VISIBLE
                        requestOtpButton.isEnabled = false
                        phoneEditText.isEnabled = false
                    } else {
                        Toast.makeText(this, "Failed to send OTP. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        verifyOtpButton.setOnClickListener {
            val phoneNumber = phoneEditText.text.toString()
            val otp = otpEditText.text.toString()

            if (otp.isEmpty() || otp.length != 6) {
                Toast.makeText(this, "Please enter a valid OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadingSpinner.visibility = View.VISIBLE
            verifyOtpButton.isEnabled = false

            val startTime = System.currentTimeMillis()
            val otpVerifyRequest = OtpVerifyRequest(phoneNumber, otp)

            RetrofitHelper.verifyOtp(otpVerifyRequest) { response ->
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingDelay = 2000 - elapsedTime

                Handler(Looper.getMainLooper()).postDelayed({
                    runOnUiThread {
                        loadingSpinner.visibility = View.GONE
                        verifyOtpButton.isEnabled = true

                        if (response.isSuccessful && response.body() != null) {

                            // Save the jwt token and hawker id in shared preferences
                            val data = response.body()
                            data?.hawkerData?.id?.let { it1 ->
                                SessionManager.saveSession(
                                    context = this,
                                    hawkerId = it1,
                                    token = data.token ?: return@let
                                )
                            }

                            // Existing user case - go to main screen
                            if(response.body()?.token != null && response.body()?.hawkerData?.name != null) {

//                              updateHawkerIfNeeded(response.body()!!.hawkerData)
                                response.body()!!.hawkerData?.let { it1 -> processHawkerData(it1) }
                                val intent = Intent(this, HawkerViewActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                // New user case - go to registration
                                val intent = Intent(this, HawkerFormActivity::class.java).apply {
                                    putExtra("VERIFIED_PHONE", phoneNumber)
                                    putExtra("HAWKER_ID", response.body()?.hawkerData?.id)
                                }
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            Toast.makeText(this, "Verification failed : ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, maxOf(remainingDelay, 0))
            }
        }
    }

    private fun processHawkerData(hawkerData : HawkerFormData) {
        // Save the hawker data in hawker's table calling storeHawkerData function from hawkermanager in a coroutinescope
        hawkerData.apply { isActive = true }
        hawkerManager = HawkerManager(this)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                hawkerManager.insertHawkerLoginData(hawkerData)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HawkerOtpActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            ) {
                Toast.makeText(
                    this,
                    "SMS permissions granted. OTP will be auto-read.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "SMS permissions denied. Please enter OTP manually.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}