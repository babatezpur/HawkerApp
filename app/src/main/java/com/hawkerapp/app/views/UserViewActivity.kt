package com.hawkerapp.app.views

import HawkerSearchBottomSheet
import com.hawkerapp.app.models.HawkerInfo
import android.Manifest
import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import com.hawkerapp.app.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.hawkerapp.app.utils.BottomSheetUtils
import com.hawkerapp.app.utils.HawkerSearchUtils
import com.hawkerapp.app.utils.PermissionUtils
import com.hawkerapp.app.utils.UserMapUtils
import com.hawkerapp.app.viewmodels.UserViewModel

class UserViewActivity : AppCompatActivity(), OnMapReadyCallback {

    private val viewModel: UserViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private lateinit var searchInput: EditText

    private val mapUtils by lazy { UserMapUtils(this) }
    private val bottomSheetUtils by lazy { BottomSheetUtils(this) }
    private val searchUtils by lazy { HawkerSearchUtils() }
    private val permissionUtils by lazy { PermissionUtils(this) }

    private val markers = mutableListOf<Marker>()

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

    private fun setupUI() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.maps) as SupportMapFragment
        searchInput = findViewById(R.id.inputSearch)
        mapFragment.getMapAsync(this)

        setupSearch()
    }

    private fun setupSearch() {
        searchUtils.setupSearchInput(
            searchInput = searchInput,
            onSearch = { searchText -> viewModel.searchHawkers(this, searchText) },
            onEmptySearch = {
                Toast.makeText(this, "Please enter an item to search", Toast.LENGTH_SHORT).show()
            }
        )

        searchUtils.setupSearchClear(
            searchInput = searchInput,
            onClear = { viewModel.fetchAllHawkers(this) }
        )
    }

    private fun setupObservers() {
        viewModel.hawkers.observe(this) { hawkersList ->
            Log.d("UserViewActivity", "Hawkers: $hawkersList")

            // Clear existing markers
            markers.forEach { it.remove() }
            markers.clear()

            val builder = LatLngBounds.Builder()

            // Use the existing processCoordinates function
            mapUtils.processCoordinates(
                mMap = mMap,
                builder = builder,
                hawkers = hawkersList,
                onMarkerAdd = { marker -> markers.add(marker) }  // Add to local markers list instead
            )

            // Show search results if this was a search operation
            if (viewModel.isSearchOperation.value == true && hawkersList.isNotEmpty()) {
                showSearchResults(hawkersList)
            }
        }

        viewModel.selectedHawker.observe(this) { hawkerInfo ->
            hawkerInfo?.let {
                bottomSheetUtils.showHawkerDetails(
                    hawkerInfo = it,
                    layoutInflater = layoutInflater,
                    onCallButtonClick = { hawker -> showCallRequestDialog(hawker) }
                )

                // Center map on selected hawker using mapUtils
                mapUtils.centerMapOnHawker(
                    mMap = mMap,
                    hawkerInfo = it,
                    markers = markers  // Pass the local markers list
                )
            }
        }
    }

    private fun showSearchResults(hawkersList: List<HawkerInfo>) {
        val hawkerSearchBottomSheet = HawkerSearchBottomSheet(applicationContext, viewModel)
        hawkerSearchBottomSheet.show(supportFragmentManager, "HawkerSearchBottomSheet")
        hawkerSearchBottomSheet.updateHawkersList(hawkersList)

        Toast.makeText(
            applicationContext,
            "Hawkers Found: ${hawkersList.size}",
            Toast.LENGTH_SHORT
        ).show()
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
        permissionUtils.checkLocationPermission {
            mMap.isMyLocationEnabled = true
            mapUtils.getCurrentLocation(
                fusedLocationClient = fusedLocationClient,
                mMap = mMap,
                onLocationNotFound = {
                    Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun setupMapClickListeners() {
        mMap.setOnMarkerClickListener { marker ->
            // Add logging to debug marker click
            Log.d("MarkerClick", "Marker clicked: ${marker.snippet}")

            val hawkerInfo = viewModel.findHawkerById(marker.snippet ?: "")
            if (hawkerInfo != null) {
                Log.d("MarkerClick", "Found hawker: ${hawkerInfo.name}")
                viewModel.setSelectedHawker(hawkerInfo)
            } else {
                Log.d("MarkerClick", "No hawker found for id: ${marker.snippet}")
            }
            true // Return true to indicate we've handled the click
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionUtils.handlePermissionResult(
            requestCode = requestCode,
            grantResults = grantResults,
            onGranted = { setupMap() },
            onDenied = {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showCallRequestDialog(hawkerInfo: HawkerInfo) {
        bottomSheetUtils.showCallDialog(
            hawkerInfo = hawkerInfo,
            layoutInflater = layoutInflater,
            onCallRequest = { hawker, name, note ->
                viewModel.sendCallRequest(this, hawker, name, note)
            }
        )
    }
}