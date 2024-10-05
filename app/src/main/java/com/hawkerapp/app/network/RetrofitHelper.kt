package com.hawkerapp.app.network

import com.hawkerapp.app.models.HawkerInfo
import android.util.Log
import com.google.gson.Gson
import com.hawkerapp.app.models.CustomLocation
import com.hawkerapp.app.models.FCMData
import com.hawkerapp.app.models.HawkerFormData
import com.hawkerapp.app.models.ImageUrlData
import com.hawkerapp.app.models.UserData
import com.hawkerapp.app.models.UserRequestData
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import com.hawkerapp.app.BuildConfig as newBuildConfig

object RetrofitHelper {
    private const val baseUrl = newBuildConfig.BASE_URL

    private fun getInstance(): Retrofit {
        return Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            // we need to add converter factory to
            // convert JSON object to Java object
            .build()
    }

    fun sendToken(fcmData : FCMData) {
        val api = getInstance().create(HawkersAPI::class.java)
        val call = api.sendFCMToken(fcmData)
        Log.d("RetrofitHelper", "Enqueuing the call for token to server")
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if(response.isSuccessful) {
                    Log.d("RetrofitHelper", "Token sent successfully")
                } else {
                    Log.d("retrofitHelper", "Token sending wasn't successful: ${response}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("RetrofitHelper", "Token sending failed")
            }
        })
    }

    private fun uploadImageAndGetPublicUrl(imagePath: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val file = File(imagePath)
        if (!file.exists()) {
            onFailure("File does not exist: $imagePath")
            return
        }

        // Prepare the file part
        val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
        val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val hawkersFetchApi = getInstance().create(HawkersAPI::class.java)
        // Call the API
        val call = hawkersFetchApi.uploadFile("Basic ZGV2cmFqOmphcnZlZA==", filePart)
        call.enqueue(object : retrofit2.Callback<ImageUrlData> {
            override fun onResponse(call: Call<ImageUrlData>, response: retrofit2.Response<ImageUrlData>) {
                if (response.isSuccessful) {
                    // Extract the public URL from the response
                    val url = response.body()?.filePath ?: "No URL returned"
                    onSuccess(url)
                } else {
                    onFailure("Request failed with code: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ImageUrlData>, t: Throwable) {
                onFailure("Error occurred: ${t.message}")
            }
        })
    }

    fun sendHawkersData(hawkerData: HawkerFormData, onSuccess: (HawkerInfo) -> Unit) {
        // Check if imagePath is not null or empty
        if (!hawkerData.imageurl.isNullOrEmpty()) {
            uploadImageAndGetPublicUrl(hawkerData.imageurl!!, { imageUrl ->
                Log.d("RetrofitHelper", imageUrl)
                // Set the returned URL to hawkerData.imagePath
                hawkerData.imageurl = imageUrl

                // Now, call the API to send hawker data
                executeSendHawkersData(hawkerData, onSuccess)
            }, { error ->
                Log.d("Upload", "Image upload failed: $error")
            })
        } else {
            // If there's no imagePath, call sendHawkerData directly
            executeSendHawkersData(hawkerData, onSuccess)
        }
    }

    private fun executeSendHawkersData(hawkerData: HawkerFormData, onSuccess: (HawkerInfo) -> Unit) {
        val basicAuth = Credentials.basic(newBuildConfig.API_USERNAME, newBuildConfig.API_PASSWORD)

        val hawkersFetchApi = getInstance().create(HawkersAPI::class.java)

        // Convert the HawkerFormData to JSON
        val hawkerDataJson = Gson().toJson(hawkerData)
        val hawkerDataRequestBody = hawkerDataJson.toRequestBody("application/json".toMediaTypeOrNull())

        val call = hawkersFetchApi.sendHawkerData(basicAuth, hawkerData)
        call.enqueue(object : Callback<HawkerInfo> {
            override fun onResponse(call: Call<HawkerInfo>, response: Response<HawkerInfo>) {
                Log.d("RetrofitHelper", "Response: ${response.body()}")
                if(response.isSuccessful) {
                    onSuccess(response.body()!!)
                } else {
                    Log.d("RetrofitHelper", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<HawkerInfo>, t: Throwable) {
                Log.d("RetrofitHelper", "Error: ${t.message}")

            }

        })
    }

    fun sendUserRequest(requestBody: UserData, onSuccess: (UserData) -> Unit) {
        val hawkersFetchApi = getInstance().create(HawkersAPI::class.java)
        val call = hawkersFetchApi.sendUserRequest(requestBody)
        call.enqueue(object : Callback<UserData> {
            override fun onResponse(call: Call<UserData>, response: Response<UserData>) {
                Log.d("RetrofitHelper", "Response: ${response.body()}")
                if(response.isSuccessful) {
                    onSuccess(response.body()!!)
                } else {
                    Log.d("RetrofitHelper", "Error: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<UserData>, t: Throwable) {
                Log.d("RetrofitHelper", "Error: ${t.message}")

            }

        })
    }

    fun fetchHawkersData( long : Double, lat: Double, onSuccess: (List<HawkerInfo>) -> Unit) {

        val basicAuth = Credentials.basic(newBuildConfig.API_USERNAME, newBuildConfig.API_PASSWORD)

        val hawkersFetchApi = getInstance().create(HawkersAPI::class.java)
        val call = hawkersFetchApi.fetchHawkersAsync(basicAuth, long, lat)
        call.enqueue(object : Callback<List<HawkerInfo>> {
            override fun onResponse(call: Call<List<HawkerInfo>>, response: Response<List<HawkerInfo>>) {
                Log.d("RetrofitHelper", " fetchHawkersData Response: ${response.body()}")
                if(response.isSuccessful) {
                    onSuccess(response.body()!!)
                } else {
                    Log.d("RetrofitHelper", "Error: ${response.errorBody()} ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<List<HawkerInfo>>, t: Throwable) {
                Log.d("RetrofitHelper", "Error: ${t.message}")

            }

        })
    }

    fun fetchUserRequests(id: String, onSuccess: (List<UserRequestData>) -> Unit) {
        val hawkersFetchApi = getInstance().create(HawkersAPI::class.java)
        val call = hawkersFetchApi.fetchVisitRequestsAsync(id)

        call.enqueue(object : Callback<List<UserRequestData>> {
            override fun onResponse(call: Call<List<UserRequestData>>, response: Response<List<UserRequestData>>) {
                Log.d("RetrofitHelper", "User Requests Response: ${response.body()}")
                if(response.isSuccessful) {
                    onSuccess(response.body()!!)
                } else {
                    Log.d("RetrofitHelper", "Error: ${response.errorBody()}")
                }
            }

            override fun onFailure(call: Call<List<UserRequestData>>, t: Throwable) {
                Log.d("RetrofitHelper", "Error: ${t.message}")

            }
        })
    }

    fun getHawkersWithItem(item: String, location: CustomLocation, onSuccess: (List<HawkerInfo>) -> Unit) {
        val hawkersApi = getInstance().create(HawkersAPI::class.java)
        val call = hawkersApi.getHawkersWithItemAsync(item, location.longitude, location.latitude)

        call.enqueue(object : Callback<List<HawkerInfo>> {
            override fun onResponse(call: Call<List<HawkerInfo>>, response: Response<List<HawkerInfo>>) {
                Log.d("RetrofitHelper", "Hawkers with Item Response: ${response.body()}")
                if (response.isSuccessful) {
                    response.body()?.let { hawkers ->
                        onSuccess(hawkers)
                    } ?: run {
                        Log.d("RetrofitHelper", "Error: Response body is null")
                    }
                } else {
                    Log.d("RetrofitHelper", "Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<List<HawkerInfo>>, t: Throwable) {
                Log.d("RetrofitHelper", "Error: ${t.message}")
            }
        })
    }
}