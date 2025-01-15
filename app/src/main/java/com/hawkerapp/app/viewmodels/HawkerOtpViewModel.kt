package com.hawkerapp.app.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hawkerapp.app.managers.HawkerManager
import com.hawkerapp.app.models.HawkerFormData
import com.hawkerapp.app.models.OtpVerificationResponse
import com.hawkerapp.app.models.OtpVerifyRequest
import com.hawkerapp.app.network.RetrofitHelper
import com.hawkerapp.app.store.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response

class HawkerOtpViewModel(application: Application) : AndroidViewModel(application) {
    private val hawkerManager = HawkerManager(application)

    // UI state LiveData
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _otpInputEnabled = MutableLiveData<Boolean>()
    val otpInputEnabled: LiveData<Boolean> = _otpInputEnabled

    private val _phoneInputEnabled = MutableLiveData<Boolean>()
    val phoneInputEnabled: LiveData<Boolean> = _phoneInputEnabled

    private val _verifyButtonEnabled = MutableLiveData<Boolean>()
    val verifyButtonEnabled: LiveData<Boolean> = _verifyButtonEnabled

    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun requestOtp(phoneNumber: String) {
        if (phoneNumber.isEmpty() || phoneNumber.length != 10) {
            _errorMessage.value = "Please enter a valid phone number"
            return
        }

        viewModelScope.launch {
            try {
                RetrofitHelper.requestOtp(phoneNumber) { success ->
                    if (success) {
                        _otpInputEnabled.postValue(true)
                        _verifyButtonEnabled.postValue(true)
                        _phoneInputEnabled.postValue(false)
                    } else {
                        _errorMessage.postValue("Failed to send OTP. Please try again.")
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error: ${e.message}")
            }
        }
    }

    fun verifyOtp(phoneNumber: String, otp: String) {
        if (otp.isEmpty() || otp.length != 6) {
            _errorMessage.value = "Please enter a valid OTP"
            return
        }

        viewModelScope.launch {
            try {
                _loading.value = true
                _verifyButtonEnabled.value = false

                val otpVerifyRequest = OtpVerifyRequest(phoneNumber, otp)
                RetrofitHelper.verifyOtp(otpVerifyRequest) { response ->
                    handleVerifyOtpResponse(response, phoneNumber)
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error: ${e.message}")
                _loading.postValue(false)
                _verifyButtonEnabled.postValue(true)
            }
        }
    }

    private fun handleVerifyOtpResponse(response: Response<OtpVerificationResponse>, phoneNumber: String) {
        viewModelScope.launch {
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!

                // Save session data if available
                data.hawkerData?.id?.let { hawkerId ->
                    data.token?.let { token ->
                        SessionManager.saveSession(
                            context = getApplication(),
                            hawkerId = hawkerId,
                            token = token
                        )
                    }
                }

                if (data.token != null && data.hawkerData?.name != null) {
                    // Existing user case
                    data.hawkerData?.let { processHawkerData(it) }
                    _navigationEvent.postValue(NavigationEvent.NavigateToMain)
                } else {
                    // New user case
                    _navigationEvent.postValue(
                        NavigationEvent.NavigateToRegistration(
                            phoneNumber = phoneNumber,
                            hawkerId = data.hawkerData?.id
                        )
                    )
                }
            } else {
                _errorMessage.postValue("Verification failed: ${response.message()}")
            }
            _loading.postValue(false)
            _verifyButtonEnabled.postValue(true)
        }
    }

    private suspend fun processHawkerData(hawkerData: HawkerFormData) {
        withContext(Dispatchers.IO) {
            try {
                hawkerData.apply { isActive = true }
                hawkerManager.insertHawkerLoginData(hawkerData)
            } catch (e: Exception) {
                _errorMessage.postValue("Error processing hawker data: ${e.message}")
            }
        }
    }
}

sealed class NavigationEvent {
    object NavigateToMain : NavigationEvent()
    data class NavigateToRegistration(
        val phoneNumber: String,
        val hawkerId: String?
    ) : NavigationEvent()
}