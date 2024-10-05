package com.hawkerapp.app.network

import com.hawkerapp.app.models.FCMData
import com.hawkerapp.app.models.HawkerFormData
import com.hawkerapp.app.models.HawkerInfo
import com.hawkerapp.app.models.ImageUrlData
import com.hawkerapp.app.models.UserData
import com.hawkerapp.app.models.UserRequestData
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query


interface HawkersAPI {

    @GET("hawkers")
    fun fetchHawkersAsync(
        @Header("Authorization") authHeader: String,
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double
    ): Call<List<HawkerInfo>>

    @GET("hawkers/{id}/visit-requests")
    @Headers(
        "Content-Type: application/json",
        "Authorization: Basic ZGV2cmFqOmphcnZlZA==",
        "Cookie: JSESSIONID=7BF55B5F644787F928FEA318B4244E06"
    )
    fun fetchVisitRequestsAsync(
        @Path("id") id: String
    ): Call<List<UserRequestData>>

    @POST("hawkers/token")
    @Headers(
        "Content-Type: application/json",
        "Authorization: Basic ZGV2cmFqOmphcnZlZA==",
        "Cookie: JSESSIONID=7BF55B5F644787F928FEA318B4244E06"
    )
    fun sendFCMToken(@Body fcmData: FCMData) : Call<Void>

    @Headers("Content-Type: application/json")
    @POST("hawkers")
    fun sendHawkerData(
        @Header("Authorization") authHeader: String,
        @Body requestData: HawkerFormData
    ): Call<HawkerInfo>

    @GET("hawkers/661b893356b89e0c1d7f4bcd/visit-requests")
    fun getVisitRequestsWithAuth(@Header("Authorization") authHeader: String?): Call<UserData>?

    @GET("hawkers")
    @Headers(
        "Content-Type: application/json",
        "Authorization: Basic ZGV2cmFqOmphcnZlZA==",
        "Cookie: JSESSIONID=7BF55B5F644787F928FEA318B4244E06"
    )
    fun getHawkersWithItemAsync(
        @Query("search") item: String,
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double
    ): Call<List<HawkerInfo>>


    @POST("hawkers/visit-requests")
    @Headers(
        "Content-Type: application/json",
        "Authorization: Basic ZGV2cmFqOmphcnZlZA==",
        "Cookie: JSESSIONID=7BF55B5F644787F928FEA318B4244E06"
    )
    fun sendUserRequest(@Body requestBody: UserData?): Call<UserData>

    @Multipart
    @POST("files/s3") // Endpoint for uploading the file
    fun uploadFile(
        @Header("Authorization") authHeader: String, // Authorization header
        @Part file: MultipartBody.Part // The file to upload
    ): Call<ImageUrlData>

}