package com.example.smd_fyp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "GroundApi")
data class GroundApi(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val price: Double = 0.0,
    val priceText: String = "",
    val rating: Double = 0.0,
    val ratingText: String = "",
    val imageUrl: String? = null,
    val imagePath: String? = null, // For PHP API
    val hasFloodlights: Boolean = true,
    val hasParking: Boolean = true,
    val description: String? = null,
    val available: Boolean = true,
    val synced: Boolean = false // For offline sync
)
