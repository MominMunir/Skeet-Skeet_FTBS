package com.example.smd_fyp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.example.smd_fyp.R
import com.example.smd_fyp.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit

object GlideHelper {
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    
    /**
     * Load image with proper error handling and logging
     */
    fun loadImage(
        context: Context,
        imageUrl: String?,
        imageView: ImageView,
        placeholder: Int = R.drawable.ic_person,
        errorDrawable: Int = R.drawable.ic_person,
        tag: String = "GlideHelper",
        useCircleCrop: Boolean = false
    ) {
        if (imageUrl.isNullOrEmpty()) {
            Log.w(tag, "Image URL is null or empty")
            imageView.setImageResource(errorDrawable)
            return
        }
        
        // Normalize URL to use correct IP
        val normalizedUrl = ApiClient.normalizeImageUrl(context, imageUrl)
        
        if (normalizedUrl.isNullOrEmpty()) {
            Log.w(tag, "Normalized URL is null or empty for: $imageUrl")
            imageView.setImageResource(errorDrawable)
            return
        }
        
        Log.d(tag, "Loading image: $normalizedUrl (original: $imageUrl)")
        
        // Set placeholder immediately
        imageView.setImageResource(placeholder)
        
        // Use OkHttp as primary method (similar to Volley approach that worked)
        // This is more reliable for local server images and is already working!
        loadImageWithOkHttp(context, normalizedUrl, imageView, errorDrawable, tag, useCircleCrop)
        
        // Also try Glide in parallel for caching benefits (non-blocking)
        try {
            val glideUrl = GlideUrl(
                normalizedUrl,
                LazyHeaders.Builder()
                    .addHeader("User-Agent", "SkeetSkeet-Android")
                    .build()
            )
            
            val requestBuilder = Glide.with(context)
                .load(glideUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(placeholder)
                .error(errorDrawable)
            
            if (useCircleCrop) {
                requestBuilder.circleCrop()
            } else {
                requestBuilder.centerCrop()
            }
            
            requestBuilder.into(imageView)
            Log.d(tag, "Glide load request also sent for caching: $normalizedUrl")
        } catch (e: Exception) {
            Log.w(tag, "Glide load failed (non-critical): ${e.message}")
            // OkHttp is already handling the image, so this is fine
        }
    }
    
    /**
     * Fallback method: Load image directly using OkHttp (similar to Volley approach)
     * This is used when Glide fails to load the image
     */
    private fun loadImageWithOkHttp(
        context: Context,
        imageUrl: String,
        imageView: ImageView,
        errorDrawable: Int,
        tag: String,
        useCircleCrop: Boolean
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(tag, "Fetching image with OkHttp from: $imageUrl")
                
                val request = Request.Builder()
                    .url(imageUrl)
                    .addHeader("User-Agent", "SkeetSkeet-Android")
                    .get()
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                Log.d(tag, "OkHttp response code: ${response.code}")
                Log.d(tag, "OkHttp response message: ${response.message}")
                
                if (response.isSuccessful && response.body != null) {
                    val inputStream: InputStream? = response.body?.byteStream()
                    if (inputStream != null) {
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()
                        
                        if (bitmap != null) {
                            Log.d(tag, "Image loaded successfully with OkHttp. Size: ${bitmap.width}x${bitmap.height}")
                            
                            withContext(Dispatchers.Main) {
                                if (useCircleCrop) {
                                    // Create circular bitmap
                                    val circularBitmap = createCircularBitmap(bitmap)
                                    imageView.setImageBitmap(circularBitmap)
                                } else {
                                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                                    imageView.setImageBitmap(bitmap)
                                }
                            }
                        } else {
                            Log.e(tag, "Failed to decode bitmap from OkHttp response")
                            withContext(Dispatchers.Main) {
                                imageView.setImageResource(errorDrawable)
                            }
                        }
                    } else {
                        Log.e(tag, "OkHttp response body is null")
                        withContext(Dispatchers.Main) {
                            imageView.setImageResource(errorDrawable)
                        }
                    }
                } else {
                    val errorBody = response.body?.string() ?: "No error body"
                    Log.e(tag, "OkHttp request failed: ${response.code} - ${response.message}")
                    Log.e(tag, "Response body: $errorBody")
                    withContext(Dispatchers.Main) {
                        imageView.setImageResource(errorDrawable)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(tag, "OkHttp exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    imageView.setImageResource(errorDrawable)
                }
            }
        }
    }
    
    /**
     * Create a circular bitmap from a square bitmap
     */
    private fun createCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
        }
        
        val rect = android.graphics.Rect(0, 0, size, size)
        val radius = size / 2f
        
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(radius, radius, radius, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        
        return output
    }
    
    /**
     * Test if an image URL is accessible (for debugging)
     */
    fun testImageUrl(context: Context, imageUrl: String?, tag: String = "GlideHelper"): Boolean {
        if (imageUrl.isNullOrEmpty()) {
            Log.w(tag, "Image URL is null or empty")
            return false
        }
        
        val normalizedUrl = ApiClient.normalizeImageUrl(context, imageUrl)
        if (normalizedUrl.isNullOrEmpty()) {
            Log.w(tag, "Normalized URL is null or empty")
            return false
        }
        
        return try {
            val request = Request.Builder()
                .url(normalizedUrl)
                .head() // Use HEAD request to check if resource exists
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            val isAccessible = response.isSuccessful
            Log.d(tag, "URL test result for $normalizedUrl: ${if (isAccessible) "ACCESSIBLE" else "NOT ACCESSIBLE"} (${response.code})")
            response.close()
            isAccessible
        } catch (e: Exception) {
            Log.e(tag, "Error testing URL: ${e.message}", e)
            false
        }
    }
}
