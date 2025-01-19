package com.hawkerapp.app.views

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import com.hawkerapp.app.MainActivity
import com.hawkerapp.app.adapters.VisitRequestAdapter
import com.hawkerapp.app.managers.HawkerManager
import com.hawkerapp.app.models.UserRequestData
import com.hawkerapp.app.viewmodels.HawkerViewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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
    private var notificationMenuItem: MenuItem? = null
    private lateinit var currentPhotoPath: String
    private var imageFile: File? = null


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val PICK_IMAGE_REQUEST = 2
        private const val CAMERA_REQUEST_CODE = 3
        private const val CAMERA_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hawker_view)

        viewModel = ViewModelProvider(this)[HawkerViewViewModel::class.java]
        hawkerManager = HawkerManager(application)


        setupToolbar()  // Call this before setupNavigation
        setupNavigation()
        setupMap()
        //setupViews()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadHawkerInfo()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Enable the action bar home button (hamburger menu)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu resource
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        notificationMenuItem = menu.findItem(R.id.action_notifications)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                viewModel.customerRequests.value?.let { customers ->
                    showCustomersPopup(customers)
                }
                viewModel.markRequestsAsRead()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.maps) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun observeViewModel() {
        viewModel.customerRequests.observe(this) { customers ->
            updateMapMarkers(customers)
            // showCustomersPopup(customers)
        }

        viewModel.hasUnreadRequests.observe(this) { hasUnread ->
            updateNotificationDot(hasUnread)
        }

        viewModel.currentLocation.observe(this) { location ->
            val currentLatLng = LatLng(location.latitude, location.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
        }
    }

    private fun updateNotificationDot(show: Boolean) {
        notificationMenuItem?.setIcon(
            if (show) R.drawable.ic_hawker_requests
            else R.drawable.ic_hawker_requests_no_dot
        )
    }

    private fun updateMapMarkers(customers: List<UserRequestData>) {
        if (!::mMap.isInitialized) {
            return  // Exit if map isn't ready yet
        }
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

        hawkerImageView.setOnClickListener {
            showImagePickerOptions()
        }

        // Load hawker info from hawkerManager
        // Observe hawker info changes
        viewModel.hawkerInfo.observe(this) { hawker ->
            // Load hawker image using Glide
            Glide.with(this)
                .load(hawker.imageurl)
                .circleCrop()
                .placeholder(R.drawable.default_profile)
                .error(R.drawable.default_profile)
                .into(hawkerImageView)

            hawkerNameTextView.text = hawker.name
            hawkerCategoryTextView.text = hawker.category
        }

        // Setup navigation item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileViewActivity::class.java))
                }
                R.id.nav_manage_items -> {
                    startActivity(Intent(this, ManageItemsActivity::class.java))
                }
                R.id.nav_logout -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        viewModel.logout(this@HawkerViewActivity)
                        withContext(Dispatchers.Main) {
                            startActivity(Intent(this@HawkerViewActivity, MainActivity::class.java))
                            finish()
                        }
                    }
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun showImagePickerOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Option")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> checkCameraPermission() // Take Photo
                1 -> openImagePicker() // Choose from Gallery
            }
        }
        builder.show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(this, "Error occurred while creating file", Toast.LENGTH_SHORT).show()
                null
            }
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "com.hawkerapp.app.fileprovider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun bitmapToFile(bitmap: Bitmap, quality: Int = 50): File {
        val file = File(cacheDir, "temp_image.jpg")
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        out.flush()
        out.close()
        return file
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to take a photo",
                    Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMapReady(mMap)
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val headerView = navigationView.getHeaderView(0)
        val hawkerImageView = headerView.findViewById<ImageView>(R.id.hawkerImageView)

        when (requestCode) {
            PICK_IMAGE_REQUEST -> {
                if (resultCode == Activity.RESULT_OK && data?.data != null) {
                    val imageUri: Uri = data.data!!
                    val inputStream = contentResolver.openInputStream(imageUri)
                    val selectedImage: Bitmap = BitmapFactory.decodeStream(inputStream)
                    imageFile = bitmapToFile(selectedImage)

                    // Update UI and ViewModel
                    Glide.with(this)
                        .load(imageFile)
                        .circleCrop()
                        .into(hawkerImageView)

                    viewModel.updateHawkerImage(this, imageFile!!.path)
                }
            }
            CAMERA_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val capturedImage: Bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                    imageFile = bitmapToFile(capturedImage)

                    // Update UI and ViewModel
                    Glide.with(this)
                        .load(imageFile)
                        .circleCrop()
                        .into(hawkerImageView)

                    viewModel.updateHawkerImage(this, imageFile!!.path)
                }
            }
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