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
     * Upload image to PHP API
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
            // Create a temporary file from URI
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(Exception("Cannot open image file"))
            
            val fileName = getFileName(context, imageUri) ?: "image_${System.currentTimeMillis()}.jpg"
            val tempFile = File(context.cacheDir, fileName)
            
            // Copy input stream to file
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            
            // Create multipart request
            val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", fileName, requestFile)
            val folderBody = folder.toRequestBody("text/plain".toMediaTypeOrNull())
            
            // Upload to PHP API
            val apiService = ApiClient.getPhpApiService(context)
            val response = apiService.uploadImage(folderBody, body)
            
            // Clean up temp file
            tempFile.delete()
            
            if (response.isSuccessful && response.body()?.success == true) {
                val url = response.body()?.url ?: response.body()?.path ?: ""
                Result.success(url)
            } else {
                Result.failure(Exception(response.message() ?: "Upload failed"))
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
