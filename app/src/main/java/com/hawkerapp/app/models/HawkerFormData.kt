package com.hawkerapp.app.models

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File

@Serializable
@Entity(tableName = "hawker_data")
data class HawkerFormData (
    @PrimaryKey @NonNull
    @ColumnInfo(name = "serialId")
    var serialId: Int = 0,

    @ColumnInfo(name = "id")
    @SerializedName("id") val id: String? = null,

    @ColumnInfo(name = "name")
    @SerializedName("name") val name: String?,

    @ColumnInfo(name = "cat")
    @SerializedName("cat") val category: String?,

    @ColumnInfo(name = "phone")
    @SerializedName("phone") val phone: String?,

    @Transient
    @SerializedName("location") val location: CustomLocation? = null,

    @Transient
    @SerializedName("items") val items: List<Item>? = null,

    @ColumnInfo(name = "is_active")
    @SerializedName("is_active") val isActive: Boolean = true,

    // Add this property to hold the image path
    @ColumnInfo(name = "imageUrl")
    @SerializedName("imageUrl") var imageurl: String? = null // Image path as a string


) {
}

@Serializable @Entity
data class CustomLocation(
    @SerializedName("x") var latitude: Double,
    @SerializedName("y") var longitude: Double
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CustomLocation> {
        override fun createFromParcel(parcel: Parcel): CustomLocation {
            return CustomLocation(parcel)
        }

        override fun newArray(size: Int): Array<CustomLocation?> {
            return arrayOfNulls(size)
        }
    }
}
