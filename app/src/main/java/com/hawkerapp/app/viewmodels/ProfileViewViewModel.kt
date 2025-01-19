package com.hawkerapp.app.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hawkerapp.app.managers.HawkerManager
import com.hawkerapp.app.models.HawkerFormData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProfileViewViewModel(application: Application) : AndroidViewModel(application) {
    private val hawkerManager = HawkerManager(application)

    private val _hawkerInfo = MutableLiveData<HawkerFormData>()
    val hawkerInfo: LiveData<HawkerFormData> = _hawkerInfo

    private val _updateStatus = MutableLiveData<Boolean>()
    val updateStatus: LiveData<Boolean> = _updateStatus

    private val _isDataChanged = MutableLiveData<Boolean>()
    val isDataChanged: LiveData<Boolean> = _isDataChanged

    private var originalHawker: HawkerFormData? = null

    init {
        loadHawkerInfo()
    }

    private fun loadHawkerInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hawkerId = hawkerManager.getActiveHawkerId()
                hawkerId?.let { id ->
                    val hawker = hawkerManager.getHawkerInfo(id)
                    originalHawker = hawker
                    _hawkerInfo.postValue(hawker)
                }
            } catch (e: Exception) {
                Log.e("ProfileViewViewModel", "Error loading hawker info: ${e.message}")
            }
        }
    }

    fun checkForChanges(name: String, category: String) {
        originalHawker?.let { original ->
            val isChanged = name != original.name || category != original.category
            _isDataChanged.value = isChanged
        }
    }

    fun updateProfile(name: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val hawkerId = hawkerManager.getActiveHawkerId()
                hawkerId?.let { id ->
                    val updatedHawker = hawkerManager.updateHawkerNameAndCategory(
                        getApplication(),
                        id,
                        name,
                        category
                    )
                    if (updatedHawker != null) {
                        // Convert the HawkerInfo to HawkerFormData
                        val updatedFormData = HawkerFormData(
                            serialId = originalHawker?.serialId ?: 0,
                            id = updatedHawker.id,
                            name = name,  // Use the new name
                            category = category,  // Use the new category
                            phone = originalHawker?.phone,
                            location = updatedHawker.location,
                            items = updatedHawker.items,
                            isActive = true,
                            imageurl = updatedHawker.imageurl,
                            createdAt = updatedHawker.createdAt
                        )

                        // Update the LiveData with new values
                        _hawkerInfo.postValue(updatedFormData)
                        originalHawker = updatedFormData
                        _isDataChanged.postValue(false)
                        _updateStatus.postValue(true)

                        // Optionally reload from database after a delay
                        viewModelScope.launch(Dispatchers.IO) {
                            delay(500)  // Give database time to update
                            loadHawkerInfo()
                        }
                    } else {
                        _updateStatus.postValue(false)
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewViewModel", "Error updating profile: ${e.message}")
                _updateStatus.postValue(false)
            }
        }
    }
}