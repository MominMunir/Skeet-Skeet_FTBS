package com.example.smd_fyp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Favorite")
data class Favorite(
    @PrimaryKey
    val id: String = "", // Composite: userId_groundId
    val userId: String = "",
    val groundId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
) {
    companion object {
        fun createId(userId: String, groundId: String): String {
            return "${userId}_${groundId}"
        }
    }
}
