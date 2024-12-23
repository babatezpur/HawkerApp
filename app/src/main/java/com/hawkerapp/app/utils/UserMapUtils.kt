package com.hawkerapp.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.hawkerapp.app.models.HawkerInfo
import com.hawkerapp.app.R
import com.hawkerapp.app.utils.DataProcessingUtils.Companion.resize


class UserMapUtils(private val context: Context) {
    fun processCoordinates(
        mMap: GoogleMap,
        builder: LatLngBounds.Builder,
        hawkers: List<HawkerInfo>?,
        onMarkerAdd: (Marker) -> Unit
    ) {
        hawkers?.forEach { hawkerInfo ->
            val hawkerLatLng = LatLng(hawkerInfo.location.latitude, hawkerInfo.location.longitude)
            val bitmapDraw = ResourcesCompat.getDrawable(context.resources, R.drawable.driver_icon, null) as BitmapDrawable
            val resizedBitmap = bitmapDraw.bitmap.resize(75, 75)
            val smallMarker = BitmapDescriptorFactory.fromBitmap(resizedBitmap)

            mMap.addMarker(
                MarkerOptions()
                    .position(hawkerLatLng)
                    .title(hawkerInfo.name)
                    .snippet(hawkerInfo.id)
                    .icon(smallMarker)
            )?.let { marker ->
                onMarkerAdd(marker)
            }

            builder.include(hawkerLatLng)
        }

        if (!hawkers.isNullOrEmpty()) {
            val bounds = builder.build()
            val padding = 100
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        }
    }

    fun getCurrentLocation(
        fusedLocationClient: FusedLocationProviderClient,
        mMap: GoogleMap,
        onLocationNotFound: () -> Unit
    ) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))
            } ?: run {
                onLocationNotFound()
            }
        }
    }

    fun centerMapOnHawker(
        mMap: GoogleMap,
        hawkerInfo: HawkerInfo,
        markers: List<Marker>
    ) {
        val hawkerLatLng = LatLng(hawkerInfo.location.latitude, hawkerInfo.location.longitude)

        mMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                hawkerLatLng,
                12f
            )
        )

        markers.find { marker ->
            marker.snippet == hawkerInfo.id
        }?.showInfoWindow()
    }
}