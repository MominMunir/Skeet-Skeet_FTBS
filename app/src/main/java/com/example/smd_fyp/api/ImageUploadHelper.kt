package com.example.smd_fyp.api

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.smd_fyp.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageUploadHelper {
    
    /**
     * Get local images directory
     */
    private fun getLocalImagesDir(context: Context): File {
        val imagesDir = File(context.filesDir, "local_images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        return imagesDir
    }
    
    /**
     * Save image locally for offline storage
     * @return Local file path or null if failed
     */
    private suspend fun saveImageLocally(
        context: Context,
        imageUri: Uri,
        folder: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext null
            
            val bytes = inputStream.readBytes()
            inputStream.close()
            
            // Determine image type
            val imageType = when {
                bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "jpg"
                bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && 
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "png"
                else -> "jpg"
            }
            
            // Create folder-specific directory
            val folderDir = File(getLocalImagesDir(context), folder)
            if (!folderDir.exists()) {
                folderDir.mkdirs()
            }
            
            // Generate unique filename
            val fileName = "${UUID.randomUUID()}.$imageType"
            val localFile = File(folderDir, fileName)
            
            // Save to local storage
            FileOutputStream(localFile).use { it.write(bytes) }
            
            // Return local path (will be used to identify the image for later upload)
            return@withContext "local://$folder/$fileName"
        } catch (e: Exception) {
            android.util.Log.e("ImageUploadHelper", "Error saving image locally: ${e.message}", e)
            null
        }
    }
    
    /**
     * Upload locally stored image when online
     */
    suspend fun uploadLocalImage(
        context: Context,
        localPath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Parse local path: "local://grounds/filename.jpg"
            if (!localPath.startsWith("local://")) {
                return@withContext Result.failure(Exception("Invalid local path format"))
            }
            
            val pathParts = localPath.removePrefix("local://").split("/")
            if (pathParts.size != 2) {
                return@withContext Result.failure(Exception("Invalid local path format"))
            }
            
            val folder = pathParts[0]
            val fileName = pathParts[1]
            
            // Read local file
            val localFile = File(getLocalImagesDir(context), "$folder/$fileName")
            if (!localFile.exists()) {
                return@withContext Result.failure(Exception("Local image file not found"))
            }
            
            val bytes = localFile.readBytes()
            val imageType = fileName.substringAfterLast(".", "jpg")
            
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
                // Delete local file after successful upload
                localFile.delete()
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
     * Upload image to PHP API using base64 encoding (more reliable for Android)
     * If offline, saves image locally and returns local path
     * @param context Android context
     * @param imageUri URI of the image to upload
     * @param folder Folder name in server (e.g., "grounds", "users", "bookings")
     * @return Result containing the image URL or local path (if offline)
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
            
            // Check if online
            if (!SyncManager.isOnline(context)) {
                // Offline: Save locally and return local path
                val localPath = saveImageLocally(context, imageUri, folder)
                if (localPath != null) {
                    android.util.Log.d("ImageUploadHelper", "Image saved locally: $localPath")
                    return@withContext Result.success(localPath)
                } else {
                    return@withContext Result.failure(Exception("Failed to save image locally"))
                }
            }
            
            // Online: Try to upload
            try {
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
                    // Upload failed, save locally as fallback
                    val localPath = saveImageLocally(context, imageUri, folder)
                    if (localPath != null) {
                        android.util.Log.d("ImageUploadHelper", "Upload failed, saved locally: $localPath")
                        Result.success(localPath)
                    } else {
                        val errorMsg = response.body()?.message ?: response.message() ?: "Upload failed"
                        Result.failure(Exception(errorMsg))
                    }
                }
            } catch (e: Exception) {
                // Network error, save locally
                val localPath = saveImageLocally(context, imageUri, folder)
                if (localPath != null) {
                    android.util.Log.d("ImageUploadHelper", "Network error, saved locally: $localPath")
                    Result.success(localPath)
                } else {
                    Result.failure(e)
                }
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
