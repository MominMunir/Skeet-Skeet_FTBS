package com.example.smd_fyp.api

import com.example.smd_fyp.model.Booking
import com.example.smd_fyp.model.Favorite
import com.example.smd_fyp.model.GroundApi
import com.example.smd_fyp.model.Notification
import com.example.smd_fyp.model.Review
import com.example.smd_fyp.model.User
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * PHP API Service for XAMPP/localhost
 * Base URL is configured via strings.xml (api_ip and api_base_url)
 * The API client will use the IP from strings.xml to build the base URL
 */
interface PhpApiService {
    
    // Grounds API
    @GET("grounds.php")
    suspend fun getGrounds(): Response<List<GroundApi>>
    
    @GET("grounds.php")
    suspend fun getGround(@Query("id") id: String): Response<GroundApi>
    
    @POST("grounds.php")
    suspend fun createGround(@Body ground: GroundApi): Response<GroundApi>
    
    @PUT("grounds.php")
    suspend fun updateGround(@Body ground: GroundApi): Response<GroundApi>
    
    @DELETE("grounds.php")
    suspend fun deleteGround(@Query("id") id: String): Response<ResponseBody>
    
    // Bookings API
    @GET("bookings.php")
    suspend fun getBookings(@Query("userId") userId: String? = null): Response<List<Booking>>
    
    @GET("bookings.php")
    suspend fun getBooking(@Query("id") id: String): Response<Booking>
    
    @POST("bookings.php")
    suspend fun createBooking(@Body booking: Booking): Response<Booking>
    
    @PUT("bookings.php")
    suspend fun updateBooking(@Body booking: Booking): Response<Booking>
    
    @DELETE("bookings.php")
    suspend fun deleteBooking(@Query("id") id: String): Response<ResponseBody>
    
    // Users API
    @GET("users.php")
    suspend fun getUser(@Query("id") id: String): Response<User>
    
    @GET("users.php")
    suspend fun getUserByEmail(@Query("email") email: String): Response<User>
    
    @POST("users.php")
    suspend fun createUser(@Body user: User): Response<User>
    
    @PUT("users.php")
    suspend fun updateUser(@Body user: User): Response<User>
    
    @DELETE("users.php")
    suspend fun deleteUser(@Query("id") id: String): Response<ResponseBody>
    
    // Reviews API
    @GET("reviews.php")
    suspend fun getReviews(@Query("groundId") groundId: String? = null, @Query("userId") userId: String? = null): Response<List<Review>>
    
    @GET("reviews.php")
    suspend fun getReview(@Query("id") id: String): Response<Review>
    
    @POST("reviews.php")
    suspend fun createReview(@Body review: Review): Response<Review>
    
    @PUT("reviews.php")
    suspend fun updateReview(@Body review: Review): Response<Review>
    
    @DELETE("reviews.php")
    suspend fun deleteReview(@Query("id") id: String): Response<ResponseBody>
    
    // Notifications API
    @GET("notifications.php")
    suspend fun getNotifications(@Query("userId") userId: String? = null): Response<List<Notification>>
    
    @GET("notifications.php")
    suspend fun getNotification(@Query("id") id: String): Response<Notification>
    
    @POST("notifications.php")
    suspend fun createNotification(@Body notification: Notification): Response<Notification>
    
    @PUT("notifications.php")
    suspend fun updateNotification(@Body notification: Notification): Response<Notification>
    
    @DELETE("notifications.php")
    suspend fun deleteNotification(@Query("id") id: String): Response<ResponseBody>
    
    // FCM Token API
    @POST("fcm_tokens.php")
    suspend fun registerFCMToken(@Body tokenData: Map<String, String>): Response<ResponseBody>
    
    // Favorites API
    @GET("favorites.php")
    suspend fun getFavorites(@Query("userId") userId: String? = null): Response<List<Favorite>>
    
    @GET("favorites.php")
    suspend fun getFavorite(@Query("id") id: String): Response<Favorite>
    
    @POST("favorites.php")
    suspend fun createFavorite(@Body favorite: Favorite): Response<Favorite>
    
    @DELETE("favorites.php")
    suspend fun deleteFavorite(@Query("id") id: String): Response<ResponseBody>
    
    // Image Upload API
    @Multipart
    @POST("upload.php")
    suspend fun uploadImage(
        @Part("folder") folder: RequestBody, // e.g., "grounds" or "users"
        @Part image: MultipartBody.Part
    ): Response<ImageUploadResponse>
    
    // Base64 Image Upload (for Android)
    @FormUrlEncoded
    @POST("upload.php")
    suspend fun uploadBase64Image(
        @Field("image") base64Image: String,
        @Field("folder") folder: String = "general",
        @Field("imageType") imageType: String = "png"
    ): Response<ImageUploadResponse>
    
    @GET("images.php")
    suspend fun getImageUrl(@Query("path") path: String): Response<ImageUrlResponse>
}

data class ImageUploadResponse(
    val success: Boolean,
    val message: String,
    val path: String? = null,
    val url: String? = null,
    val imageName: String? = null
)

data class ImageUrlResponse(
    val success: Boolean,
    val url: String? = null,
    val path: String? = null
)
