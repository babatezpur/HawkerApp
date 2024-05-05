package com.hawkerapp.app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("HawkerApp Firebase", "onMessagerecevived triggered : ${remoteMessage.data}")
        // Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated.
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("HawkerApp Firebase", "onNewToken triggerred: $token")
        // Log and optionally send the new registration token to your app server.
    }
}