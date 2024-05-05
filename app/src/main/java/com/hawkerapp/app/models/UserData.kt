package com.hawkerapp.app.models

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import java.io.Serial

@Serializable
data class UserData(
    @SerializedName("hawkerId")
    val hawkerId: String,
    @SerializedName("customerName")
    val name: String,
    @SerializedName("customerPhone")
    val phone: String,
    @SerializedName("location")
    val location: CustomLocation,
    @SerializedName("notes")
    val notes: String,
    @SerializedName("status")
    val status: String = "PENDING"
) {
}