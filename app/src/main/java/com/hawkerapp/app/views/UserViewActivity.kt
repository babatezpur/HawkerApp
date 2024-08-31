package com.hawkerapp.app.views

import com.hawkerapp.app.models.HawkerInfo
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.hawkerapp.app.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hawkerapp.app.models.UserData
import com.hawkerapp.app.network.RetrofitHelper
import com.hawkerapp.app.utils.LocationProvider

//complete the createcoords9list function
class UserViewActivity : AppCompatActivity(), OnMapReadyCallback{

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private lateinit var btnSearchItem: Button
    private var callButton: Button? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_view)

        val hawkers = intent.extras?.getParcelableArray("hawkers")


        val mapFragment = supportFragmentManager.findFragmentById(R.id.maps) as SupportMapFragment
        btnSearchItem = findViewById(R.id.btnSearchItem)

        mapFragment.getMapAsync(this)

        btnSearchItem.setOnClickListener {
            showSearchDialog()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    }

    @SuppressLint("ServiceCast")
    private fun showSearchDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Search Item")

        // Set up the input
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.imeOptions = EditorInfo.IME_ACTION_DONE
        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton("Search") { dialog, which ->
            val searchText = input.text.toString()
            // TODO: Implement your search functionality here
            getHawkersWithItem(searchText)
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        val dialog = builder.create()

        // Show keyboard automatically when dialog appears
        dialog.setOnShowListener {
            input.requestFocus()
            input.postDelayed({
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
            }, 200)
        }

        dialog.show()
    }

    private fun getHawkersWithItem(item: String) {
        LocationProvider.init(this)
        LocationProvider.getLocation(this,
            { location ->
                RetrofitHelper.getHawkersWithItem(item, location) {
                    Toast.makeText(applicationContext, "API CALL MADE", Toast.LENGTH_SHORT).show()
                }
            },
            {
                error ->
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()

            }
        )
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show()
        Log.d("hawkerMap", "Map is ready")
        mMap = googleMap
        val builder = LatLngBounds.Builder()
        processCoordinates(builder)
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
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        }


//        mMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
//            override fun getInfoWindow(p0: com.google.android.gms.maps.model.Marker): android.view.View? {
//                return null
//            }
//
//            override fun getInfoContents(marker: Marker): android.view.View? {
//                val info = marker.title
//                val infoWindow = layoutInflater.inflate(R.layout.marker_info_window, null)
//                infoWindow.findViewById<android.widget.TextView>(R.id.titleTextView).text = info
//                return infoWindow
//            }
//        })

        mMap.setOnMarkerClickListener {
            //it.showInfoWindow()
            //false
            val hawkerInfo = findHawkerById(it.snippet!!)

            val bottomSheetDialog = BottomSheetDialog(this)
            val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_info, null)
            bottomSheetDialog.setContentView(bottomSheetView)

            // Get references to views in the bottom sheet layout
            val titleTextView = bottomSheetView.findViewById<TextView>(R.id.titleTextView)
            val itemsListView = bottomSheetView.findViewById<ListView>(R.id.itemsListView)


            // Set the title and snippet text
            titleTextView.text = it.title

            val itemNamesAndPrices = hawkerInfo?.items?.map { "${it.name}: ${it.price}" }

            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, itemNamesAndPrices!!.toMutableList())

            // Set the adapter for the ListView
            itemsListView.adapter = adapter


            bottomSheetDialog.window?.setDimAmount(0.0f)
            bottomSheetDialog.setCancelable(true)

            // Show the BottomSheetDialog
            bottomSheetDialog.show()

            callButton = bottomSheetView.findViewById<Button>(R.id.callHawkerButton)
            callButton?.setOnClickListener {
                Toast.makeText(this, "Calling ${hawkerInfo.name}", Toast.LENGTH_SHORT).show()
                LocationProvider.init(this)
                LocationProvider.getLocation(this,
                { location ->
                    val userData = UserData(
                        hawkerInfo.id,
                        "Customer 1",
                        "1234567890",
                        location,
                        "Pelase come to me urgnetly",
                    )
                    RetrofitHelper.sendUserRequest(userData) {
                        Toast.makeText(this, "Request sent to ${hawkerInfo.name}", Toast.LENGTH_SHORT).show()
                    }

                },
                    { error ->
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
                )
            }

            // Return false to indicate that we have not consumed the event and that we wish for the default behavior to occur
            false
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

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun processCoordinates(builder: LatLngBounds.Builder) {
        val hawkers = intent.extras?.getParcelableArray("hawkers")
        Log.d("hawkerMap", "Hawkers: ${hawkers.toString()}")
        val bitmapdraw = ResourcesCompat.getDrawable(resources, R.drawable.driver_icon, null) as BitmapDrawable
        var b = bitmapdraw.bitmap
        b = b.resize(75, 75) // Resize the image to width: 50, height: 50

        val smallMarker = BitmapDescriptorFactory.fromBitmap(b)
        if (hawkers != null) {
            for (hawker in hawkers) {
                val hawkerInfo = hawker as HawkerInfo
                val hawkerLatLng = LatLng(hawkerInfo.location.latitude, hawkerInfo.location.longitude)
                mMap.addMarker(MarkerOptions().position(hawkerLatLng).title(hawkerInfo.name).snippet(hawkerInfo.id))?.setIcon(smallMarker)
                builder.include(hawkerLatLng)
            }
            val bounds = builder.build()
            val padding = 100
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            Log.d("hawkerMap", "Moving camera, bounds: $bounds, padding: $padding")
            mMap.moveCamera(cameraUpdate)
        }
    }

    private fun Bitmap.resize(width: Int, height: Int): Bitmap {
        val mutableBitmap = this.copy(Bitmap.Config.ARGB_8888, true)
        return Bitmap.createScaledBitmap(mutableBitmap, width, height, false)
    }

    fun findHawkerById(id: String): HawkerInfo? {
        val hawkers = intent.extras?.getParcelableArray("hawkers")
        if (hawkers != null) {
            for (hawker in hawkers) {
                val hawkerInfo = hawker as HawkerInfo
                if (hawkerInfo.id == id) {
                    return hawkerInfo
                }
            }
        }
        return null
    }
}