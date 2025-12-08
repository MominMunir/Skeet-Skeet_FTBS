package com.example.smd_fyp.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.model.BookingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Periodically updates booking statuses based on current time:
 * - If now >= start and status is PENDING -> set CONFIRMED
 * - If now >= end and status is not COMPLETED/CANCELLED -> set COMPLETED
 */
class AutoBookingStatusWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            LocalDatabaseHelper.initialize(applicationContext)

            val bookings = LocalDatabaseHelper.getAllBookingsSync()
            val now = System.currentTimeMillis()

            bookings.forEach { booking ->
                val startMillis = parseStartMillis(booking.date, booking.time) ?: return@forEach
                val durationMillis = TimeUnit.HOURS.toMillis(booking.duration.toLong())
                val endMillis = startMillis + durationMillis

                val newStatus = when {
                    now >= endMillis && booking.status != BookingStatus.COMPLETED && booking.status != BookingStatus.CANCELLED ->
                        BookingStatus.COMPLETED
                    now >= startMillis && now < endMillis && booking.status == BookingStatus.PENDING ->
                        BookingStatus.CONFIRMED
                    else -> booking.status
                }

                if (newStatus != booking.status) {
                    val updated = booking.copy(
                        status = newStatus,
                        updatedAt = now,
                        synced = false
                    )
                    LocalDatabaseHelper.updateBooking(updated)

                    if (SyncManager.isOnline(applicationContext)) {
                        SyncManager.syncBooking(applicationContext, updated)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun parseStartMillis(date: String, time: String): Long? {
        return try {
            dateTimeFormat.parse("$date $time")?.time
        } catch (_: Exception) {
            null
        }
    }
}
