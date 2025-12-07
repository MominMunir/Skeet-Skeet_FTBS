package com.example.smd_fyp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "User")
data class User(
    @PrimaryKey
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: UserRole = UserRole.PLAYER,
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false // For offline sync
)

enum class UserRole {
    PLAYER,
    GROUNDKEEPER,
    ADMIN
}
