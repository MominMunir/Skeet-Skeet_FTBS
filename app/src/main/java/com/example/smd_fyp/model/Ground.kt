package com.example.smd_fyp.model

data class Ground(
    val name: String,
    val location: String,
    val priceText: String,
    val ratingText: String,
    val imageResId: Int,
    val hasFloodlights: Boolean = true,
    val hasParking: Boolean = true
)
