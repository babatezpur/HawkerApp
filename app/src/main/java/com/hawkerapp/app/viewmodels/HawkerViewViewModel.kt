package com.hawkerapp.app.viewmodels

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hawkerapp.app.managers.HawkerManager
import com.hawkerapp.app.models.UserRequestData
import androidx.lifecycle.viewModelScope
import com.hawkerapp.app.models.HawkerInfo
import com.hawkerapp.app.network.RetrofitHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HawkerViewViewModel(application: Application) : AndroidViewModel(application) {
    private val hawkerManager = HawkerManager(application)
    private val _customerRequests = MutableLiveData<List<UserRequestData>>()
    val customerRequests: LiveData<List<UserRequestData>> = _customerRequests

    private val _hasUnreadRequests = MutableLiveData<Boolean>()
    val hasUnreadRequests: LiveData<Boolean> = _hasUnreadRequests

    private var activeHawkerId: String? = null

    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location> = _currentLocation


    init {
        viewModelScope.launch(Dispatchers.IO) {
            activeHawkerId = hawkerManager.getActiveHawkerId()
            checkForNewRequests(context = application)
        }
    }

    private fun checkForNewRequests(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            activeHawkerId?.let { hawkerId ->
                RetrofitHelper.fetchUserRequests(context, hawkerId) { customers ->
                    _customerRequests.postValue(customers)
                    _hasUnreadRequests.postValue(customers.isNotEmpty())
                }
            }
        }
    }

    fun markRequestsAsRead() {
        _hasUnreadRequests.value = false
    }

    fun updateCurrentLocation(location: Location) {
        _currentLocation.value = location
    }

    fun logout(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            hawkerManager.logout(context)
        }
    }
}