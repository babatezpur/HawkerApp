package com.hawkerapp.app.repositories

import android.content.Context
import com.hawkerapp.app.database.HawkerDatabase
import com.hawkerapp.app.database.HawkerLoginDataDao
import com.hawkerapp.app.models.HawkerFormData

class HawkerLoginDataRepository(context: Context) {
    private val hawkerLoginDataDao: HawkerLoginDataDao = HawkerDatabase.getInstance(context).hawkerLoginDataDao()

    suspend fun insertHawkerLoginData(hawkerLoginData: HawkerFormData) {
        hawkerLoginDataDao.insertHawkerLoginData(hawkerLoginData)
    }

    suspend fun getActiveHawkerId(): String {
        return hawkerLoginDataDao.getActiveHawkerId()
    }

    suspend fun markAllHawkersInactive(exceptDriverId: String) {
        hawkerLoginDataDao.markAllHawkersInactive(exceptDriverId)
    }
}