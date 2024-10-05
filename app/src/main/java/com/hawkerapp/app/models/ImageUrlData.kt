package com.hawkerapp.app.models

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

data class ImageUrlData (
    @SerializedName("id")
    val imgId: String,
    @SerializedName("fileName")
    val fileName: String,
    @SerializedName("filePath")
    val filePath: String
)