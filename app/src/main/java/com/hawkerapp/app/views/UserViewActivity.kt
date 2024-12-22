package com.hawkerapp.app.views

import HawkerSearchBottomSheet
import com.hawkerapp.app.models.HawkerInfo
import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hawkerapp.app.repositories.HawkerRelatedApis
import com.hawkerapp.app.utils.DataProcessingUtils.Companion.findHawkerById
import com.hawkerapp.app.utils.DataProcessingUtils.Companion.resize
import com.hawkerapp.app.viewmodels.UserViewModel

//complete the createcoords9list function
class UserViewActivity : AppCompatActivity(), OnMapReadyCallback{

    private val viewModel: UserViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private lateinit var searchInput: EditText
    // private var callButton: ImageButton? = null
    private var existingMarkers = mutableListOf<Marker>()

    private lateinit var bottomSheetDialog: BottomSheetDialog

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_view)

        setupUI()
        setupObservers()
        setupLocationServices()

        viewModel.fetchAllHawkers(this)

    }


//        val mapFragment = supportFragmentManager.findFragmentById(R.id.maps) as SupportMapFragment
//        searchInput = findViewById(R.id.inputSearch)
//
//        mapFragment.getMapAsync(this)
//
//        searchInput.setOnEditorActionListener { v, actionId, event ->
//            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
//                event?.keyCode == KeyEvent.KEYCODE_ENTER) {
//
//                val searchText = searchInput.text.toString()
//
//                if (searchText.isNotEmpty()) {
//                    // First, get the data from the API
//                    HawkerRelatedApis.getHawkersWithItem(
//                        this,
//                        applicationContext,
//                        searchText
//                    ) { hawkersList ->
//                        // Only create and show the bottom sheet after we have the data
//                        runOnUiThread {
//                            val hawkerSearchBottomSheet = HawkerSearchBottomSheet(applicationContext)
//                            hawkerSearchBottomSheet.show(supportFragmentManager, "HawkerSearchBottomSheet")
//                            // Update the list after the bottom sheet is created
//                            hawkerSearchBottomSheet.updateHawkersList(hawkersList)
//
//                            val builder = LatLngBounds.Builder()
//                            processCoordinates(builder, hawkersList)
//
//                            Toast.makeText(
//                                applicationContext,
//                                "Hawkers Found: ${hawkersList.size}",
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//                    }
//                } else {
//                    Toast.makeText(
//                        applicationContext,
//                        "Please enter an item to search",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//                true
//            } else {
//                false
//            }
//        }
//
//// Handle 'Clear' functionality when the cross icon is clicked
//        searchInput.setOnTouchListener { v, event ->
//            val drawableEnd = 2
//            if (event.action == MotionEvent.ACTION_UP) {
//                if (event.rawX >= (searchInput.right - searchInput.compoundDrawables[drawableEnd].bounds.width())) {
//                    searchInput.text.clear()
//                    Toast.makeText(this, "Fetching all hawkers", Toast.LENGTH_SHORT).show()
//                    val builder = LatLngBounds.Builder()
//                    processCoordinates(builder, null)
//                    v.performClick()
//                    return@setOnTouchListener true
//                }
//            }
//            false
//        }
//
//
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//    }

    private fun setupUI() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.maps) as SupportMapFragment
        searchInput = findViewById(R.id.inputSearch)
        mapFragment.getMapAsync(this)

        setupSearchInput()
        setupSearchClear()
    }

    private fun setupSearchInput() {
        searchInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                handleSearch()
                true
            } else {
                false
            }
        }
    }

    private fun handleSearch() {
        val searchText = searchInput.text.toString()
        if (searchText.isNotEmpty()) {
            viewModel.searchHawkers(this, searchText)
        } else {
            Toast.makeText(
                applicationContext,
                "Please enter an item to search",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSearchClear() {
        searchInput.setOnTouchListener { v, event ->
            val drawableEnd = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (searchInput.right - searchInput.compoundDrawables[drawableEnd].bounds.width())) {
                    searchInput.text.clear()
                    viewModel.fetchAllHawkers(this) // This will fetch all hawkers without showing bottom sheet
                    v.performClick()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun setupObservers() {
        viewModel.hawkers.observe(this) { hawkersList ->
            // Update map markers regardless of search state
            val builder = LatLngBounds.Builder()
            processCoordinates(builder, hawkersList)

            // Only show bottom sheet if it's a search operation
            if (viewModel.isSearchOperation.value == true && hawkersList.isNotEmpty()) {
                val hawkerSearchBottomSheet = HawkerSearchBottomSheet(applicationContext)
                hawkerSearchBottomSheet.show(supportFragmentManager, "HawkerSearchBottomSheet")
                hawkerSearchBottomSheet.updateHawkersList(hawkersList)

                Toast.makeText(
                    applicationContext,
                    "Hawkers Found: ${hawkersList.size}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.selectedHawker.observe(this) { hawkerInfo ->
            hawkerInfo?.let { showHawkerDetails(it) }
        }
    }

    private fun setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupMap()
        setupMapClickListeners()
    }

    private fun setupMap() {
        if (checkLocationPermission()) {
            mMap.isMyLocationEnabled = true
            getCurrentLocation()
        }
    }


    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))
            } ?: run {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMapClickListeners() {
        mMap.setOnMarkerClickListener { marker ->
            val hawkerInfo = viewModel.findHawkerById(marker.snippet!!)
            hawkerInfo?.let { viewModel.setSelectedHawker(it) }
            false
        }
    }

     fun showHawkerDetails(hawkerInfo: HawkerInfo) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_info, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        setupBottomSheetUI(bottomSheetView, hawkerInfo, bottomSheetDialog)
    }

    private fun setupBottomSheetUI(view: View, hawkerInfo: HawkerInfo, dialog: BottomSheetDialog) {
        view.apply {
            findViewById<TextView>(R.id.titleTextView).apply {
                text = hawkerInfo.name
                setTextColor(Color.BLUE)
            }

            setupHawkerImage(findViewById(R.id.hawkerImageView), hawkerInfo.imageUrl)
            setupItemsList(findViewById(R.id.itemsListView), hawkerInfo)
            setupCallButton(findViewById(R.id.callButton), hawkerInfo)
        }

        dialog.apply {
            window?.setDimAmount(0.5f)
            setCancelable(true)
            behavior.apply {
                state = BottomSheetBehavior.STATE_HALF_EXPANDED
                peekHeight = 600
            }
            show()
        }
    }

    private fun setupHawkerImage(imageView: ImageView, imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(imageView)
        }
    }

    private fun setupItemsList(listView: ListView, hawkerInfo: HawkerInfo) {
        val itemNamesAndPrices = hawkerInfo.items.map { "${it.name}: ${it.price}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, itemNamesAndPrices)
        listView.adapter = adapter
    }

    private fun setupCallButton(button: ImageButton, hawkerInfo: HawkerInfo) {
        button.setOnClickListener {
            showCallRequestDialog(hawkerInfo)
        }
    }

    private fun showCallRequestDialog(hawkerInfo: HawkerInfo) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_call_hawker, null)
        val alertDialog = createCallDialog(dialogView, hawkerInfo)

        alertDialog.show()
        setupCallDialogButton(alertDialog, dialogView, hawkerInfo)
    }

    private fun createCallDialog(dialogView: View, hawkerInfo: HawkerInfo): AlertDialog {
        return AlertDialog.Builder(this)
            .setTitle("CALL HAWKER")
            .setView(dialogView)
            .setCancelable(true)
            .setPositiveButton("SEND REQUEST", null)
            .create()
    }

    private fun setupCallDialogButton(dialog: AlertDialog, dialogView: View, hawkerInfo: HawkerInfo) {
        val nameEditText = dialogView.findViewById<EditText>(R.id.nameEditText)
        val noteEditText = dialogView.findViewById<EditText>(R.id.noteEditText)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.GREEN)

        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Call") { dialogInterface, _ ->
            handleCallRequest(hawkerInfo, nameEditText.text.toString().trim(),
                noteEditText.text.toString().trim(), dialogInterface)
        }
    }

    private fun handleCallRequest(hawkerInfo: HawkerInfo, name: String, note: String, dialog: DialogInterface) {
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is mandatory", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.sendCallRequest(this, hawkerInfo, name, note)
            dialog.dismiss()
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun processCoordinates(builder: LatLngBounds.Builder, hawkers: List<HawkerInfo>?) {
        viewModel.clearMarkers()

        hawkers?.forEach { hawkerInfo ->
            val hawkerLatLng = LatLng(hawkerInfo.location.latitude, hawkerInfo.location.longitude)
            val bitmapDraw = ResourcesCompat.getDrawable(resources, R.drawable.driver_icon, null) as BitmapDrawable
            val resizedBitmap = bitmapDraw.bitmap.resize(75, 75)
            val smallMarker = BitmapDescriptorFactory.fromBitmap(resizedBitmap)

            mMap.addMarker(
                MarkerOptions()
                    .position(hawkerLatLng)
                    .title(hawkerInfo.name)
                    .snippet(hawkerInfo.id)
                    .icon(smallMarker)
            )?.let { marker ->
                viewModel.addMarker(marker)
            }

            builder.include(hawkerLatLng)
        }

        if (!hawkers.isNullOrEmpty()) {
            val bounds = builder.build()
            val padding = 100
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        }
    }

    private fun checkLocationPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupMap()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun centerMapOnHawker(hawkerInfo: HawkerInfo) {
        val hawkerLatLng = LatLng(hawkerInfo.location.latitude, hawkerInfo.location.longitude)

        mMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                hawkerLatLng,
                12f
            )
        )

        viewModel.existingMarkers.value?.find { marker ->
            marker.snippet == hawkerInfo.id
        }?.showInfoWindow()
    }
}

