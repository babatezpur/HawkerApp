package com.hawkerapp.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.hawkerapp.app.models.HawkerFormData
import com.hawkerapp.app.models.HawkerInfo

@Database(entities = [HawkerInfo::class, HawkerFormData::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
internal abstract class HawkerDatabase : RoomDatabase() {

    abstract fun hawkerInfoDao(): HawkerInfoDao

    abstract fun hawkerLoginDataDao(): HawkerLoginDataDao

    companion object {
        private var sharedInstance: HawkerDatabase? = null
        private const val DATABASE_NAME = "hawker_info_database"

        fun getInstance(context: Context): HawkerDatabase {
            return sharedInstance ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context,
                        HawkerDatabase::class.java,
                        DATABASE_NAME,
                    ).build()
                sharedInstance = instance
                instance
            }
        }
    }
}