package com.example.smd_fyp.utils

import com.example.smd_fyp.api.OpenMeteoForecastResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Helper class to calculate ground condition based on weather data
 */
object GroundConditionHelper {
    
    enum class GroundCondition {
        EXCELLENT,  // Perfect weather for playing
        GOOD,       // Good conditions, minor concerns
        MODERATE,   // Some concerns, playable but not ideal
        POOR        // Poor conditions, not recommended
    }
    
    /**
     * Calculate ground condition based on weather forecast for today
     */
    fun calculateCondition(forecast: OpenMeteoForecastResponse?): GroundCondition {
        if (forecast == null || forecast.daily == null) {
            return GroundCondition.GOOD // Default if no weather data
        }
        
        val daily = forecast.daily
        val times = daily.time ?: return GroundCondition.GOOD
        val codes = daily.weathercode ?: return GroundCondition.GOOD
        val tempMax = daily.temperature_2m_max ?: return GroundCondition.GOOD
        val tempMin = daily.temperature_2m_min ?: return GroundCondition.GOOD
        val precipProb = daily.precipitation_probability_max ?: return GroundCondition.GOOD
        val precipSum = daily.precipitation_sum ?: return GroundCondition.GOOD
        
        if (times.isEmpty() || codes.isEmpty()) {
            return GroundCondition.GOOD
        }
        
        // Get today's date
        val today = Calendar.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.time)
        
        // Find today's index
        val todayIndex = times.indexOfFirst { it.startsWith(todayStr) }
        if (todayIndex == -1 || todayIndex >= codes.size) {
            return GroundCondition.GOOD
        }
        
        val code = codes[todayIndex]
        val rainProb = precipProb.getOrNull(todayIndex) ?: 0
        val rainSum = precipSum.getOrNull(todayIndex) ?: 0.0
        val maxTemp = tempMax.getOrNull(todayIndex) ?: 25.0
        val minTemp = tempMin.getOrNull(todayIndex) ?: 15.0
        
        // Calculate condition based on multiple factors
        var score = 100 // Start with perfect score
        
        // Rain factor (most important)
        if (isRainCode(code) || rainProb >= 50 || rainSum > 5.0) {
            score -= 60 // Heavy penalty for rain
        } else if (rainProb >= 30 || rainSum > 2.0) {
            score -= 30 // Moderate penalty
        } else if (rainProb >= 15) {
            score -= 10 // Light penalty
        }
        
        // Temperature factor
        if (maxTemp > 40 || minTemp < 0) {
            score -= 30 // Extreme temperatures
        } else if (maxTemp > 35 || minTemp < 5) {
            score -= 15 // Uncomfortable temperatures
        } else if (maxTemp > 30 || minTemp < 10) {
            score -= 5 // Slightly uncomfortable
        }
        
        // Determine condition based on score
        return when {
            score >= 85 -> GroundCondition.EXCELLENT
            score >= 70 -> GroundCondition.GOOD
            score >= 50 -> GroundCondition.MODERATE
            else -> GroundCondition.POOR
        }
    }
    
    /**
     * Get condition text for display
     */
    fun getConditionText(condition: GroundCondition): String {
        return when (condition) {
            GroundCondition.EXCELLENT -> "Excellent"
            GroundCondition.GOOD -> "Good"
            GroundCondition.MODERATE -> "Moderate"
            GroundCondition.POOR -> "Poor"
        }
    }
    
    /**
     * Get condition color resource
     */
    fun getConditionColorRes(condition: GroundCondition): Int {
        return when (condition) {
            GroundCondition.EXCELLENT -> android.R.color.holo_green_dark
            GroundCondition.GOOD -> android.R.color.holo_green_light
            GroundCondition.MODERATE -> android.R.color.holo_orange_light
            GroundCondition.POOR -> android.R.color.holo_red_dark
        }
    }
    
    /**
     * Get condition icon resource
     */
    fun getConditionIconRes(condition: GroundCondition): Int {
        return when (condition) {
            GroundCondition.EXCELLENT -> com.example.smd_fyp.R.drawable.ic_star_24
            GroundCondition.GOOD -> com.example.smd_fyp.R.drawable.ic_star_24
            GroundCondition.MODERATE -> com.example.smd_fyp.R.drawable.ic_notifications
            GroundCondition.POOR -> com.example.smd_fyp.R.drawable.ic_notifications
        }
    }
    
    private fun isRainCode(code: Int): Boolean {
        // WMO Weather interpretation codes for rain
        // 51-67: Drizzle and rain
        // 80-82: Rain showers
        return (code in 51..67) || (code in 80..82)
    }
}
