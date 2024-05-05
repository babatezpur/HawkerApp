package com.hawkerapp.app.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hawkerapp.app.models.CustomLocation
import com.hawkerapp.app.models.Item

class Converters {
    @TypeConverter
    fun fromItemList(itemList: List<Item>): String {
        val gson = Gson()
        return gson.toJson(itemList)
    }

    @TypeConverter
    fun toItemList(itemListString: String): List<Item> {
        val gson = Gson()
        val itemType = object : TypeToken<List<Item>>() {}.type
        return gson.fromJson(itemListString, itemType)
    }

    @TypeConverter
    fun fromCustomLocation(location: CustomLocation): String {
        val gson = Gson()
        return gson.toJson(location)
    }

    @TypeConverter
    fun toCustomLocation(locationString: String): CustomLocation {
        val gson = Gson()
        return gson.fromJson(locationString, CustomLocation::class.java)
    }
}