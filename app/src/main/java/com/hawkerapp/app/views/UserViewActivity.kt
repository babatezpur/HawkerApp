package com.hawkerapp.app.views

import com.hawkerapp.app.models.HawkerInfo
import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hawkerapp.app.repositories.HawkerRelatedApis
import com.hawkerapp.app.utils.DataProcessingUtils.Companion.findHawkerById
import com.hawkerapp.app.utils.DataProcessingUtils.Companion.resize

//complete the createcoords9list function
class UserViewActivity : AppCompatActivity(), OnMapReadyCallback{

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private lateinit var searchInput: EditText
    private var callButton: Button? = null
    private var existingMarkers = mutableListOf<Marker>()

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_view)


        val mapFragment = supportFragmentManager.findFragmentById(R.id.maps) as SupportMapFragment
        searchInput = findViewById(R.id.inputSearch)

        mapFragment.getMapAsync(this)

        searchInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER) {

                val searchText = searchInput.text.toString()

                if (searchText.isNotEmpty()) {
                    // Call your API with the search text
                    val hawkers = HawkerRelatedApis.getHawkersWithItem(
                        this,
                        applicationContext,
                        searchText
                    ) { hawkersList ->
                        val builder = LatLngBounds.Builder()
                        processCoordinates(builder, hawkersList)

                        // Show a toast message with the number of hawkers found
                        Toast.makeText(
                            applicationContext,
                            "Hawkers Found: ${hawkersList.size}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // If search text is empty, show a warning toast
                    Toast.makeText(
                        applicationContext,
                        "Please enter an item to search",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                true // Return true to indicate that the action has been handled
            } else {
                false
            }
        }

// Handle 'Clear' functionality when the cross icon is clicked
        searchInput.setOnTouchListener { v, event ->
            val drawableEnd = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (searchInput.right - searchInput.compoundDrawables[drawableEnd].bounds.width())) {
                    searchInput.text.clear()
                    Toast.makeText(this, "Fetching all hawkers", Toast.LENGTH_SHORT).show()
                    val builder = LatLngBounds.Builder()
                    processCoordinates(builder, null)
                    v.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    }

    /*
    UNDERSTAND THE FOLLWING CODE BEFORE DELETING.
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
            val hawkers  = HawkerRelatedApis.getHawkersWithItem(this, applicationContext, searchText) {
                val builder = LatLngBounds.Builder()
                processCoordinates(builder, it)
                Toast.makeText(applicationContext,"Hawkers Found: ${it.size}", Toast.LENGTH_SHORT).show()
            }
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

     */


    override fun onMapReady(googleMap: GoogleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show()
        Log.d("hawkerMap", "Map is ready")
        mMap = googleMap
        val builder = LatLngBounds.Builder()
        processCoordinates(builder, null)
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

        mMap.setOnMarkerClickListener {
            //it.showInfoWindow()
            //false
            val hawkerInfo = findHawkerById(intent, it.snippet!!)

            val bottomSheetDialog = BottomSheetDialog(this)
            val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_info, null)
            bottomSheetDialog.setContentView(bottomSheetView)

            // Get references to views in the bottom sheet layout
            val titleTextView = bottomSheetView.findViewById<TextView>(R.id.titleTextView)
            val itemsListView = bottomSheetView.findViewById<ListView>(R.id.itemsListView)
            val hawkerImageView = bottomSheetView.findViewById<ImageView>(R.id.hawkerImageView)


            // Set the title and snippet text
            titleTextView.text = it.title
            titleTextView.setTextColor(Color.BLUE) // Set the text color to white

            val imageUrl = hawkerInfo?.imageUrl
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image) // Optional placeholder image
                    .error(R.drawable.error_image) // Optional error image
                    .into(hawkerImageView)
            }

            val itemNamesAndPrices = hawkerInfo?.items?.map { "${it.name}: ${it.price}" }

            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, itemNamesAndPrices!!.toMutableList())

            // Set the adapter for the ListView
            itemsListView.adapter = adapter


            bottomSheetDialog.window?.setDimAmount(0.5f)
            bottomSheetDialog.setCancelable(true)

            // Show the BottomSheetDialog
            bottomSheetDialog.show()

            callButton = bottomSheetView.findViewById(R.id.callHawkerButton)
            callButton?.setOnClickListener {
                showCallRequestDialog(hawkerInfo)
                //HawkerRelatedApis.senUserRequestToHawker(this, hawkerInfo)
            }

            // Return false to indicate that we have not consumed the event and that we wish for the default behavior to occur
            false
        }
    }

    private fun showCallRequestDialog(hawkerInfo: HawkerInfo) {
        // Inflate the dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_call_hawker, null)

        // Create the AlertDialog
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("CALL HAWKER")
            .setView(dialogView)
            .setCancelable(true)
            .setPositiveButton("SEND REQUEST", null) // Set the positive button to "Call"
            .create()

        // Show the dialog
        alertDialog.show()

        // Get references to the EditTexts
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        val noteEditText = dialogView.findViewById<EditText>(R.id.noteEditText)

        // Set button colors
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.GREEN)

        // Handle the "Call" button click event
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Call") { dialog, _ ->
            handleCallRequest(hawkerInfo, nameEditText.text.toString().trim(), noteEditText.text.toString().trim(), dialog)
        }
    }

    private fun handleCallRequest(hawkerInfo: HawkerInfo, name: String, note: String, dialog: DialogInterface) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is mandatory", Toast.LENGTH_SHORT).show()
        } else {
            // Call the API or handle the call request
            HawkerRelatedApis.senUserRequestToHawker(this, hawkerInfo, name, note)
            dialog.dismiss() // Close the dialog
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
    private fun processCoordinates(builder: LatLngBounds.Builder, hawkersPassed: List<HawkerInfo>?) {
        // Clear existing markers
        for (marker in existingMarkers) {
            marker.remove()
        }
        existingMarkers.clear()


        val hawkers = hawkersPassed ?: intent.extras?.getParcelableArray("hawkers")?.map { it as HawkerInfo }
        Log.d("hawkerMap", "Hawkers: ${hawkers.toString()}")

        val bitmapDraw = ResourcesCompat.getDrawable(resources, R.drawable.driver_icon, null) as BitmapDrawable
        val resizedBitmap = bitmapDraw.bitmap.resize(75, 75)
        val smallMarker = BitmapDescriptorFactory.fromBitmap(resizedBitmap)


        hawkers?.forEach { hawkerInfo ->
            val hawkerLatLng = LatLng(hawkerInfo.location.latitude, hawkerInfo.location.longitude)
            val marker =  mMap.addMarker(
                MarkerOptions().position(hawkerLatLng).title(hawkerInfo.name)
                    .snippet(hawkerInfo.id)
            )?.apply {
                setIcon(smallMarker)
            }

            marker?.let { existingMarkers.add(it) }

            builder.include(hawkerLatLng)
        }

        if (!hawkers.isNullOrEmpty()) {
            val bounds = builder.build()
            val padding = 100
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            Log.d("hawkerMap", "Moving camera, bounds: $bounds, padding: $padding")
            mMap.moveCamera(cameraUpdate)
        }
    }
}