package com.example.smd_fyp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Booking")
data class Booking(
    @PrimaryKey
    val id: String = "",
    val userId: String = "",
    val groundId: String = "",
    val groundName: String = "",
    val date: String = "",
    val time: String = "",
    val duration: Int = 1, // hours
    val totalPrice: Double = 0.0,
    val status: BookingStatus = BookingStatus.PENDING,
    val paymentId: String? = null,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false // For offline sync
)

enum class BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}

enum class PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}
