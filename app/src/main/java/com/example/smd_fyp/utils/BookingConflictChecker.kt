package com.example.smd_fyp.utils

import com.example.smd_fyp.model.Booking
import com.example.smd_fyp.model.BookingStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility to check for booking conflicts
 */
object BookingConflictChecker {
    
    /**
     * Check if a new booking conflicts with existing bookings
     * @param newBooking The booking to check
     * @param existingBookings List of existing bookings for the same ground
     * @return true if there's a conflict, false otherwise
     */
    fun hasConflict(
        newBooking: Booking,
        existingBookings: List<Booking>
    ): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        // Parse new booking date and time
        val newBookingDate = try {
            dateFormat.parse(newBooking.date) ?: return false
        } catch (e: Exception) {
            return false
        }
        
        val newBookingTime = try {
            timeFormat.parse(newBooking.time) ?: return false
        } catch (e: Exception) {
            return false
        }
        
        val newBookingTimeCal = Calendar.getInstance().apply {
            time = newBookingTime
        }
        
        val newBookingStart = Calendar.getInstance().apply {
            time = newBookingDate
            set(Calendar.HOUR_OF_DAY, newBookingTimeCal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, newBookingTimeCal.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val newBookingEnd = Calendar.getInstance().apply {
            timeInMillis = newBookingStart.timeInMillis
            add(Calendar.HOUR_OF_DAY, newBooking.duration)
        }
        
        // Check against existing bookings
        for (existingBooking in existingBookings) {
            // Skip cancelled bookings
            if (existingBooking.status == BookingStatus.CANCELLED) {
                continue
            }
            
            // Only check bookings for the same ground
            if (existingBooking.groundId != newBooking.groundId) {
                continue
            }
            
            // Parse existing booking date and time
            val existingDate = try {
                dateFormat.parse(existingBooking.date) ?: continue
            } catch (e: Exception) {
                continue
            }
            
            val existingTime = try {
                timeFormat.parse(existingBooking.time) ?: continue
            } catch (e: Exception) {
                continue
            }
            
            val existingTimeCal = Calendar.getInstance().apply {
                time = existingTime
            }
            
            val existingStart = Calendar.getInstance().apply {
                time = existingDate
                set(Calendar.HOUR_OF_DAY, existingTimeCal.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, existingTimeCal.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val existingEnd = Calendar.getInstance().apply {
                timeInMillis = existingStart.timeInMillis
                add(Calendar.HOUR_OF_DAY, existingBooking.duration)
            }
            
            // Check for overlap
            if (isOverlapping(newBookingStart, newBookingEnd, existingStart, existingEnd)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check if two time ranges overlap
     */
    private fun isOverlapping(
        start1: Calendar,
        end1: Calendar,
        start2: Calendar,
        end2: Calendar
    ): Boolean {
        // Two ranges overlap if: start1 < end2 && start2 < end1
        return start1.timeInMillis < end2.timeInMillis && start2.timeInMillis < end1.timeInMillis
    }
    
    /**
     * Get list of unavailable dates for a ground
     * @param groundId The ground ID
     * @param existingBookings List of all bookings
     * @param startDate Start date to check from
     * @param endDate End date to check until
     * @return Set of dates (as yyyy-MM-dd strings) that are fully booked
     */
    fun getUnavailableDates(
        groundId: String,
        existingBookings: List<Booking>,
        startDate: Calendar,
        endDate: Calendar
    ): Set<String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val unavailableDates = mutableSetOf<String>()
        
        // Group bookings by date
        val bookingsByDate = existingBookings
            .filter { 
                it.groundId == groundId && 
                it.status != BookingStatus.CANCELLED 
            }
            .groupBy { it.date }
        
        // Check each date in range
        val currentDate = Calendar.getInstance().apply {
            timeInMillis = startDate.timeInMillis
        }
        
        while (currentDate.before(endDate) || currentDate == endDate) {
            val dateStr = dateFormat.format(currentDate.time)
            val dayBookings = bookingsByDate[dateStr] ?: emptyList()
            
            // Count total booked hours for this day
            val totalBookedHours = dayBookings.sumOf { it.duration }
            
            // If more than 20 hours are booked (assuming 24-hour availability), mark as unavailable
            // Or if there are too many bookings, mark as unavailable
            if (totalBookedHours >= 20 || dayBookings.size >= 10) {
                unavailableDates.add(dateStr)
            }
            
            currentDate.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return unavailableDates
    }
    
    /**
     * Get list of unavailable time slots for a specific date
     * @param groundId The ground ID
     * @param date Date string in yyyy-MM-dd format
     * @param existingBookings List of all bookings
     * @return List of time ranges (as Pair<startHour, endHour>) that are booked
     */
    fun getUnavailableTimeSlots(
        groundId: String,
        date: String,
        existingBookings: List<Booking>
    ): List<Pair<Int, Int>> {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val unavailableSlots = mutableListOf<Pair<Int, Int>>()
        
        val dayBookings = existingBookings
            .filter { 
                it.groundId == groundId && 
                it.date == date &&
                it.status != BookingStatus.CANCELLED 
            }
        
        for (booking in dayBookings) {
            try {
                val time = timeFormat.parse(booking.time) ?: continue
                val timeCal = Calendar.getInstance().apply {
                    this.time = time
                }
                val startHour = timeCal.get(Calendar.HOUR_OF_DAY)
                val endHour = startHour + booking.duration
                unavailableSlots.add(Pair(startHour, endHour))
            } catch (e: Exception) {
                continue
            }
        }
        
        return unavailableSlots.sortedBy { it.first }
    }
}
