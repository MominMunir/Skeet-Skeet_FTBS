package com.example.smd_fyp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Review")
data class Review(
    @PrimaryKey
    val id: String = "",
    val userId: String = "",
    val groundId: String = "",
    val bookingId: String = "",
    val rating: Float = 0f,
    val reviewText: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
