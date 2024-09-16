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
import android.provider.MediaStore
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
import com.hawkerapp.app.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HawkerSelfDetails : Fragment() {


    var editTextName : EditText? = null
    var editTextCategory : EditText? = null
    var editTextPhone : EditText? = null

    private lateinit var imageUploader: ImageView
    private val PICK_IMAGE_REQUEST = 1
    private val CAMERA_REQUEST_CODE = 2
    private val CAMERA_PERMISSION_REQUEST = 100
    private lateinit var currentPhotoPath: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_hawker_self_details, container, false)
        editTextName = view?.findViewById(R.id.name_edit_text)
        editTextCategory = view?.findViewById(R.id.category_edit_text)
        editTextPhone = view?.findViewById(R.id.phone_edit_text)

        imageUploader = view?.findViewById(R.id.imageUploader)!!

        // Set click listener for the image view
        imageUploader.setOnClickListener {
            showImagePickerOptions()
        }

        return view

    }

    fun areAllDataFilled(): Boolean {
        val a = editTextName?.text?.toString()?.trim()?.isNotEmpty() ?: false
        val b = editTextCategory?.text?.toString()?.trim()?.isNotEmpty() ?: false
        val c = editTextPhone?.text?.toString()?.trim()?.isNotEmpty() ?: false
        return a && b && c
    }

    private fun showImagePickerOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        val builder = android.app.AlertDialog.Builder(requireContext())
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
        } else {
            openCamera()
        }
    }

    // Open the camera to capture an image
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

    // Create an image file
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }

    // Open the gallery to select an image
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri: Uri = data.data!!
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val selectedImage: Bitmap = BitmapFactory.decodeStream(inputStream)
            imageUploader.setImageBitmap(selectedImage)
        } else if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val capturedImage: Bitmap = BitmapFactory.decodeFile(currentPhotoPath)
            imageUploader.setImageBitmap(capturedImage)
        }
    }
}