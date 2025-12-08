package com.example.smd_fyp.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smd_fyp.MainActivity
import com.example.smd_fyp.R
import com.example.smd_fyp.api.OpenMeteoService
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.model.Notification
import com.example.smd_fyp.model.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * Worker that checks weather daily and sends notifications for rainy days
 * Runs once per day to check tomorrow's weather
 */
class WeatherNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val weatherService = OpenMeteoService.create()
    
    // City coordinates mapping
    private val cityCoordinates = mapOf(
        "Islamabad" to Pair(33.7215, 73.0433),
        "Lahore" to Pair(31.558, 74.3507),
        "Karachi" to Pair(24.8608, 67.0104),
        "G-13, Islamabad" to Pair(33.7215, 73.0433),
        "DHA Phase ll, Islamabad" to Pair(33.7215, 73.0433),
        "DHA Phase II, Islamabad" to Pair(33.7215, 73.0433)
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("WeatherNotificationWorker", "Starting weather check...")
            
            // Initialize database
            LocalDatabaseHelper.initialize(applicationContext)
            
            // Check if weather alerts are enabled
            val prefs = applicationContext.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
            val weatherAlertsEnabled = prefs.getBoolean("weather_alerts", true)
            
            if (!weatherAlertsEnabled) {
                android.util.Log.d("WeatherNotificationWorker", "Weather alerts are disabled by user")
                return@withContext Result.success()
            }
            
            // Get all users who have weather alerts enabled
            val allUsers = LocalDatabaseHelper.getAllUsersSync()
            val usersWithAlerts = allUsers.filter { user ->
                // Check if user has weather alerts enabled
                // For now, we'll check the global preference, but ideally each user should have their own preference
                // Since we're using SharedPreferences which is app-wide, we'll send to all users if enabled
                true // Will be filtered by the preference check above
            }
            
            if (usersWithAlerts.isEmpty()) {
                android.util.Log.d("WeatherNotificationWorker", "No users with weather alerts enabled")
                return@withContext Result.success()
            }
            
            // Get all grounds to find unique cities
            val allGrounds = LocalDatabaseHelper.getAllGrounds()?.first() ?: emptyList()
            val uniqueCities = allGrounds.mapNotNull { ground ->
                ground.location?.trim()?.takeIf { it.isNotEmpty() }
            }.distinct()
            
            if (uniqueCities.isEmpty()) {
                android.util.Log.d("WeatherNotificationWorker", "No grounds found, skipping weather check")
                return@withContext Result.success()
            }
            
            android.util.Log.d("WeatherNotificationWorker", "Checking weather for ${uniqueCities.size} cities: $uniqueCities")
            
            // Check weather for each unique city
            val rainyCities = mutableListOf<String>()
            
            for (city in uniqueCities) {
                val coords = getCityCoordinates(city)
                if (coords != null) {
                    try {
                        val forecast = weatherService.getDailyForecast(coords.first, coords.second, forecastDays = 2)
                        val isRainy = checkIfRainyTomorrow(forecast)
                        
                        if (isRainy) {
                            rainyCities.add(city)
                            android.util.Log.d("WeatherNotificationWorker", "Rain forecast for $city")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("WeatherNotificationWorker", "Error checking weather for $city", e)
                    }
                } else {
                    android.util.Log.w("WeatherNotificationWorker", "Could not find coordinates for city: $city")
                }
            }
            
            // Send notification to all users with weather alerts enabled if any cities have rain forecast
            if (rainyCities.isNotEmpty()) {
                for (user in usersWithAlerts) {
                    sendWeatherNotification(user.id, rainyCities)
                }
                android.util.Log.d("WeatherNotificationWorker", "Sent weather notifications to ${usersWithAlerts.size} users")
            } else {
                android.util.Log.d("WeatherNotificationWorker", "No rain forecast for any city")
            }
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("WeatherNotificationWorker", "Error in weather notification worker", e)
            Result.retry()
        }
    }
    
    private fun getCityCoordinates(city: String): Pair<Double, Double>? {
        // Try exact match first
        cityCoordinates[city]?.let { return it }
        
        // Try partial match
        for ((key, coords) in cityCoordinates) {
            if (city.contains(key, ignoreCase = true) || key.contains(city, ignoreCase = true)) {
                return coords
            }
        }
        
        // Default mappings based on common patterns
        return when {
            city.contains("Islamabad", ignoreCase = true) -> Pair(33.7215, 73.0433)
            city.contains("Lahore", ignoreCase = true) -> Pair(31.558, 74.3507)
            city.contains("Karachi", ignoreCase = true) -> Pair(24.8608, 67.0104)
            else -> null
        }
    }
    
    private fun checkIfRainyTomorrow(forecast: com.example.smd_fyp.api.OpenMeteoForecastResponse): Boolean {
        val daily = forecast.daily ?: return false
        val times = daily.time ?: return false
        val codes = daily.weathercode ?: return false
        val precipProb = daily.precipitation_probability_max ?: return false
        
        if (times.isEmpty() || codes.isEmpty()) return false
        
        // Get tomorrow's date
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tomorrow.time)
        
        // Find tomorrow's index
        val tomorrowIndex = times.indexOfFirst { it.startsWith(tomorrowStr) }
        if (tomorrowIndex == -1 || tomorrowIndex >= codes.size) return false
        
        val code = codes[tomorrowIndex]
        val rainProb = precipProb.getOrNull(tomorrowIndex) ?: 0
        
        // Check if it's rainy: weather code indicates rain OR precipitation probability >= 30%
        return isRainCode(code) || rainProb >= 30
    }
    
    private fun isRainCode(code: Int): Boolean {
        // WMO Weather interpretation codes for rain
        // 51-67: Drizzle and rain
        // 80-82: Rain showers
        return (code in 51..67) || (code in 80..82)
    }
    
    private suspend fun sendWeatherNotification(userId: String, rainyCities: List<String>) {
        try {
            val cityList = if (rainyCities.size == 1) {
                rainyCities[0]
            } else if (rainyCities.size <= 3) {
                rainyCities.joinToString(", ")
            } else {
                "${rainyCities.take(2).joinToString(", ")} and ${rainyCities.size - 2} more"
            }
            
            val title = "🌧️ Rain Forecast Alert"
            val message = if (rainyCities.size == 1) {
                "Rain is forecast for tomorrow in $cityList. Consider rescheduling your booking!"
            } else {
                "Rain is forecast for tomorrow in $cityList. Check weather before your bookings!"
            }
            
            // Create notification in database
            val notification = Notification(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = title,
                message = message,
                type = NotificationType.WEATHER,
                relatedId = null,
                isRead = false,
                createdAt = System.currentTimeMillis(),
                synced = false
            )
            
            // Save to local database
            LocalDatabaseHelper.saveNotification(notification)
            
            // Sync to server if online
            if (SyncManager.isOnline(applicationContext)) {
                SyncManager.syncNotification(applicationContext, notification)
            }
            
            // Show system notification (on main thread)
            withContext(Dispatchers.Main) {
                showSystemNotification(title, message)
            }
            
            android.util.Log.d("WeatherNotificationWorker", "Weather notification sent for cities: $rainyCities")
        } catch (e: Exception) {
            android.util.Log.e("WeatherNotificationWorker", "Error sending weather notification", e)
        }
    }
    
    private fun showSystemNotification(title: String, message: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val channelId = applicationContext.getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Weather Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for weather alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
