package com.example.smd_fyp.database

import androidx.room.*
import com.example.smd_fyp.model.Booking
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query("SELECT * FROM Booking WHERE userId = :userId ORDER BY createdAt DESC")
    fun getBookingsByUser(userId: String): Flow<List<Booking>>
    
    @Query("SELECT * FROM Booking WHERE id = :bookingId")
    suspend fun getBooking(bookingId: String): Booking?
    
    @Query("SELECT * FROM Booking WHERE synced = 0")
    suspend fun getUnsyncedBookings(): List<Booking>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookings(bookings: List<Booking>)
    
    @Update
    suspend fun updateBooking(booking: Booking)
    
    @Delete
    suspend fun deleteBooking(booking: Booking)
    
    @Query("DELETE FROM Booking WHERE id = :bookingId")
    suspend fun deleteBookingById(bookingId: String)
    
    @Query("SELECT * FROM Booking")
    fun getAllBookings(): Flow<List<Booking>>
    
    @Query("SELECT * FROM Booking")
    suspend fun getAllBookingsSync(): List<Booking>
}
