package com.example.smd_fyp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Helper class to open locations in Google Maps
 */
object MapHelper {
    
    /**
     * Open location in Google Maps
     * @param context Android context
     * @param location Location string (e.g., "DHA Phase 5, Lahore" or coordinates "33.7215,73.0433")
     * @param label Optional label for the location (e.g., ground name)
     */
    fun openInGoogleMaps(context: Context, location: String, label: String? = null) {
        try {
            // Always search by place name for accurate results
            // Build the search query - combine label and location for better search results
            val searchQuery = if (label != null && label.isNotEmpty()) {
                "${Uri.encode(label)}, ${Uri.encode(location)}"
            } else {
                Uri.encode(location)
            }
            
            // Method 1: Try generic geo intent with search query (works with any map app including Google Maps)
            // Use geo:0,0?q= to search for the place name
            val geoUri = Uri.parse("geo:0,0?q=$searchQuery")
            
            try {
                val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)
                context.startActivity(geoIntent)
                Log.d("MapHelper", "Opened maps app with geo URI search: $searchQuery")
                return
            } catch (e: android.content.ActivityNotFoundException) {
                Log.w("MapHelper", "Geo URI failed: ${e.message}, trying web URL")
            }
            
            // Method 2: Try Google Maps web URL with search query (works in app or browser)
            val mapsWebUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$searchQuery")
            
            // Try with Google Maps package first
            try {
                val googleMapsIntent = Intent(Intent.ACTION_VIEW, mapsWebUri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                context.startActivity(googleMapsIntent)
                Log.d("MapHelper", "Opened Google Maps app with search query: $searchQuery")
                return
            } catch (e: android.content.ActivityNotFoundException) {
                Log.w("MapHelper", "Google Maps app not found, trying generic web intent")
            }
            
            // Method 3: Try generic web URL intent (will open in browser or any app that handles it)
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, mapsWebUri)
                context.startActivity(webIntent)
                Log.d("MapHelper", "Opened Google Maps in browser/app with search query: $searchQuery")
                return
            } catch (e: android.content.ActivityNotFoundException) {
                Log.w("MapHelper", "No app found for web URL: ${e.message}")
            }
            
            // If all methods fail, show error
            Log.e("MapHelper", "No app available to open maps. Location: $location")
            android.widget.Toast.makeText(context, "Please install Google Maps or a web browser", android.widget.Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            Log.e("MapHelper", "Error opening maps: ${e.message}", e)
            android.widget.Toast.makeText(context, "Error opening maps: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Parse coordinates from string
     * Supports formats: "33.7215,73.0433" or "33.7215, 73.0433"
     */
    private fun parseCoordinates(location: String): Pair<Double, Double>? {
        return try {
            val parts = location.split(",").map { it.trim() }
            if (parts.size == 2) {
                val lat = parts[0].toDouble()
                val lng = parts[1].toDouble()
                // Validate coordinates are in valid ranges
                if (lat in -90.0..90.0 && lng in -180.0..180.0) {
                    Pair(lat, lng)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get coordinates for common Pakistani cities
     * This can be used as a fallback if location string matches known cities
     */
    fun getCityCoordinates(city: String): Pair<Double, Double>? {
        val normalized = city.lowercase()
        return when {
            normalized.contains("islamabad") -> Pair(33.7215, 73.0433)
            normalized.contains("lahore") -> Pair(31.558, 74.3507)
            normalized.contains("karachi") -> Pair(24.8608, 67.0104)
            normalized.contains("rawalpindi") -> Pair(33.5651, 73.0169)
            normalized.contains("faisalabad") -> Pair(31.4504, 73.1350)
            normalized.contains("multan") -> Pair(30.1575, 71.5249)
            normalized.contains("peshawar") -> Pair(34.0151, 71.5249)
            normalized.contains("quetta") -> Pair(30.1798, 66.9750)
            normalized.contains("sialkot") -> Pair(32.4945, 74.5229)
            normalized.contains("gujranwala") -> Pair(32.1617, 74.1883)
            else -> null
        }
    }
}

