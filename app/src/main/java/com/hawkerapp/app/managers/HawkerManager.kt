package com.hawkerapp.app.managers

import android.content.Context
import android.util.Log
import com.hawkerapp.app.models.HawkerFormData
import com.hawkerapp.app.models.HawkerInfo
import com.hawkerapp.app.models.Item
import com.hawkerapp.app.repositories.HawkerInfoRepository
import com.hawkerapp.app.repositories.HawkerLoginDataRepository
import com.hawkerapp.app.store.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HawkerManager (private val context: Context) {

    private val hawkerInfoRepository = HawkerInfoRepository(context)
    private val hawkerLoginDataRepository = HawkerLoginDataRepository(context)

    suspend fun insertHawkerInfos(hawkerInfos: List<HawkerInfo>) {
        hawkerInfoRepository.insertHawkerInfos(hawkerInfos)
    }

    suspend fun insertHawkerLoginData(hawkerLoginData: HawkerFormData) {
        hawkerLoginDataRepository.insertHawkerLoginData(hawkerLoginData)
    }

    suspend fun getActiveHawkerId(): String? {
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

    suspend fun getHawkerInfo(hawkerId: String?): HawkerFormData {
        return hawkerLoginDataRepository.getHawkerInfo(hawkerId)
    }

    suspend fun updateItem(items: List<Item>) {
        withContext(Dispatchers.IO) {
            val hawkerId = hawkerLoginDataRepository.getActiveHawkerId()
            if (hawkerId != null) {
                hawkerLoginDataRepository.updateHawkerItem(hawkerId, items)
            }
        }
    }

    suspend fun logout(context: Context) {
        val hawkerId = hawkerLoginDataRepository.getActiveHawkerId()
        hawkerLoginDataRepository.logout(hawkerId)
        SessionManager.clearSession(context)
    }

    suspend fun deleteItem(item: Item) {

    }
}