package com.hawkerapp.app.views

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.hawkerapp.app.R
import com.hawkerapp.app.SMSReceiver
import com.hawkerapp.app.viewmodels.HawkerOtpViewModel
import com.hawkerapp.app.viewmodels.NavigationEvent
import com.google.android.gms.auth.api.credentials.Credential
import com.google.android.gms.auth.api.credentials.Credentials
import com.google.android.gms.auth.api.credentials.HintRequest
import android.content.IntentSender

class HawkerOtpActivity : AppCompatActivity() {
    private lateinit var viewModel: HawkerOtpViewModel
    private lateinit var viewFlipper: ViewFlipper
    private lateinit var phoneEditText: EditText
    private lateinit var requestOtpButton: Button
    private lateinit var verifyOtpButton: Button
    private lateinit var loadingSpinnerPhone: ProgressBar
    private lateinit var loadingSpinnerOtp: ProgressBar
    private lateinit var resendOtpText: TextView
    private lateinit var subtitleOtp: TextView
    private lateinit var otpDigits: List<EditText>
    private var resendTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hawker_otp)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        viewModel = ViewModelProvider(this)[HawkerOtpViewModel::class.java]

        checkSMSPermissions()
        setupSMSReceiver()
        initializeViews()
        setupOtpDigitNavigation()
        setupClickListeners()
        observeViewModel()
        requestHintPermission()
    }

    private fun initializeViews() {
        viewFlipper = findViewById(R.id.viewFlipper)
        phoneEditText = findViewById(R.id.phone_edit_text)
        requestOtpButton = findViewById(R.id.request_otp_button)
        verifyOtpButton = findViewById(R.id.verify_otp_button)
        loadingSpinnerPhone = findViewById(R.id.loading_spinner_phone)
        loadingSpinnerOtp = findViewById(R.id.loading_spinner)
        resendOtpText = findViewById(R.id.resend_otp_text)

        otpDigits = listOf(
            findViewById(R.id.otp_digit_1),
            findViewById(R.id.otp_digit_2),
            findViewById(R.id.otp_digit_3),
            findViewById(R.id.otp_digit_4),
            findViewById(R.id.otp_digit_5),
            findViewById(R.id.otp_digit_6)
        )

        subtitleOtp = findViewById(R.id.subtitle_otp)
        subtitleOtp.text = Html.fromHtml(
            "We have sent you an <b>One Time Password(OTP)</b> on this mobile number.",
            Html.FROM_HTML_MODE_COMPACT
        )

        loadingSpinnerPhone.visibility = View.GONE
        loadingSpinnerOtp.visibility = View.GONE
    }

    private fun setupOtpDigitNavigation() {
        for (i in otpDigits.indices) {
            otpDigits[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < otpDigits.size - 1) {
                        otpDigits[i + 1].requestFocus()
                    }
                }
            })
            otpDigits[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL &&
                    event.action == android.view.KeyEvent.ACTION_DOWN &&
                    otpDigits[i].text.isEmpty() && i > 0
                ) {
                    otpDigits[i - 1].requestFocus()
                    otpDigits[i - 1].text.clear()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun getOtpFromDigits(): String {
        return otpDigits.joinToString("") { it.text.toString() }
    }

    private fun setOtpDigits(otp: String) {
        for (i in otpDigits.indices) {
            if (i < otp.length) {
                otpDigits[i].setText(otp[i].toString())
            }
        }
    }

    private fun showOtpScreen() {
        viewFlipper.displayedChild = 1
    }

    private fun showPhoneScreen() {
        viewFlipper.displayedChild = 0
    }

    private fun observeViewModel() {
        viewModel.loading.observe(this) { isLoading ->
            loadingSpinnerOtp.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.otpInputEnabled.observe(this) { enabled ->
            if (enabled) {
                showOtpScreen()
                requestOtpButton.text = "Continue  \u2192"
                requestOtpButton.isEnabled = true
                loadingSpinnerPhone.visibility = View.GONE
                startResendCooldown()
            }
        }

        viewModel.phoneInputEnabled.observe(this) { enabled ->
            phoneEditText.isEnabled = enabled
        }

        viewModel.verifyButtonEnabled.observe(this) { enabled ->
            verifyOtpButton.isEnabled = enabled
        }

        viewModel.errorMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            requestOtpButton.text = "Continue  \u2192"
            requestOtpButton.isEnabled = true
            loadingSpinnerPhone.visibility = View.GONE
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
        viewModel.phoneNumber.observe(this) { number ->
            phoneEditText.setText(number)
        }
    }

    private fun requestHintPermission() {
        val hintRequest = HintRequest.Builder()
            .setPhoneNumberIdentifierSupported(true)
            .build()

        val credentialsClient = Credentials.getClient(this)
        val intent = credentialsClient.getHintPickerIntent(hintRequest)

        try {
            startIntentSenderForResult(
                intent.intentSender,
                CREDENTIAL_PICKER_REQUEST,
                null, 0, 0, 0
            )
        } catch (e: IntentSender.SendIntentException) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREDENTIAL_PICKER_REQUEST && resultCode == RESULT_OK) {
            val credential: Credential? = data?.getParcelableExtra(Credential.EXTRA_KEY)
            credential?.id?.let { phoneNumber ->
                viewModel.setPhoneNumber(phoneNumber)
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
                setOtpDigits(otp)
            }
        }
    }

    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.btn_back_phone).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.btn_back_otp).setOnClickListener {
            showPhoneScreen()
            phoneEditText.isEnabled = true
        }

        requestOtpButton.setOnClickListener {
            val phone = phoneEditText.text.toString()
            if (phone.length == 10) {
                requestOtpButton.text = "Sending..."
                requestOtpButton.isEnabled = false
                loadingSpinnerPhone.visibility = View.VISIBLE
            }
            viewModel.requestOtp(phone)
        }

        verifyOtpButton.setOnClickListener {
            viewModel.verifyOtp(
                phoneNumber = phoneEditText.text.toString(),
                otp = getOtpFromDigits()
            )
        }

        resendOtpText.setOnClickListener {
            if (resendOtpText.isEnabled) {
                viewModel.requestOtp(phoneEditText.text.toString())
                Toast.makeText(this, "OTP resent", Toast.LENGTH_SHORT).show()
                startResendCooldown()
            }
        }
    }

    private fun startResendCooldown() {
        resendOtpText.isEnabled = false
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                resendOtpText.text = "Resend OTP in ${seconds}s"
                resendOtpText.setTextColor(ContextCompat.getColor(this@HawkerOtpActivity, R.color.black))
            }

            override fun onFinish() {
                resendOtpText.text = Html.fromHtml(
                    "Didn't receive the OTP? <font color='#4CAF50'><b>Resend OTP</b></font>",
                    Html.FROM_HTML_MODE_COMPACT
                )
                resendOtpText.isEnabled = true
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
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
        private const val CREDENTIAL_PICKER_REQUEST = 1
    }
}
