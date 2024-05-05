package com.hawkerapp.app.managers

import android.content.Context
import android.util.Log
import com.hawkerapp.app.models.HawkerFormData
import com.hawkerapp.app.models.HawkerInfo
import com.hawkerapp.repositories.HawkerInfoRepository
import com.hawkerapp.repositories.HawkerLoginDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HawkerManager (private val context: Context) {

    private val hawkerInfoRepository = HawkerInfoRepository(context)
    private val hawkerLoginDataRepository = HawkerLoginDataRepository(context)

    suspend fun insertHawkerInfos(hawkerInfos: List<HawkerInfo>) {
        hawkerInfoRepository.insertHawkerInfos(hawkerInfos)
    }

    suspend fun insertHawkerLoginData(hawkerLoginData: HawkerFormData) {
        hawkerLoginDataRepository.insertHawkerLoginData(hawkerLoginData)
    }

    suspend fun getActiveHawkerId(): String {
        return hawkerLoginDataRepository.getActiveHawkerId()
    }

    fun storeHawkerData(hawkerInfo: HawkerInfo) {
        Log.d("fm", "storeHawkerData: $hawkerInfo")
        val hawkerData = HawkerFormData(
            0,
            hawkerInfo.id,
            hawkerInfo.name,
            hawkerInfo.category,
            hawkerInfo.phone,
            hawkerInfo.location,
            hawkerInfo.items,

            )
        CoroutineScope(Dispatchers.IO).launch {
            insertHawkerLoginData(hawkerData)
        }
    }

    fun markAllHawkersInactive(exceptDriverId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            hawkerLoginDataRepository.markAllHawkersInactive(exceptDriverId)
        }
    }
}