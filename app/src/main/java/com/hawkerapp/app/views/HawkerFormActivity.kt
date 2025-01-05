package com.hawkerapp.app.views

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
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

    //private lateinit var hawkerFormData: HawkerFormData


    // Variable to track the current fragment
    private var currentFragment: Int = FRAGMENT_A

    companion object {
        private const val FRAGMENT_PHONE = 0
        private const val FRAGMENT_A = 1
        private const val FRAGMENT_B = 2
        private var hawkerFormData: HawkerFormData? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hawker_form)
        nextButton = findViewById<Button>(R.id.next_button)
        nextButton.setOnClickListener {
            onNextButtonClicked()
        }
        loadPhoneVerificationFragment()
    }

    private fun loadPhoneVerificationFragment() {
        currentFragment = FRAGMENT_PHONE
        nextButton.visibility = View.GONE  // Hide the next button
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HawkerPhoneDetails())
            .commit()
    }

    // Function to be called after successful OTP verification
    fun proceedToSelfDetails(verifiedPhone: String) {
        currentFragment = FRAGMENT_A
        nextButton.visibility = View.VISIBLE  // Show the next button again
        loadFirstFragment(verifiedPhone)
    }

    private fun loadFirstFragment(verifiedPhone: String) {
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
                            null,
                            fragmentA?.editTextName?.text.toString(),
                            fragmentA?.editTextCategory?.text.toString(),
                            fragmentA?.editTextPhone?.text.toString(),
                            customLocation,
                            imageurl = fragmentA?.imageFile?.path
                        )
                        Log.d("HawkerFormActivity", "hawkerFromData is : $hawkerFormData")
                        // Call any function that depends on hawkerFormData or pass it to another function
                        // e.g., processHawkerFormData(hawkerFormData)
                    },
                    { errorMessage ->
                        // This block is executed when there's an error retrieving the location
                        // Handle the error message accordingly
                        Log.e("HawkerApp", errorMessage)
                    }
                )
                loadSecondFragment()
                currentFragment = FRAGMENT_B
            }

            FRAGMENT_B -> {
                hawkerLoginDataRepository = HawkerLoginDataRepository(this)
                val fragmentB =
                    supportFragmentManager.findFragmentById(R.id.fragment_container) as? HawkerItemDetails
                val itemsList = fragmentB?.getItemsList()?.subList(1, fragmentB.getItemsList().size)
                // Update HawkerFormData object with the items list
                hawkerFormData = hawkerFormData?.copy(items = itemsList)

                Log.d("HawkerFormActivity", "HawkerFormData: $hawkerFormData")

                RetrofitHelper.sendHawkersData(hawkerFormData!!) {
                    Log.d("HawkerFormActivity", "Response: $it")
                    Toast.makeText(this, "Data sent successfully", Toast.LENGTH_SHORT).show()
                    hawkerManager.storeHawkerData(it)
                    hawkerManager.markAllHawkersInactive(it.id)
                    val intent = Intent(this, HawkerViewActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}