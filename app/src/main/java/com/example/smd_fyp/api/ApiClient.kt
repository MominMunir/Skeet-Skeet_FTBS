package com.example.smd_fyp.api

import android.content.Context
import com.example.smd_fyp.R
import com.example.smd_fyp.model.GroundApi
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
     * Replaces any IP address (including old IPs) with the current configured IP address
     * This ensures that when network changes, all image URLs are updated automatically
     */
    fun normalizeImageUrl(context: Context, imageUrl: String?): String? {
        if (imageUrl.isNullOrEmpty()) return null
        
        val ip = context.getString(R.string.api_ip)
        val actualIp = if (ip == "localhost") "10.0.2.2" else ip
        
        // Check if URL already uses the correct IP
        if (imageUrl.contains("://$actualIp/") || imageUrl.contains("://$actualIp:")) {
            return imageUrl
        }
        
        // Pattern to match IP addresses (IPv4)
        // Matches: http://192.168.0.192/... or http://172.15.66.69/... etc.
        val ipPattern = Regex("""(http[s]?://)(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})([/:])""")
        
        // Replace any IP address with the current IP
        val normalized = ipPattern.replace(imageUrl) { matchResult ->
            val protocol = matchResult.groupValues[1] // http:// or https://
            val separator = matchResult.groupValues[3] // / or :
            "$protocol$actualIp$separator"
        }
        
        // Also handle localhost/127.0.0.1 replacements (for backward compatibility)
        var finalUrl = normalized
            .replace("http://localhost/", "http://$actualIp/")
            .replace("https://localhost/", "https://$actualIp/")
            .replace("http://127.0.0.1/", "http://$actualIp/")
            .replace("https://127.0.0.1/", "https://$actualIp/")
            .replace("http://localhost:", "http://$actualIp:")
            .replace("https://localhost:", "https://$actualIp:")
            .replace("http://127.0.0.1:", "http://$actualIp:")
            .replace("https://127.0.0.1:", "https://$actualIp:")
        
        return finalUrl
    }
    
    /**
     * Normalize a GroundApi object by updating its imageUrl with the current IP address
     * This should be called whenever grounds are loaded from the database
     */
    fun normalizeGroundImageUrl(context: Context, ground: GroundApi): GroundApi {
        val normalizedImageUrl = normalizeImageUrl(context, ground.imageUrl)
        return ground.copy(imageUrl = normalizedImageUrl)
    }
    
    /**
     * Normalize a list of GroundApi objects by updating their imageUrls with the current IP address
     * This should be called whenever grounds are loaded from the database
     */
    fun normalizeGroundImageUrls(context: Context, grounds: List<GroundApi>): List<GroundApi> {
        return grounds.map { normalizeGroundImageUrl(context, it) }
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
