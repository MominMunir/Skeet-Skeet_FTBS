package com.example.smd_fyp.api

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUploadHelper {
    
    /**
     * Upload image to PHP API using base64 encoding (more reliable for Android)
     * @param context Android context
     * @param imageUri URI of the image to upload
     * @param folder Folder name in server (e.g., "grounds", "users", "bookings")
     * @return Result containing the image URL or path
     */
    suspend fun uploadImage(
        context: Context,
        imageUri: Uri,
        folder: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Read image from URI
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("Cannot open image file"))
            
            // Read bytes
            val bytes = inputStream.readBytes()
            inputStream.close()
            
            // Determine image type
            val imageType = when {
                bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "jpg"
                bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && 
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "png"
                else -> "jpg" // Default to jpg
            }
            
            // Convert to base64
            val base64Image = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            
            // Upload using base64 API
            val apiService = ApiClient.getPhpApiService(context)
            val response = apiService.uploadBase64Image(
                base64Image = base64Image,
                folder = folder,
                imageType = imageType
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val url = response.body()?.url ?: response.body()?.path ?: ""
                Result.success(url)
            } else {
                val errorMsg = response.body()?.message ?: response.message() ?: "Upload failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get image URL from PHP API
     */
    suspend fun getImageUrl(context: Context, imagePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiService = ApiClient.getPhpApiService(context)
            val response = apiService.getImageUrl(imagePath)
            if (response.isSuccessful && response.body()?.success == true) {
                val url = response.body()?.url ?: imagePath
                Result.success(url)
            } else {
                Result.failure(Exception("Failed to get image URL"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
    }
}
