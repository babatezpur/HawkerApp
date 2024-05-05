package com.hawkerapp.app.models

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "hawker_info_user")
data class HawkerInfo(
//    @PrimaryKey(autoGenerate = true)
//    @ColumnInfo(name = "serialId")
//    var serialId: Int = 0,
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerializedName("id") val id: String,
    @ColumnInfo(name = "name")
    @SerializedName("name") val name: String,
    @ColumnInfo(name = "cat")
    @SerializedName("cat") val category: String,
    @ColumnInfo(name = "location")
    @SerializedName("location") val location: CustomLocation,
    @ColumnInfo(name = "phone")
    @SerializedName("phone") val phone: String,
    @ColumnInfo(name = "rating")
    @SerializedName("rating") val rating: Double,
    @ColumnInfo(name = "distance")
    @SerializedName("distance") val distance: Double,
    @ColumnInfo(name = "items")
    @SerializedName("items") val items: List<Item>
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readParcelable(CustomLocation::class.java.classLoader) ?: CustomLocation(0.0, 0.0),
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble(),
        mutableListOf<Item>().apply {
            parcel.readTypedList(this, Item)
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(category)
        parcel.writeParcelable(location, flags)
        parcel.writeString(phone)
        parcel.writeDouble(rating)
        parcel.writeDouble(distance)
        parcel.writeTypedList(items)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<HawkerInfo> {
        override fun createFromParcel(parcel: Parcel): HawkerInfo {
            return HawkerInfo(parcel)
        }

        override fun newArray(size: Int): Array<HawkerInfo?> {
            return arrayOfNulls(size)
        }
    }
}

@Serializable
@Entity
data class Item(
    @SerializedName("name") var name: String,
    @SerializedName("price") var price: Int,
    @SerializedName("quantity") var quantity: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeInt(price)
        parcel.writeInt(quantity)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Item> {
        override fun createFromParcel(parcel: Parcel): Item {
            return Item(parcel)
        }

        override fun newArray(size: Int): Array<Item?> {
            return arrayOfNulls(size)
        }
    }
}



enum class Category {
    FOOD,
    FRUITS,
    VEGETABLES,
    GROCERY,
    KITCHENWARE,
    CLOTHING,
    OTHER
}