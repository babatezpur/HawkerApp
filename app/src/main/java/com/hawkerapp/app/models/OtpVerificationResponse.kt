package com.hawkerapp.app.models

import com.google.gson.annotations.SerializedName

data class OtpVerificationResponse(
    @SerializedName("userData")
    val hawkerData : HawkerFormData?,

    @SerializedName("token")
    val token: String?
)
sealed class OtpResult {
    data class Success(val token: String, val isNewUser: Boolean) : OtpResult()
    data class Error(val message: String) : OtpResult()
}

data class OtpVerifyRequest(
    val phoneNumber: String,
    val otp: String
)