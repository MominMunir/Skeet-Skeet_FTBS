package com.example.smd_fyp.database

import androidx.room.TypeConverter
import com.example.smd_fyp.model.BookingStatus
import com.example.smd_fyp.model.NotificationType
import com.example.smd_fyp.model.PaymentStatus
import com.example.smd_fyp.model.UserRole

class Converters {
    @TypeConverter
    fun fromBookingStatus(status: BookingStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toBookingStatus(status: String): BookingStatus {
        return BookingStatus.valueOf(status)
    }
    
    @TypeConverter
    fun fromPaymentStatus(status: PaymentStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toPaymentStatus(status: String): PaymentStatus {
        return PaymentStatus.valueOf(status)
    }
    
    @TypeConverter
    fun fromUserRole(role: UserRole): String {
        return role.name
    }
    
    @TypeConverter
    fun toUserRole(role: String): UserRole {
        return UserRole.valueOf(role)
    }
    
    @TypeConverter
    fun fromNotificationType(type: NotificationType): String {
        return type.name
    }
    
    @TypeConverter
    fun toNotificationType(type: String): NotificationType {
        return NotificationType.valueOf(type)
    }
}
