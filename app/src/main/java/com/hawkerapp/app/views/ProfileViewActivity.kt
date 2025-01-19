package com.hawkerapp.app.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.hawkerapp.app.R
import com.hawkerapp.app.viewmodels.ProfileViewViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileViewActivity : AppCompatActivity() {
    private lateinit var viewModel: ProfileViewViewModel
    private lateinit var profileImage: ImageView
    private lateinit var nameEditText: EditText
    private lateinit var categoryEditText: EditText
    private lateinit var createdAtText: TextView
    private lateinit var phoneText: TextView
    private lateinit var saveButton: Button
    private var date: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_view)

        viewModel = ViewModelProvider(this)[ProfileViewViewModel::class.java]



        initViews()
        setupObservers()
        setupListeners()
    }

    private fun initViews() {
        profileImage = findViewById(R.id.profileImage)
        nameEditText = findViewById(R.id.nameEditText)
        categoryEditText = findViewById(R.id.categoryEditText)
        createdAtText = findViewById(R.id.createdAtText)
        saveButton = findViewById(R.id.saveButton)
        phoneText = findViewById(R.id.phoneText)
    }

    private fun setupObservers() {
        viewModel.hawkerInfo.observe(this) { hawker ->
            // Load image
            Glide.with(this)
                .load(hawker.imageurl)
                .circleCrop()
                .placeholder(R.drawable.default_profile)
                .into(profileImage)

            // Set text fields
            nameEditText.setText(hawker.name)
            categoryEditText.setText(hawker.category)
            phoneText.text = hawker.phone


            date = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val inputDate = inputFormat.parse(hawker.createdAt)
                inputDate?.let { outputFormat.format(it) } ?: "Date not available"
            } catch (e: Exception) {
                Log.e("DateFormatting", "Error parsing date: ${e.message}")
                "Date not available"
            }

            createdAtText.text = date
        }

        viewModel.isDataChanged.observe(this) { isChanged ->
            saveButton.isEnabled = isChanged
        }

        viewModel.updateStatus.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.checkForChanges(
                    nameEditText.text.toString(),
                    categoryEditText.text.toString()
                )
            }
        }

        nameEditText.addTextChangedListener(textWatcher)
        categoryEditText.addTextChangedListener(textWatcher)

        saveButton.setOnClickListener {
            viewModel.updateProfile(
                nameEditText.text.toString(),
                categoryEditText.text.toString()
            )
        }
    }
}