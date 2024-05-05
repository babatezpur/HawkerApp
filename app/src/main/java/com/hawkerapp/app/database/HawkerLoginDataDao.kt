package com.hawkerapp.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hawkerapp.app.models.HawkerFormData

@Dao
interface HawkerLoginDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHawkerLoginData(hawkerData: HawkerFormData) : Long

    @Query("SELECT id FROM hawker_data WHERE is_active = 1")
    fun getActiveHawkerId(): String

    @Query("UPDATE hawker_data SET is_active = 0 WHERE id != :exceptDriverId")
    fun markAllHawkersInactive(exceptDriverId: String)
}