/*
    fun centerMapOnHawker(hawkerInfo: HawkerInfo) {
        // Create LatLng object for the hawker's location
        val hawkerLatLng = LatLng(hawkerInfo.location.latitude, hawkerInfo.location.longitude)


        // Animate camera to center on hawker with zoom
        mMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                hawkerLatLng,
                12f  // Zoom level - adjust this value as needed (higher = more zoomed in)
            )
        )

        // Optional: Highlight the marker
        existingMarkers.find { marker ->
                marker.snippet == hawkerInfo.id
            }?.showInfoWindow()
    }

    override fun onMapReady(googleMap: GoogleMap) {
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

        mMap.setOnMarkerClickListener { marker ->
            val hawkerInfo = findHawkerById(intent, marker.snippet!!)
            hawkerInfo?.let { showHawkerDetails(it) }
            false
        }
    }

    fun showHawkerDetails(hawkerInfo: HawkerInfo) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_info, null)
        bottomSheetDialog.setContentView(bottomSheetView)

        // Get references to views in the bottom sheet layout
        val titleTextView = bottomSheetView.findViewById<TextView>(R.id.titleTextView)
        val itemsListView = bottomSheetView.findViewById<ListView>(R.id.itemsListView)
        val hawkerImageView = bottomSheetView.findViewById<ImageView>(R.id.hawkerImageView)

        // Set the title text
        titleTextView.text = hawkerInfo.name
        titleTextView.setTextColor(Color.BLUE)

        // Load hawker image
        val imageUrl = hawkerInfo.imageUrl
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(hawkerImageView)
        }

        // Set up items list
        val itemNamesAndPrices = hawkerInfo.items.map { "${it.name}: ${it.price}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, itemNamesAndPrices.toMutableList())
        itemsListView.adapter = adapter

        // Configure dialog
        bottomSheetDialog.window?.setDimAmount(0.5f)
        bottomSheetDialog.setCancelable(true)

        // Add this code after setContentView
        bottomSheetDialog.behavior.apply {
            state = BottomSheetBehavior.STATE_HALF_EXPANDED  // Makes it open fully by default
            peekHeight = 600  // Set the initial peek height in pixels
        }

        // Set up call button
        val callButton = bottomSheetView.findViewById<ImageButton>(R.id.callButton)
        callButton.setOnClickListener {
            showCallRequestDialog(hawkerInfo)
        }

        bottomSheetDialog.show()
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
 */