package com.hawkerapp.app
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import java.util.regex.Pattern

class SMSReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SMSReceiver"
        // Callback to be set from the fragment
        var otpListener: ((String) -> Unit)? = null
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        messages.forEach { smsMessage ->
            val messageBody = smsMessage.messageBody
            Log.d(TAG, "SMS received: $messageBody")

            // Assuming OTP is a 6-digit number
            // Adjust the pattern based on your actual OTP format
            val pattern = Pattern.compile("\\b(\\d{6})\\b")
            val matcher = pattern.matcher(messageBody)

            if (matcher.find()) {
                val otp = matcher.group(1)
                Log.d(TAG, "OTP found: $otp")
                otpListener?.invoke(otp)
            }
        }
    }
}