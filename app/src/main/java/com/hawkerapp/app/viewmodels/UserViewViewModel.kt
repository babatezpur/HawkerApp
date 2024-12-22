package com.hawkerapp.app.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.hawkerapp.app.models.HawkerInfo
import com.hawkerapp.app.repositories.HawkerRelatedApis
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val _hawkers = MutableLiveData<List<HawkerInfo>>()
    val hawkers: LiveData<List<HawkerInfo>> = _hawkers

    private val _selectedHawker = MutableLiveData<HawkerInfo>()
    val selectedHawker: LiveData<HawkerInfo> = _selectedHawker

    private val _existingMarkers = MutableLiveData<MutableList<Marker>>()
    val existingMarkers: LiveData<MutableList<Marker>> = _existingMarkers

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Flag to indicate if it's a search operation or clear operation
    private val _isSearchOperation = MutableLiveData<Boolean>()
    val isSearchOperation: LiveData<Boolean> = _isSearchOperation

    init {
        _existingMarkers.value = mutableListOf()
    }

    fun searchHawkers(context: Context, searchText: String) {
        _isLoading.value = true
        _isSearchOperation.value = searchText.isNotEmpty()

        viewModelScope.launch {
            HawkerRelatedApis.getHawkersWithItem(
                context,
                getApplication(),
                searchText
            ) { hawkersList ->
                _hawkers.postValue(hawkersList)
                _isLoading.postValue(false)
            }
        }
    }

    fun fetchAllHawkers(context: Context) {
        _isLoading.value = true
        // Explicitly set search operation to false for initial load
        _isSearchOperation.value = false

        viewModelScope.launch {
            HawkerRelatedApis.getHawkersWithItem(
                context,
                getApplication(),
                ""  // Empty search string to get all hawkers
            ) { hawkersList ->
                _hawkers.postValue(hawkersList)
                _isLoading.postValue(false)
            }
        }
    }

    fun clearMarkers() {
        _existingMarkers.value?.forEach { marker ->
            marker.remove()
        }
        _existingMarkers.value?.clear()
    }

    fun addMarker(marker: Marker) {
        val currentMarkers = _existingMarkers.value ?: mutableListOf()
        currentMarkers.add(marker)
        _existingMarkers.postValue(currentMarkers)
    }

    fun setSelectedHawker(hawkerInfo: HawkerInfo) {
        _selectedHawker.value = hawkerInfo
    }

    fun sendCallRequest(context: Context, hawkerInfo: HawkerInfo, name: String, note: String) {
        viewModelScope.launch {
            HawkerRelatedApis.senUserRequestToHawker(context, hawkerInfo, name, note)
        }
    }

    fun findHawkerById(id: String): HawkerInfo? {
        return _hawkers.value?.find { it.id == id }
    }
}