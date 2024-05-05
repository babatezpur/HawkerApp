package com.hawkerapp.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.hawkerapp.app.models.CustomLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

object LocationProvider{
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    fun init(context: Context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun getLocation(context: Context, callback: (CustomLocation) -> Unit, errorCallback: (String) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                // Check if the required permissions are granted
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, handle accordingly
            errorCallback.invoke("Location permission not granted")
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    callback.invoke(CustomLocation(latitude, longitude))
                } ?: errorCallback.invoke("Location is null")
            }
            .addOnFailureListener { e ->
                errorCallback.invoke("Error getting location: ${e.message}")
            }
    }
}