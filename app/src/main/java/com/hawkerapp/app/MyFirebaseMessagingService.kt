package com.hawkerapp.app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hawkerapp.app.managers.HawkerManager
import com.hawkerapp.app.models.FCMData
import com.hawkerapp.app.network.RetrofitHelper
import com.hawkerapp.app.utils.showNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("HawkerApp Firebase", "onMessagerecevived triggered : ${remoteMessage.data}")
        // Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated.
        val custName = remoteMessage.notification?.title
        val custMsg = remoteMessage.notification?.body
        showNotification(custName!!,custMsg!!, this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("HawkerApp Firebase", "onNewToken triggered: $token")
        CoroutineScope(Dispatchers.Default).launch {
            sendFCMTokenToServer(token)
        }
        // Log and optionally send the new registration token to your app server.
    }

    private suspend fun sendFCMTokenToServer(token: String){
        val hawkerManager = HawkerManager(applicationContext)
        val currentHawkerId = hawkerManager.getActiveHawkerId() ?: return
        val fcmData = FCMData(token, currentHawkerId)
        RetrofitHelper.sendToken(fcmData)
    }
}