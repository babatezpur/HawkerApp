package com.hawkerapp.app.network

import com.hawkerapp.app.models.HawkerInfo
import android.util.Log
import com.hawkerapp.app.models.FCMData
import com.hawkerapp.app.models.HawkerFormData
import com.hawkerapp.app.models.UserData
import com.hawkerapp.app.models.UserRequestData
import okhttp3.Credentials
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitHelper {
    private const val baseUrl = "http://13.233.38.184:5001/"

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

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if(response.isSuccessful) {
                    Log.d("RetrofitHelper", "Token sent successfully")
                } else {
                    Log.d("retrofitHelper", "Token sending wasn't successful")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.d("RetrofitHelper", "Token sending failed")
            }
        })
    }

    fun sendHawkersData(hawkerData: HawkerFormData, onSuccess: (HawkerInfo) -> Unit) {
        val basicAuth = Credentials.basic("devraj", "jarved")

        val hawkersFetchApi = getInstance().create(HawkersAPI::class.java)
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

    fun fetchHawkersData( onSuccess: (List<HawkerInfo>) -> Unit) {

        val basicAuth = Credentials.basic("devraj", "jarved")

        val hawkersFetchApi = getInstance().create(HawkersAPI::class.java)
        val call = hawkersFetchApi.fetchHawkersAsync(basicAuth)
        call.enqueue(object : Callback<List<HawkerInfo>> {
            override fun onResponse(call: Call<List<HawkerInfo>>, response: Response<List<HawkerInfo>>) {
                Log.d("RetrofitHelper", "Response: ${response.body()}")
                if(response.isSuccessful) {
                    onSuccess(response.body()!!)
                } else {
                    Log.d("RetrofitHelper", "Error: ${response.errorBody()}")
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
}