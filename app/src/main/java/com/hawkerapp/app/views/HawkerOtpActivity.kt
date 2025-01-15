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
import androidx.lifecycle.ViewModelProvider
import com.hawkerapp.app.R
import com.hawkerapp.app.SMSReceiver
import com.hawkerapp.app.managers.HawkerManager
import com.hawkerapp.app.models.HawkerFormData
import com.hawkerapp.app.models.OtpVerifyRequest
import com.hawkerapp.app.network.RetrofitHelper
import com.hawkerapp.app.store.SessionManager
import com.hawkerapp.app.viewmodels.HawkerOtpViewModel
import com.hawkerapp.app.viewmodels.NavigationEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HawkerOtpActivity : AppCompatActivity() {
    private lateinit var viewModel: HawkerOtpViewModel
    private lateinit var phoneEditText: EditText
    private lateinit var otpEditText: EditText
    private lateinit var requestOtpButton: Button
    private lateinit var verifyOtpButton: Button
    private lateinit var loadingSpinner: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hawker_otp)

        viewModel = ViewModelProvider(this)[HawkerOtpViewModel::class.java]

        checkSMSPermissions()
        setupSMSReceiver()
        initializeViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun initializeViews() {
        loadingSpinner = findViewById(R.id.loading_spinner)
        phoneEditText = findViewById(R.id.phone_edit_text)
        otpEditText = findViewById(R.id.otp_edit_text)
        requestOtpButton = findViewById(R.id.request_otp_button)
        verifyOtpButton = findViewById(R.id.verify_otp_button)

        // Initially hide OTP related views
        loadingSpinner.visibility = View.GONE
        otpEditText.visibility = View.GONE
        verifyOtpButton.visibility = View.GONE
    }

    private fun observeViewModel() {
        viewModel.loading.observe(this) { isLoading ->
            loadingSpinner.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.otpInputEnabled.observe(this) { enabled ->
            otpEditText.visibility = if (enabled) View.VISIBLE else View.GONE
        }

        viewModel.phoneInputEnabled.observe(this) { enabled ->
            phoneEditText.isEnabled = enabled
        }

        viewModel.verifyButtonEnabled.observe(this) { enabled ->
            verifyOtpButton.apply {
                this.visibility = if (enabled) View.VISIBLE else View.GONE
                this.isEnabled = enabled
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        viewModel.navigationEvent.observe(this) { event ->
            when (event) {
                is NavigationEvent.NavigateToMain -> {
                    startActivity(Intent(this, HawkerViewActivity::class.java))
                    finish()
                }
                is NavigationEvent.NavigateToRegistration -> {
                    val intent = Intent(this, HawkerFormActivity::class.java).apply {
                        putExtra("VERIFIED_PHONE", event.phoneNumber)
                        putExtra("HAWKER_ID", event.hawkerId)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
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
            viewModel.requestOtp(phoneEditText.text.toString())
        }

        verifyOtpButton.setOnClickListener {
            viewModel.verifyOtp(
                phoneNumber = phoneEditText.text.toString(),
                otp = otpEditText.text.toString()
            )
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

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }
}