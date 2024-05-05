package com.hawkerapp.app.models

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

data class UserRequestData(
    @SerializedName("customerName")
    val customerName: String,
    @SerializedName("customerPhone")
    val customerPhone: String,
    @SerializedName("location")
    val location: CustomLocation,
    @SerializedName("notes")
    val notes: String,
    @SerializedName("status")
    val status: String = "PENDING") {
}