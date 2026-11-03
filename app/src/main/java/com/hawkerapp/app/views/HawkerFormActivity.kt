package com.hawkerapp.app.views

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.hawkerapp.app.R
import com.hawkerapp.app.managers.HawkerManager
import com.hawkerapp.app.models.HawkerFormData
import com.hawkerapp.app.network.RetrofitHelper
import com.hawkerapp.app.utils.LocationProvider
import com.hawkerapp.app.repositories.HawkerLoginDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HawkerFormActivity : AppCompatActivity() {


    private val fragmentManager: FragmentManager = supportFragmentManager
    private lateinit var hawkerLoginDataRepository: HawkerLoginDataRepository
    private val hawkerManager = HawkerManager(this)
    private lateinit var nextButton: Button


    // Variable to track the current fragment
    private var currentFragment: Int = FRAGMENT_A

    private var verifiedPhone: String? = null
    private var hawkerId: String? = null

    companion object {
        private const val FRAGMENT_A = 1
        private const val FRAGMENT_B = 2
        private var hawkerFormData: HawkerFormData? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hawker_form)

        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        verifiedPhone = intent.getStringExtra("VERIFIED_PHONE")
        hawkerId = intent.getStringExtra("HAWKER_ID")

        if (verifiedPhone == null) {
            Toast.makeText(this, "Error: Phone number not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            if (currentFragment == FRAGMENT_B) {
                loadFirstFragment()
                currentFragment = FRAGMENT_A
                nextButton.text = "Add products  \u2192"
            } else {
                finish()
            }
        }

        nextButton = findViewById(R.id.next_button)
        nextButton.setOnClickListener {
            onNextButtonClicked()
        }
        loadFirstFragment()
    }


    private fun loadFirstFragment() {
        val fragment = HawkerSelfDetails().apply {
            arguments = Bundle().apply {
                putString("verified_phone", verifiedPhone)
            }
        }
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun loadSecondFragment() {
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HawkerItemDetails())
            .commit()
    }

    private fun onNextButtonClicked() {
        when (currentFragment) {
            FRAGMENT_A -> {
                val fragmentA =
                    supportFragmentManager.findFragmentById(R.id.fragment_container) as? HawkerSelfDetails
                if (fragmentA != null && !fragmentA.areAllDataFilled()) {
                    Toast.makeText(
                        this,
                        "Please fill all the fields.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                LocationProvider.init(this)
                Log.d("HawkerFormActivity", "hawkers details: ${fragmentA?.editTextName?.text.toString()} ${fragmentA?.imageFile}")
                LocationProvider.getLocation(
                    this,
                    { customLocation ->
                        // This block is executed when location is successfully retrieved
                        // Now you have the CustomLocation object
                        // You can use it here or pass it to another function
                        hawkerFormData = HawkerFormData(
                            0,
                            hawkerId,
                            fragmentA?.editTextName?.text.toString(),
                            fragmentA?.editTextCategory?.text.toString(),
                            verifiedPhone,
                            customLocation,
                            imageurl = fragmentA?.imageFile?.path
                        )
                        Log.d("HawkerFormActivity", "hawkerFromData is : $hawkerFormData")
                        // Call any function that depends on hawkerFormData or pass it to another function
                        // e.g., processHawkerFormData(hawkerFormData)
                    },
                    { errorMessage ->
                        // This block is executed when there's an error retrieving the location
                        Log.e("HawkerApp", errorMessage)
                    }
                )
                loadSecondFragment()
                currentFragment = FRAGMENT_B
                nextButton.text = "Submit"
            }

            FRAGMENT_B -> {
                val fragmentB =
                    supportFragmentManager.findFragmentById(R.id.fragment_container) as? HawkerItemDetails
                val itemsList = fragmentB?.getItemsList()?.subList(1, fragmentB.getItemsList().size)
                // Update HawkerFormData object with the items list
                hawkerFormData = hawkerFormData?.copy(items = itemsList)

                Log.d("HawkerFormActivity", "HawkerFormData: $hawkerFormData")

                RetrofitHelper.sendHawkersData(this,  hawkerFormData!!) {
                    Log.d("HawkerFormActivity", "Response: $it")
                    if(it == null){
                        Toast.makeText(this, "Error sending data", Toast.LENGTH_SHORT).show()
                        return@sendHawkersData
                    }
                    Toast.makeText(this, "Data sent successfully", Toast.LENGTH_SHORT).show()
                    hawkerManager.storeHawkerData(it)
                    it.id?.let { it1 -> hawkerManager.markAllHawkersInactive(it1) }
                    val intent = Intent(this, HawkerViewActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}