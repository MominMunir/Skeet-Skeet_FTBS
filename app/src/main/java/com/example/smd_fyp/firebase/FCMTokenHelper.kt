package com.example.smd_fyp.firebase

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FCMTokenHelper {
    /**
     * Get FCM token for push notifications
     * Call this when user logs in to register their device token
     */
    suspend fun getFCMToken(): Result<String> {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save FCM token to SharedPreferences
     */
    fun saveTokenToPreferences(context: Context, token: String) {
        val prefs = context.getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }

    /**
     * Get saved FCM token from SharedPreferences
     */
    fun getSavedToken(context: Context): String? {
        val prefs = context.getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
        return prefs.getString("fcm_token", null)
    }

    /**
     * Delete FCM token (e.g., on logout)
     */
    fun deleteToken(context: Context) {
        val prefs = context.getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("fcm_token").apply()
    }
}
