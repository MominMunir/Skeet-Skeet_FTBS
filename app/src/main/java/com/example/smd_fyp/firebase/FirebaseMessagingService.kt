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
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            sendNotification(it.title ?: "New Notification", it.body ?: "")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to your server or save locally
        // You can use SharedPreferences or send to your backend
        saveTokenToPreferences(token)
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "New Notification"
        val body = data["body"] ?: ""
        val type = data["type"] ?: "general"
        
        // Handle different notification types
        when (type) {
            "booking" -> {
                // Handle booking notification
                sendNotification(title, body)
            }
            "ground" -> {
                // Handle ground notification
                sendNotification(title, body)
            }
            else -> {
                sendNotification(title, body)
            }
        }
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        
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
