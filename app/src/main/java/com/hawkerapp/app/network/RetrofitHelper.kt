package com.hawkerapp.app.network

import android.content.Context
import com.hawkerapp.app.models.HawkerInfo
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.hawkerapp.app.models.CustomLocation
import com.hawkerapp.app.models.FCMData
import com.hawkerapp.app.models.HawkerFormData
import com.hawkerapp.app.models.ImageUrlData
import com.hawkerapp.app.models.Item
import com.hawkerapp.app.models.OtpVerificationResponse
import com.hawkerapp.app.models.OtpVerifyRequest
import com.hawkerapp.app.models.UserData
import com.hawkerapp.app.models.UserRequestData
import com.hawkerapp.app.store.SessionManager
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
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

    fun updateHawkerFieldInServer(
        context: Context,
        hawkerId: String?,
        field: String,
        value: Any,
        onComplete: (HawkerFormData?) -> Unit
    ) {
        if(hawkerId.isNullOrEmpty()) {
            Log.d("RetrofitHelper", "HawkerId is null/empty")
            onComplete(null)
            return
        }

        // Special case for image as it needs to be uploaded first
        if(field == "imageUrl" && value is String) {
            uploadImageAndGetPublicUrl(value, { imageUrl ->
                Log.d("RetrofitHelper", "Image url : $imageUrl")
                updateHawkerInApi(context, hawkerId, field, imageUrl, onComplete)
            }, { error ->
                Log.d("RetrofitHelper", "Image update failed: $error")
                onComplete(null)
            })
        } else {
            // For all other fields, update directly
            updateHawkerInApi(context, hawkerId, field, value, onComplete)
        }
    }

    private fun updateHawkerInApi(
        context: Context,
        hawkerId: String,
        field: String,
        value: Any,
        onComplete: (HawkerFormData?) -> Unit
    ) {
        val token = SessionManager.getAuthToken(context) ?: run {
            onComplete(null)
            return
        }

        var json = JsonObject()
        if(field == "profile"){
            json = value as JsonObject
        }
        when(value) {
            is String -> json.addProperty(field, value)
            is Number -> json.addProperty(field, value)
            is Boolean -> json.addProperty(field, value)
            is List<*> -> {
                // Check if it's a List of Items
                if (value.all { it is Item }) {
                    val itemsArray = JsonArray()
                    value.forEach { item ->
                        item as Item  // Safe cast since we checked above
                        val itemObject = JsonObject().apply {
                            addProperty("name", item.name)
                            addProperty("price", item.price)
                            addProperty("quantity", item.quantity)
                        }
                        itemsArray.add(itemObject)
                    }
                    json.add(field, itemsArray)
                }
            }

        }
        Log.d("RetrofitHelper", "Json: $json")

        val hawkersFetchApi = getInstance().create(HawkersAPI::class.java)
        val call = hawkersFetchApi.updateDataForHawker("Bearer $token", json, hawkerId)

        call.enqueue(object : Callback<HawkerFormData> {
            override fun onResponse(call: Call<HawkerFormData>, response: Response<HawkerFormData>) {
                if(response.isSuccessful) {
                    Log.d("RetrofitHelper", "$field updated successfully : ${response.body()}")
                    onComplete(response.body())
                } else {
                    Log.d("RetrofitHelper", "update UNsuccesful: $response")
                    onComplete(null)
                }
            }

            override fun onFailure(call: Call<HawkerFormData>, t: Throwable) {
                Log.d("RetrofitHelper", "Error in update: ${t.message}")
                onComplete(null)
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

    fun sendHawkersData(context: Context,  hawkerData: HawkerFormData, onSuccess: (HawkerFormData?) -> Unit) {
        // Check if imagePath is not null or empty
        if (!hawkerData.imageurl.isNullOrEmpty()) {
            uploadImageAndGetPublicUrl(hawkerData.imageurl!!, { imageUrl ->
                Log.d("RetrofitHelper", "Image url : $imageUrl")
                // Set the returned URL to hawkerData.imagePath
                hawkerData.imageurl = imageUrl

                // Now, call the API to send hawker data
                executeSendHawkersData(context, hawkerData, onSuccess)
            }, { error ->
                Log.d("Upload", "Image upload failed: $error")
            })
        } else {
            // If there's no imagePath, call sendHawkerData directly
            executeSendHawkersData(context,  hawkerData, onSuccess)
        }
    }

    private fun executeSendHawkersData(context: Context, hawkerData: HawkerFormData, onSuccess: (HawkerFormData?) -> Unit) {
        val basicAuth = Credentials.basic(newBuildConfig.API_USERNAME, newBuildConfig.API_PASSWORD)

        val token = SessionManager.getAuthToken(context) ?: run {
            onSuccess(null)
            return
        }

        val hawkersFetchApi = getInstance().create(HawkersAPI::class.java)

        val call = hawkersFetchApi.sendHawkerData("Bearer $token", hawkerData)
        call.enqueue(object : Callback<HawkerFormData> {
            override fun onResponse(call: Call<HawkerFormData>, response: Response<HawkerFormData>) {
                Log.d("RetrofitHelper", "Response: ${response.body()}")
                if(response.isSuccessful) {
                    onSuccess(response.body()!!)
                } else {
                    Log.d("RetrofitHelper", "Error: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<HawkerFormData>, t: Throwable) {
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

    fun fetchUserRequests(context : Context, id: String, onSuccess: (List<UserRequestData>) -> Unit) {
        val hawkersFetchApi = getInstance().create(HawkersAPI::class.java)
        val token = SessionManager.getAuthToken(context) ?: run {
            onSuccess(emptyList())
            return
        }
        val call = hawkersFetchApi.fetchVisitRequestsAsync("Bearer $token",id)

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

    // In RetrofitHelper
    fun requestOtp(phoneNumber: String, callback: (Boolean) -> Unit) {
        val hawkersFetchApi = getInstance().create(HawkersAPI::class.java)
        val call = hawkersFetchApi.requestOtp(phoneNumber)
        call.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                Log.d("RetrofitHelper", "Response from request otp: ${response.body()}")
                if(response.isSuccessful) {
                    callback(true)
                } else {
                    Log.d("RetrofitHelper", "Response not successfull, Error: ${response}")
                    callback(false)
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Log.d("RetrofitHelper", "Failure !! Error: ${t.message}")

            }
        })
        return callback(true);
        // Implementation to make API call to request OTP
    }

    fun verifyOtp(otpVerifyRequest: OtpVerifyRequest, callback: (Response<OtpVerificationResponse>) -> Unit) {
        val hawkersFetchApi = getInstance().create(HawkersAPI::class.java)
        val call = hawkersFetchApi.verifyOtp(otpVerifyRequest)

        call.enqueue(object : Callback<OtpVerificationResponse> {

            override fun onResponse(
                call: Call<OtpVerificationResponse>,
                response: Response<OtpVerificationResponse>
            ) {
                Log.d("RetrofitHelper", "Response from verifyOtp: ${response.body()?.hawkerData}")
                callback(response)
            }

            override fun onFailure(call: Call<OtpVerificationResponse>, t: Throwable) {
                Log.e("RetrofitHelper", "Failure !! Error: ${t.message}")
                // You might want to create an error response or handle this differently
            }
        })
    }

}