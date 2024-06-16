package com.hawkerapp.app.views

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hawkerapp.app.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.hawkerapp.app.adapters.VisitRequestAdapter
import com.hawkerapp.app.managers.HawkerManager
import com.hawkerapp.app.models.UserRequestData
import com.hawkerapp.app.network.RetrofitHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HawkerViewActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private lateinit var btnFetchRequests:  Button
    private lateinit var hawkerManager: HawkerManager
    private var activeHawkerId: String? = null
    private lateinit var floatingWindow: PopupWindow
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VisitRequestAdapter

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hawker_view)
        hawkerManager = HawkerManager(this)
        CoroutineScope(Dispatchers.IO).launch {
            activeHawkerId = hawkerManager.getActiveHawkerId()
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.maps) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        btnFetchRequests = findViewById(R.id.btnFetchRequests)

    }

    override fun onResume() {
        super.onResume()

        btnFetchRequests.setOnClickListener {
            loadCustomers()
        }
    }

    private fun loadCustomers() {

//        val floatingWindowLayout = layoutInflater.inflate(R.layout.visit_requests_floating_window, null)
//
//        floatingWindow = PopupWindow(
//            floatingWindowLayout,
//            ViewGroup.LayoutParams.WRAP_CONTENT,
//            ViewGroup.LayoutParams.WRAP_CONTENT,
//            true
//        )
//
//        //floatingWindow.showAtLocation(window.decorView.rootView, Gravity.CENTER, 0, 0)
//        if (floatingWindow.isShowing) {
//            Log.d("HawkerViewActivity", "Popup window is showing")
//        } else {
//            Log.e("HawkerViewActivity", "Popup window failed to show")
//        }


        // Fetch customers from the server
        // Display customers on the map
        if(activeHawkerId == null){
            Log.d("hawkerViewActivity","No active hawker")
            return
        }
        RetrofitHelper.fetchUserRequests(activeHawkerId!!) {
            Log.d("HawkerViewActivity", "Users fetched")
            val customers = it

            for (user in it) {
                val userLocation = LatLng(user.location.latitude, user.location.longitude)
                mMap.addMarker(MarkerOptions().position(userLocation).title(user.customerName))
            }



            Log.d("HawkerViewActivity", "Inflating the requests popup with customers: ${customers.size}")
            val floatingWindowLayout = layoutInflater.inflate(R.layout.visit_requests_floating_window, null)
            val recyclerView = floatingWindowLayout.findViewById<RecyclerView>(R.id.visitReqsRecyclerView)

            Log.d("HawkerViewActivity", "The linearlayoutmanager is : ${recyclerView.layoutManager}")
            if( recyclerView.layoutManager == null)
                recyclerView.layoutManager = LinearLayoutManager(this)


            val adapter = VisitRequestAdapter(customers) { user ->
                val marker = mMap.addMarker(
                    MarkerOptions().position(
                        LatLng(
                            user.location.latitude,
                            user.location.longitude
                        )
                    ).title(user.customerName)
                )
                if (marker != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15f))
                }
            }
            recyclerView.adapter = adapter
            Log.d("HawkerViewActivity", "Inflating completed, recyclerView: ${recyclerView.adapter}")

            floatingWindow = PopupWindow(
                floatingWindowLayout,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )

            Log.d("HawkerViewActivity", "Showing window")
            floatingWindow.showAtLocation(window.decorView.rootView, Gravity.CENTER, 0, 0)

        }


    }

    override fun onMapReady(googleMap: GoogleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show()
        Log.d("hawkerMap", "Map is ready")
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(
                this,
            Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        Log.d("hawkerApp", "Location permission granted")
        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                //mMap.addMarker(MarkerOptions().position(currentLatLng).title("Your Location"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMapReady(mMap)
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


}