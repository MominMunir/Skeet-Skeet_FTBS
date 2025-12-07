package com.example.smd_fyp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Notification")
data class Notification(
    @PrimaryKey
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val relatedId: String? = null, // bookingId, groundId, etc.
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
) {
    // Helper function to format time
    fun getFormattedTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - createdAt
        
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000} minutes ago"
            diff < 86400000 -> "${diff / 3600000} hours ago"
            diff < 604800000 -> "${diff / 86400000} days ago"
            else -> "${diff / 604800000} weeks ago"
        }
    }
}

