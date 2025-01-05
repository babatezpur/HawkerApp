package com.hawkerapp.app.views

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.hawkerapp.app.R
import com.hawkerapp.app.SMSReceiver
import com.hawkerapp.app.network.RetrofitHelper

class HawkerPhoneDetails : Fragment() {
    private lateinit var phoneEditText: EditText
    private lateinit var otpEditText: EditText
    private lateinit var requestOtpButton: Button
    private lateinit var verifyOtpButton: Button
    private val PERMISSION_REQUEST_CODE = 123
    private var isOtpVerified = false
    private var verifiedPhoneNumber = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_hawker_phone_details, container, false)

        checkSMSPermissions()
        setupSMSReceiver()

        // Initialize views
        phoneEditText = view.findViewById(R.id.phone_edit_text)
        otpEditText = view.findViewById(R.id.otp_edit_text)
        requestOtpButton = view.findViewById(R.id.request_otp_button)
        verifyOtpButton = view.findViewById(R.id.verify_otp_button)

        // Initially hide OTP related views
        otpEditText.visibility = View.GONE
        verifyOtpButton.visibility = View.GONE

        setupClickListeners()

        return view
    }

    private fun checkSMSPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
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
            // Run on UI thread since this callback might come from a background thread
            activity?.runOnUiThread {
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
                Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Make API call to request OTP
            RetrofitHelper.requestOtp(phoneNumber) { success ->
                if (success) {
                    // Show OTP input field and verify button
                    otpEditText.visibility = View.VISIBLE
                    verifyOtpButton.visibility = View.VISIBLE
                    requestOtpButton.isEnabled = false
                    phoneEditText.isEnabled = false
                } else {
                    Toast.makeText(context, "Failed to send OTP. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        verifyOtpButton.setOnClickListener {
            val phoneNumber = phoneEditText.text.toString()
            val otp = otpEditText.text.toString()

            if (otp.isEmpty() || otp.length != 6) {
                Toast.makeText(context, "Please enter a valid OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Make API call to verify OTP
            RetrofitHelper.verifyOtp(phoneNumber, otp) { response ->
                when (response) {
                    is OtpVerificationResponse.AlreadyRegistered -> {
                        Toast.makeText(context, "This number is already registered", Toast.LENGTH_LONG).show()
                    }
                    is OtpVerificationResponse.Success -> {
                        isOtpVerified = true
                        verifiedPhoneNumber = phoneEditText.text.toString()
                        // Notify activity to proceed to next fragment
                        (activity as? HawkerFormActivity)?.proceedToSelfDetails(verifiedPhoneNumber)
                    }
                    is OtpVerificationResponse.Error -> {
                        Toast.makeText(context, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun isVerified(): Boolean = isOtpVerified

    fun getVerifiedPhoneNumber(): String = verifiedPhoneNumber

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            ) {
                Toast.makeText(
                    context,
                    "SMS permissions granted. OTP will be auto-read.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    context,
                    "SMS permissions denied. Please enter OTP manually.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

sealed class OtpVerificationResponse {
    data object Success : OtpVerificationResponse()
    data object AlreadyRegistered : OtpVerificationResponse()
    data class Error(val message: String) : OtpVerificationResponse()
}