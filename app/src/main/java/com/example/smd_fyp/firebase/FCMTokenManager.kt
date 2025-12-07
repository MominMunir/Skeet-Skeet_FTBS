package com.example.smd_fyp.firebase

import android.content.Context
import com.example.smd_fyp.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FCMTokenManager {
    /**
     * Register FCM token with the server
     */
    suspend fun registerToken(context: Context, userId: String, token: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val apiService = ApiClient.getPhpApiService(context)
                val response = apiService.registerFCMToken(
                    mapOf(
                        "userId" to userId,
                        "token" to token,
                        "platform" to "android"
                    )
                )
                
                if (response.isSuccessful) {
                    android.util.Log.d("FCMTokenManager", "Successfully registered FCM token for user: $userId")
                    Result.success(Unit)
                } else {
                    val errorBody = try {
                        response.errorBody()?.string() ?: "Unknown error"
                    } catch (e: Exception) {
                        "Could not read error body: ${e.message}"
                    }
                    android.util.Log.e("FCMTokenManager", "Failed to register token: ${response.code()} - $errorBody")
                    Result.failure(Exception("Failed to register token: ${response.code()}"))
                }
            } catch (e: Exception) {
                android.util.Log.e("FCMTokenManager", "Error registering FCM token: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}
