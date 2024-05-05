package com.hawkerapp.app.repositories

import android.content.Context
import com.hawkerapp.app.database.HawkerDatabase
import com.hawkerapp.app.database.HawkerInfoDao
import com.hawkerapp.app.models.HawkerInfo

class HawkerInfoRepository (context: Context) {

    private val hawkerInfoDao: HawkerInfoDao = HawkerDatabase.getInstance(context).hawkerInfoDao()

    suspend fun insertHawkerInfos(hawkerInfos: List<HawkerInfo>) {
        hawkerInfos.forEach { hawkerInfo ->
            hawkerInfoDao.insertHawkerInfo(hawkerInfo)
        }
        //hawkerInfoDao.insertHawkerInfo(hawkerInfo)
    }
}