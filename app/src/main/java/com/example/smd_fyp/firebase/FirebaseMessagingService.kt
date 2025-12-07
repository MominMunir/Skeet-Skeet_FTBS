package com.example.smd_fyp.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.smd_fyp.MainActivity
import com.example.smd_fyp.R
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.model.Notification
import com.example.smd_fyp.model.NotificationType
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Initialize database
        LocalDatabaseHelper.initialize(this)

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            handleDataMessage(remoteMessage.data)
        } else {
            // Check if message contains notification payload
            remoteMessage.notification?.let {
                handleNotificationPayload(it.title ?: "New Notification", it.body ?: "", remoteMessage.data)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save token locally
        saveTokenToPreferences(token)
        
        // Sync token to server (will be handled by token registration on login)
        android.util.Log.d("FCM", "New FCM token: $token")
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "New Notification"
        val body = data["body"] ?: data["message"] ?: ""
        val type = data["type"] ?: "SYSTEM"
        val userId = data["userId"] ?: ""
        val relatedId = data["relatedId"]
        
        // Save notification to database
        serviceScope.launch {
            try {
                val notification = Notification(
                    id = data["id"] ?: UUID.randomUUID().toString(),
                    userId = userId,
                    title = title,
                    message = body,
                    type = parseNotificationType(type),
                    relatedId = relatedId,
                    isRead = false,
                    createdAt = System.currentTimeMillis(),
                    synced = false
                )
                
                LocalDatabaseHelper.saveNotification(notification)
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Error saving notification: ${e.message}", e)
            }
        }
        
        // Show system notification
        sendNotification(title, body, data)
    }
    
    private fun handleNotificationPayload(title: String, body: String, data: Map<String, String>) {
        val userId = data["userId"] ?: ""
        val type = data["type"] ?: "SYSTEM"
        val relatedId = data["relatedId"]
        
        // Save notification to database
        serviceScope.launch {
            try {
                val notification = Notification(
                    id = data["id"] ?: UUID.randomUUID().toString(),
                    userId = userId,
                    title = title,
                    message = body,
                    type = parseNotificationType(type),
                    relatedId = relatedId,
                    isRead = false,
                    createdAt = System.currentTimeMillis(),
                    synced = false
                )
                
                LocalDatabaseHelper.saveNotification(notification)
            } catch (e: Exception) {
                android.util.Log.e("FCM", "Error saving notification: ${e.message}", e)
            }
        }
        
        // Show system notification
        sendNotification(title, body, data)
    }
    
    private fun parseNotificationType(type: String): NotificationType {
        return try {
            NotificationType.valueOf(type.uppercase())
        } catch (e: Exception) {
            NotificationType.SYSTEM
        }
    }

    private fun sendNotification(title: String, messageBody: String, data: Map<String, String> = emptyMap()) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        
        // Add notification data to intent
        data.forEach { (key, value) ->
            intent.putExtra(key, value)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SkeetSkeet Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for SkeetSkeet app"
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun saveTokenToPreferences(token: String) {
        val prefs = getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }

    companion object {
        fun getFCMToken(context: Context): String? {
            val prefs = context.getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
            return prefs.getString("fcm_token", null)
        }
    }
}
