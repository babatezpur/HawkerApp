package com.hawkerapp.app.managers

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import com.hawkerapp.app.models.HawkerFormData
import com.hawkerapp.app.models.HawkerInfo
import com.hawkerapp.app.models.Item
import com.hawkerapp.app.network.RetrofitHelper
import com.hawkerapp.app.repositories.HawkerInfoRepository
import com.hawkerapp.app.repositories.HawkerLoginDataRepository
import com.hawkerapp.app.store.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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

    fun storeHawkerData(hawkerInfo: HawkerFormData) {
        Log.d("fm", "storeHawkerData: $hawkerInfo")
        val hawkerData = HawkerFormData(
            0,
            hawkerInfo.id,
            hawkerInfo.name,
            hawkerInfo.category,
            hawkerInfo.phone,
            hawkerInfo.location,
            hawkerInfo.items,
            true,
            hawkerInfo.imageurl,
            hawkerInfo.createdAt
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

    suspend fun updateHawkerImage(context: Context, activeHawkerId: String?, imageUri: String): HawkerFormData? {
        return updateHawkerField(context, activeHawkerId, "imageUrl", imageUri)
    }

    suspend fun updateHawkerItems(context: Context, items: List<Item>): HawkerFormData? {
        val activeHawkerId = getActiveHawkerId()
        return updateHawkerField(context, activeHawkerId, "items", items)
    }

    suspend fun getHawkerCreatedDate(hawkerId: String): String? {
        return hawkerLoginDataRepository.getHawkerCreatedDate(hawkerId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun updateHawkerField(
        context: Context,
        activeHawkerId: String?,
        field: String,
        value: Any
    ): HawkerFormData? = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            RetrofitHelper.updateHawkerFieldInServer(context, activeHawkerId, field, value) { updatedHawker ->
                if (updatedHawker == null) {
                    Log.d("HawkerManager", "Field update failed")
                    continuation.resume(null) { }
                    return@updateHawkerFieldInServer
                }
                Log.d("HawkerManager", "Field updated successfully")
                storeHawkerData(updatedHawker)
                continuation.resume(updatedHawker) { }
            }
        }
    }

    suspend fun updateHawkerNameAndCategory(
        context: Context,
        hawkerId: String?,
        name: String?,
        category: String?
    ): HawkerFormData? {
        val profileUpdate = JsonObject().apply {
            name?.let { addProperty("name", it) }
            category?.let { addProperty("cat", it) }
        }

        return updateHawkerField(context, hawkerId, "profile", profileUpdate)
    }
}