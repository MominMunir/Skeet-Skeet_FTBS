package com.example.smd_fyp.utils

import com.example.smd_fyp.model.GroundApi

object SearchFilterHelper {
    
    /**
     * Search and filter grounds
     */
    fun filterGrounds(
        grounds: List<GroundApi>,
        searchQuery: String? = null,
        location: String? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        minRating: Double? = null,
        hasFloodlights: Boolean? = null,
        hasParking: Boolean? = null,
        availableOnly: Boolean = true
    ): List<GroundApi> {
        return grounds.filter { ground ->
            // Search query filter
            val matchesSearch = searchQuery.isNullOrBlank() || 
                ground.name.contains(searchQuery, ignoreCase = true) ||
                ground.location.contains(searchQuery, ignoreCase = true) ||
                ground.description?.contains(searchQuery, ignoreCase = true) == true
            
            // Location filter
            val matchesLocation = location.isNullOrBlank() || 
                ground.location.contains(location, ignoreCase = true)
            
            // Price filter
            val matchesPrice = (minPrice == null || ground.price >= minPrice) &&
                (maxPrice == null || ground.price <= maxPrice)
            
            // Rating filter
            val matchesRating = minRating == null || ground.rating >= minRating
            
            // Amenities filter
            val matchesFloodlights = hasFloodlights == null || ground.hasFloodlights == hasFloodlights
            val matchesParking = hasParking == null || ground.hasParking == hasParking
            
            // Availability filter
            val matchesAvailability = !availableOnly || ground.available
            
            matchesSearch && matchesLocation && matchesPrice && 
            matchesRating && matchesFloodlights && matchesParking && matchesAvailability
        }
    }
    
    /**
     * Sort grounds
     */
    fun sortGrounds(
        grounds: List<GroundApi>,
        sortBy: SortOption = SortOption.NAME
    ): List<GroundApi> {
        return when (sortBy) {
            SortOption.NAME -> grounds.sortedBy { it.name }
            SortOption.PRICE_LOW_TO_HIGH -> grounds.sortedBy { it.price }
            SortOption.PRICE_HIGH_TO_LOW -> grounds.sortedByDescending { it.price }
            SortOption.RATING_HIGH_TO_LOW -> grounds.sortedByDescending { it.rating }
            SortOption.RATING_LOW_TO_HIGH -> grounds.sortedBy { it.rating }
            SortOption.LOCATION -> grounds.sortedBy { it.location }
        }
    }
}

enum class SortOption {
    NAME,
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    RATING_HIGH_TO_LOW,
    RATING_LOW_TO_HIGH,
    LOCATION
}
