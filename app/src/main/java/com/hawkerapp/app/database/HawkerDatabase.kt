package com.hawkerapp.app.database

import android.content.Context
import android.database.SQLException
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hawkerapp.app.models.HawkerFormData
import com.hawkerapp.app.models.HawkerInfo

@Database(entities = [HawkerInfo::class, HawkerFormData::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
internal abstract class HawkerDatabase : RoomDatabase() {

    abstract fun hawkerInfoDao(): HawkerInfoDao

    abstract fun hawkerLoginDataDao(): HawkerLoginDataDao

    companion object {
        private var sharedInstance: HawkerDatabase? = null
        private const val DATABASE_NAME = "hawker_info_database"

        private val MIGRATION_ADD_CREATED_AT = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add the new created_at column to hawker_data table
                    database.execSQL("ALTER TABLE hawker_data ADD COLUMN created_at TEXT DEFAULT NULL")
                } catch (e: SQLException) {
                    Log.e("HawkerDatabase", "Migration failed: ${e.message}")
                }
            }
        }

        fun getInstance(context: Context): HawkerDatabase {
            return sharedInstance ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(
                        context,
                        HawkerDatabase::class.java,
                        DATABASE_NAME,
                    ).addMigrations(MIGRATION_ADD_CREATED_AT)
                        .build()
                sharedInstance = instance
                instance
            }
        }
    }
}