package com.hawkerapp.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hawkerapp.app.models.HawkerInfo
import retrofit2.http.GET

@Dao
interface HawkerInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHawkerInfo(hawkerInfo: HawkerInfo) : Long
}