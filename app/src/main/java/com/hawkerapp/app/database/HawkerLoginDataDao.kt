package com.hawkerapp.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hawkerapp.app.models.HawkerFormData
import com.hawkerapp.app.models.HawkerInfo
import com.hawkerapp.app.models.Item

@Dao
interface HawkerLoginDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHawkerLoginData(hawkerData: HawkerFormData) : Long

    @Query("SELECT id FROM hawker_data WHERE is_active = 1")
    fun getActiveHawkerId(): String?

    @Query("UPDATE hawker_data SET is_active = 0 WHERE id != :exceptDriverId")
    fun markAllHawkersInactive(exceptDriverId: String)

    @Query("SELECT * FROM hawker_data WHERE id = :hawkerId")
    suspend fun getHawkerInfo(hawkerId: String?): HawkerFormData

    @Query("UPDATE hawker_data SET items = :items WHERE id = :hawkerId")
    suspend fun updateHawkerItem(hawkerId: String, items: List<Item>)

    @Query("UPDATE hawker_data SET is_active = 0 WHERE id = :id")
    suspend fun logout(id: String?)
}