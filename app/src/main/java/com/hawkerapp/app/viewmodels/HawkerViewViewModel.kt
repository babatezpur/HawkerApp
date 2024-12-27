package com.hawkerapp.app.viewmodels

import android.app.Application
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

    private var activeHawkerId: String? = null

    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location> = _currentLocation


    init {
        viewModelScope.launch(Dispatchers.IO) {
            activeHawkerId = hawkerManager.getActiveHawkerId()
        }
    }

    fun loadCustomers() {
        viewModelScope.launch(Dispatchers.IO) {  // Also use IO dispatcher here
            activeHawkerId?.let { hawkerId ->
                RetrofitHelper.fetchUserRequests(hawkerId) { customers ->
                    _customerRequests.postValue(customers)  // postValue is safe to call from background thread
                }
            }
        }
    }

    fun updateCurrentLocation(location: Location) {
        _currentLocation.value = location
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            hawkerManager.logout()
        }
    }
}