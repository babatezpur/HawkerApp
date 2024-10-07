package com.hawkerapp.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import com.hawkerapp.app.R

fun showNotification(title: String, message: String, context: Context) {
    // Create a notification channel for Android 8.0 and higher
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "hawker_notification_channel"
        val channelName = "Hawker Notification Channel"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val notificationChannel = NotificationChannel(channelId, channelName, importance)
        notificationChannel.description = "Hawker App Notification Channel"

        // Register the channel with the system
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }

    // Create a notification
    val notificationBuilder = NotificationCompat.Builder(context, "hawker_notification_channel")
        .setSmallIcon(R.drawable.driver_icon) // Replace with your app's icon
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    // Show the notification
    val notificationManager = NotificationManagerCompat.from(context)
    notificationManager.notify(1, notificationBuilder.build())
}