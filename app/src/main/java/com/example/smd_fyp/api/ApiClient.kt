package com.example.smd_fyp.api

import android.content.Context
import com.example.smd_fyp.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var retrofitInstance: Retrofit? = null
    private var phpApiServiceInstance: PhpApiService? = null
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Get base URL from strings.xml
     * Format: http://{IP}/skeetskeet/api/
     */
    fun getBaseUrl(context: Context): String {
        val ip = context.getString(R.string.api_ip)
        // For Android emulator, use 10.0.2.2 instead of localhost
        val actualIp = if (ip == "localhost") "10.0.2.2" else ip
        return context.getString(R.string.api_base_url, actualIp)
    }
    
    /**
     * Initialize Retrofit with base URL from strings.xml
     */
    private fun getRetrofit(context: Context): Retrofit {
        if (retrofitInstance == null) {
            val baseUrl = getBaseUrl(context)
            retrofitInstance = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofitInstance!!
    }
    
    /**
     * Get PHP API service instance
     * Requires context to read base URL from strings.xml
     */
    fun getPhpApiService(context: Context): PhpApiService {
        if (phpApiServiceInstance == null) {
            phpApiServiceInstance = getRetrofit(context).create(PhpApiService::class.java)
        }
        return phpApiServiceInstance!!
    }
    
    /**
     * Normalize image URL to use the correct IP address from strings.xml
     * Replaces localhost with the configured IP address
     */
    fun normalizeImageUrl(context: Context, imageUrl: String?): String? {
        if (imageUrl.isNullOrEmpty()) return null
        
        val ip = context.getString(R.string.api_ip)
        val actualIp = if (ip == "localhost") "10.0.2.2" else ip
        
        // Check if URL already uses the correct IP
        if (imageUrl.contains("://$actualIp/") || imageUrl.contains("://$ip/")) {
            return imageUrl
        }
        
        // Replace localhost/127.0.0.1 with the actual IP
        var normalized = imageUrl
            .replace("http://localhost/", "http://$actualIp/")
            .replace("https://localhost/", "https://$actualIp/")
            .replace("http://127.0.0.1/", "http://$actualIp/")
            .replace("https://127.0.0.1/", "https://$actualIp/")
        
        // Also handle URLs without trailing slash
        normalized = normalized
            .replace("http://localhost:", "http://$actualIp:")
            .replace("https://localhost:", "https://$actualIp:")
            .replace("http://127.0.0.1:", "http://$actualIp:")
            .replace("https://127.0.0.1:", "https://$actualIp:")
        
        return normalized
    }
    
    /**
     * Reset API client (useful for testing or when IP changes)
     */
    fun reset() {
        retrofitInstance = null
        phpApiServiceInstance = null
    }
    
    // Legacy property for backward compatibility
    // Note: This requires context, so use getPhpApiService(context) instead
    @Deprecated("Use getPhpApiService(context) instead", ReplaceWith("getPhpApiService(context)"))
    val phpApiService: PhpApiService
        get() = throw IllegalStateException("Use getPhpApiService(context) instead")
}
