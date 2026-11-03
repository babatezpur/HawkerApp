package com.hawkerapp.app.views

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.Manifest
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hawkerapp.app.BuildConfig
import com.hawkerapp.app.R
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class HawkerSelfDetails : Fragment() {

    var editTextName: EditText? = null
    var editTextCategory: EditText? = null
    var imageFile: File? = null

    var selectedPlaceName: String? = null

    private lateinit var imageUploader: ImageView
    private lateinit var locationEditText: EditText
    private lateinit var suggestionsRecyclerView: RecyclerView
    private lateinit var suggestionAdapter: PlaceSuggestionAdapter

    private val PICK_IMAGE_REQUEST = 1
    private val CAMERA_REQUEST_CODE = 2
    private val CAMERA_PERMISSION_REQUEST = 100
    private lateinit var currentPhotoPath: String

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private var isSettingText = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_hawker_self_details, container, false)
        editTextName = view.findViewById(R.id.name_edit_text)
        editTextCategory = view.findViewById(R.id.category_edit_text)
        locationEditText = view.findViewById(R.id.location_edit_text)

        imageUploader = view.findViewById(R.id.imageUploader)
        imageUploader.setOnClickListener { showImagePickerOptions() }

        view.findViewById<View>(R.id.cameraOverlay)?.setOnClickListener {
            showImagePickerOptions()
        }

        // Set up suggestions RecyclerView
        suggestionsRecyclerView = view.findViewById(R.id.places_suggestions)
        suggestionAdapter = PlaceSuggestionAdapter { suggestion ->
            isSettingText = true
            locationEditText.setText(suggestion.fullText)
            selectedPlaceName = suggestion.fullText
            isSettingText = false
            suggestionsRecyclerView.visibility = View.GONE
        }
        suggestionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        suggestionsRecyclerView.adapter = suggestionAdapter

        // Text watcher with debounce
        locationEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isSettingText) return
                selectedPlaceName = null
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                val query = s?.toString()?.trim() ?: ""
                if (query.length < 3) {
                    suggestionsRecyclerView.visibility = View.GONE
                    return
                }
                searchRunnable = Runnable { fetchPlaceSuggestions(query) }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
        })

        return view
    }

    private fun fetchPlaceSuggestions(query: String) {
        thread {
            try {
                val url = URL("https://places.googleapis.com/v1/places:autocomplete")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                conn.doOutput = true

                val body = JSONObject().apply {
                    put("input", query)
                    put("includedRegionCodes", org.json.JSONArray().put("in"))
                }

                conn.outputStream.use { it.write(body.toString().toByteArray()) }

                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val suggestionsArray = json.optJSONArray("suggestions") ?: return@thread

                val results = mutableListOf<PlaceSuggestion>()
                for (i in 0 until suggestionsArray.length()) {
                    val prediction = suggestionsArray.getJSONObject(i)
                        .optJSONObject("placePrediction") ?: continue
                    val structured = prediction.optJSONObject("structuredFormat") ?: continue
                    val mainText = structured.optJSONObject("mainText")?.optString("text", "") ?: ""
                    val secondaryText = structured.optJSONObject("secondaryText")?.optString("text", "") ?: ""
                    val fullText = prediction.optJSONObject("text")?.optString("text", "") ?: ""
                    results.add(PlaceSuggestion(mainText, secondaryText, fullText))
                }

                Handler(Looper.getMainLooper()).post {
                    if (results.isNotEmpty()) {
                        suggestionAdapter.updateSuggestions(results)
                        suggestionsRecyclerView.visibility = View.VISIBLE
                    } else {
                        suggestionsRecyclerView.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e("HawkerSelfDetails", "Places autocomplete failed", e)
            }
        }
    }

    fun areAllDataFilled(): Boolean {
        val a = editTextName?.text?.toString()?.trim()?.isNotEmpty() ?: false
        val b = editTextCategory?.text?.toString()?.trim()?.isNotEmpty() ?: false
        val c = !selectedPlaceName.isNullOrBlank()
        return a && b && c
    }

    private fun showImagePickerOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Select Option")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> checkCameraPermission()
                1 -> openImagePicker()
            }
        }
        builder.show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        } else {
            openCamera()
        }
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                Toast.makeText(requireContext(), "Error occurred while creating file", Toast.LENGTH_SHORT).show()
                null
            }
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(requireContext(), "com.hawkerapp.app.fileprovider", it)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri: Uri = data.data!!
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val selectedImage: Bitmap = BitmapFactory.decodeStream(inputStream)
            imageUploader.setImageBitmap(selectedImage)
            imageFile = bitmapToFile(selectedImage)
        } else if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val capturedImage: Bitmap = BitmapFactory.decodeFile(currentPhotoPath)
            imageUploader.setImageBitmap(capturedImage)
            imageFile = bitmapToFile(capturedImage)
        }
    }

    private fun bitmapToFile(bitmap: Bitmap, quality: Int = 50): File {
        val file = File(requireContext().cacheDir, "temp_image.jpg")
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        out.flush()
        out.close()
        return file
    }
}
