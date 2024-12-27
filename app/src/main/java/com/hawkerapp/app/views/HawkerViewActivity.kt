package com.hawkerapp.app.views

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hawkerapp.app.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import com.hawkerapp.app.adapters.VisitRequestAdapter
import com.hawkerapp.app.managers.HawkerManager
import com.hawkerapp.app.models.UserRequestData
import com.hawkerapp.app.network.RetrofitHelper
import com.hawkerapp.app.viewmodels.HawkerViewViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// the  utton isnt working. check it.
class HawkerViewActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var viewModel: HawkerViewViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private lateinit var btnFetchRequests:  Button
    private var activeHawkerId: String? = null
    private lateinit var floatingWindow: PopupWindow
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VisitRequestAdapter
    private val markersMap = mutableMapOf<String, Marker>()
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var hawkerManager: HawkerManager

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hawker_view)

        viewModel = ViewModelProvider(this)[HawkerViewViewModel::class.java]
        hawkerManager = HawkerManager(application)


        setupNavigation()
        setupMap()
        setupViews()
        observeViewModel()
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.maps) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupViews() {
        btnFetchRequests = findViewById(R.id.btnFetchRequests)
        btnFetchRequests.setOnClickListener {
            viewModel.loadCustomers()
        }
    }

    private fun observeViewModel() {
        viewModel.customerRequests.observe(this) { customers ->
            updateMapMarkers(customers)
            showCustomersPopup(customers)
        }

        viewModel.currentLocation.observe(this) { location ->
            val currentLatLng = LatLng(location.latitude, location.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        }
    }

    private fun updateMapMarkers(customers: List<UserRequestData>) {
        markersMap.clear()
        mMap.clear()

        for (user in customers) {
            val userLocation = LatLng(user.location.latitude, user.location.longitude)
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(userLocation)
                    .title(user.customerName)
            )
            if (marker != null) {
                markersMap[user.customerName] = marker
            }
        }
    }



    private fun showCustomersPopup(customers: List<UserRequestData>) {
        val floatingWindowLayout = layoutInflater.inflate(
            R.layout.visit_requests_floating_window,
            null
        )
        recyclerView = floatingWindowLayout.findViewById(R.id.visitReqsRecyclerView)

        if (recyclerView.layoutManager == null) {
            recyclerView.layoutManager = LinearLayoutManager(this)
        }

        adapter = VisitRequestAdapter(customers) { user ->
            handleCustomerSelection(user)
        }
        recyclerView.adapter = adapter

        showPopupWindow(floatingWindowLayout)
    }

    private fun handleCustomerSelection(user: UserRequestData) {
        markersMap[user.customerName]?.let { marker ->
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15f))
            marker.showInfoWindow()
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        }
        floatingWindow.dismiss()
    }

    private fun showPopupWindow(layout: View) {
        floatingWindow = PopupWindow(
            layout,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        floatingWindow.showAtLocation(window.decorView.rootView, Gravity.CENTER, 0, 0)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
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
            return
        }

        setupLocationTracking()
    }

    private fun setupLocationTracking() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return  // Return if we don't have permission
        }

        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let { viewModel.updateCurrentLocation(it) }
        }
    }

    private fun loadCustomers() {

        // Fetch customers from the server
        // Display customers on the map
        if (activeHawkerId == null) {
            Log.d("hawkerViewActivity", "No active hawker")
            return
        }
        RetrofitHelper.fetchUserRequests(activeHawkerId!!) {
            Log.d("HawkerViewActivity", "Users fetched")
            val customers = it
            val markersMap = mutableMapOf<String, Marker>()

            for (user in it) {
                val userLocation = LatLng(user.location.latitude, user.location.longitude)
                val marker =
                    mMap.addMarker(MarkerOptions().position(userLocation).title(user.customerName))
                if (marker != null) {
                    markersMap[user.customerName] = marker
                }
            }



            Log.d(
                "HawkerViewActivity",
                "Inflating the requests popup with customers: ${customers.size}"
            )
            val floatingWindowLayout =
                layoutInflater.inflate(R.layout.visit_requests_floating_window, null)
            val recyclerView =
                floatingWindowLayout.findViewById<RecyclerView>(R.id.visitReqsRecyclerView)

            Log.d(
                "HawkerViewActivity",
                "The linearlayoutmanager is : ${recyclerView.layoutManager}"
            )
            if (recyclerView.layoutManager == null)
                recyclerView.layoutManager = LinearLayoutManager(this)


            val adapter = VisitRequestAdapter(customers) { user ->
                val marker = markersMap[user.customerName]
                if (marker != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15f))
                    marker.showInfoWindow()  // Show info window to highlight
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)) // Custom method to highlight marker
                }
                floatingWindow.dismiss()
            }
            recyclerView.adapter = adapter
            Log.d(
                "HawkerViewActivity",
                "Inflating completed, recyclerView: ${recyclerView.adapter}"
            )

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

    private fun setupNavigation() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        // Setup hamburger icon
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Setup navigation header
        val headerView = navigationView.getHeaderView(0)
        val hawkerImageView = headerView.findViewById<ImageView>(R.id.hawkerImageView)
        val hawkerNameTextView = headerView.findViewById<TextView>(R.id.hawkerNameTextView)
        val hawkerCategoryTextView = headerView.findViewById<TextView>(R.id.hawkerCategoryTextView)

        // Load hawker info from hawkerManager
        lifecycleScope.launch(Dispatchers.IO) {
            val hawkerId = hawkerManager.getActiveHawkerId()
            val hawker = hawkerManager.getHawkerInfo(hawkerId)  // Assuming this method exists
            withContext(Dispatchers.Main) {
                hawker?.let {
                    // Load hawker image using Glide
                    Glide.with(this@HawkerViewActivity)
                        .load("https://picsum.photos/200/300")
                        .circleCrop()
                        .into(hawkerImageView)

                    hawkerNameTextView.text = hawker.name
                    hawkerCategoryTextView.text = hawker.category
                }
            }
        }

        // Setup navigation item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    // startActivity(Intent(this, ProfileActivity::class.java))
                }
                R.id.nav_manage_items -> {
                    startActivity(Intent(this, ManageItemsActivity::class.java))
                }
                R.id.nav_logout -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        viewModel.logout()
                        withContext(Dispatchers.Main) {
                            startActivity(Intent(this@HawkerViewActivity, PreHawkerScreenActivity::class.java))
                            finish()
                        }
                    }
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }


}