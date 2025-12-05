package com.example.smd_fyp.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FirestoreHelper
import com.example.smd_fyp.model.Booking
import com.example.smd_fyp.model.GroundApi
import com.example.smd_fyp.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sync Manager for offline/online data synchronization
 * Syncs data between:
 * - Local Room Database (offline)
 * - PHP API (XAMPP server)
 * - Firestore (cloud backup)
 */
object SyncManager {
    
    /**
     * Check if device is online
     */
    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Sync all unsynced data from local SQLite to PHP API and Firestore
     */
    suspend fun syncAll(context: Context): Result<SyncResult> = withContext(Dispatchers.IO) {
        // Initialize database if not already done
        LocalDatabaseHelper.initialize(context)
        
        if (!isOnline(context)) {
            return@withContext Result.failure(Exception("Device is offline"))
        }
        
        return@withContext try {
            var syncedBookings = 0
            var syncedGrounds = 0
            var syncedUsers = 0
            var errors = 0
            
            // Sync bookings from local SQLite
            val unsyncedBookings = LocalDatabaseHelper.getUnsyncedBookings()
            unsyncedBookings.forEach { booking ->
                val result = syncBooking(context, booking)
                if (result.isSuccess) {
                    syncedBookings++
                    LocalDatabaseHelper.markBookingAsSynced(booking.id)
                } else {
                    errors++
                }
            }
            
            // Sync grounds from local SQLite
            val unsyncedGrounds = LocalDatabaseHelper.getUnsyncedGrounds()
            unsyncedGrounds.forEach { ground ->
                val result = syncGround(context, ground)
                if (result.isSuccess) {
                    syncedGrounds++
                    LocalDatabaseHelper.markGroundAsSynced(ground.id)
                } else {
                    errors++
                }
            }
            
            // Sync users from local SQLite
            val unsyncedUsers = LocalDatabaseHelper.getUnsyncedUsers()
            unsyncedUsers.forEach { user ->
                val result = syncUser(context, user)
                if (result.isSuccess) {
                    syncedUsers++
                    LocalDatabaseHelper.markUserAsSynced(user.id)
                } else {
                    errors++
                }
            }
            
            Result.success(
                SyncResult(
                    syncedBookings = syncedBookings,
                    syncedGrounds = syncedGrounds,
                    syncedUsers = syncedUsers,
                    errors = errors
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sync a single booking
     * First saves to local SQLite (works offline), then syncs to PHP API and Firestore when online
     */
    suspend fun syncBooking(context: Context, booking: Booking): Result<Booking> = withContext(Dispatchers.IO) {
        return@withContext try {
            // First, save to local SQLite database (works offline)
            LocalDatabaseHelper.saveBooking(booking.copy(synced = false))
            
            // If offline, just return success (data is saved locally)
            // Note: You should check isOnline before calling this, but we handle it gracefully
            if (!isOnline(context)) {
                return@withContext Result.success(booking)
            }
            
            // Try PHP API first
            val phpResult = try {
                val apiService = ApiClient.getPhpApiService(context)
                if (booking.id.isEmpty()) {
                    // Create new booking
                    val response = apiService.createBooking(booking)
                    if (response.isSuccessful && response.body() != null) {
                        Result.success(response.body()!!)
                    } else {
                        Result.failure(Exception("PHP API failed"))
                    }
                } else {
                    // Update existing booking
                    val response = apiService.updateBooking(booking)
                    if (response.isSuccessful && response.body() != null) {
                        Result.success(response.body()!!)
                    } else {
                        Result.failure(Exception("PHP API failed"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
            
            // Also sync to Firestore as backup
            if (phpResult.isSuccess) {
                val syncedBooking = phpResult.getOrNull() ?: booking
                FirestoreHelper.saveBooking(syncedBooking.copy(synced = true))
                // Update local SQLite with synced status
                LocalDatabaseHelper.updateBooking(syncedBooking.copy(synced = true))
            }
            
            phpResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sync a single ground
     * First saves to local SQLite (works offline), then syncs to PHP API and Firestore when online
     */
    suspend fun syncGround(context: Context, ground: GroundApi): Result<GroundApi> = withContext(Dispatchers.IO) {
        return@withContext try {
            // First, save to local SQLite database (works offline)
            LocalDatabaseHelper.saveGround(ground.copy(synced = false))
            
            // If offline, just return success (data is saved locally)
            if (!isOnline(context)) {
                return@withContext Result.success(ground)
            }
            
            // Try PHP API first
            val phpResult = try {
                val apiService = ApiClient.getPhpApiService(context)
                if (ground.id.isEmpty()) {
                    val response = apiService.createGround(ground)
                    if (response.isSuccessful && response.body() != null) {
                        Result.success(response.body()!!)
                    } else {
                        Result.failure(Exception("PHP API failed"))
                    }
                } else {
                    val response = apiService.updateGround(ground)
                    if (response.isSuccessful && response.body() != null) {
                        Result.success(response.body()!!)
                    } else {
                        Result.failure(Exception("PHP API failed"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
            
            // Also sync to Firestore
            if (phpResult.isSuccess) {
                val syncedGround = phpResult.getOrNull() ?: ground
                FirestoreHelper.saveGround(syncedGround.copy(synced = true))
                // Update local SQLite with synced status
                LocalDatabaseHelper.updateGround(syncedGround.copy(synced = true))
            }
            
            phpResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sync user data
     * First saves to local SQLite (works offline), then syncs to PHP API and Firestore when online
     */
    suspend fun syncUser(context: Context, user: User): Result<User> = withContext(Dispatchers.IO) {
        return@withContext try {
            // First, save to local SQLite database (works offline)
            LocalDatabaseHelper.saveUser(user.copy(synced = false))
            
            // If offline, just return success (data is saved locally)
            if (!isOnline(context)) {
                return@withContext Result.success(user)
            }
            
            // Try PHP API first
            val phpResult = try {
                val apiService = ApiClient.getPhpApiService(context)
                if (user.id.isEmpty()) {
                    val response = apiService.createUser(user)
                    if (response.isSuccessful && response.body() != null) {
                        Result.success(response.body()!!)
                    } else {
                        Result.failure(Exception("PHP API failed"))
                    }
                } else {
                    val response = apiService.updateUser(user)
                    if (response.isSuccessful && response.body() != null) {
                        Result.success(response.body()!!)
                    } else {
                        Result.failure(Exception("PHP API failed"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
            
            // Also sync to Firestore
            if (phpResult.isSuccess) {
                val syncedUser = phpResult.getOrNull() ?: user
                FirestoreHelper.saveUser(syncedUser.copy(synced = true))
                // Update local SQLite with synced status
                LocalDatabaseHelper.updateUser(syncedUser.copy(synced = true))
            }
            
            phpResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class SyncResult(
    val syncedBookings: Int,
    val syncedGrounds: Int,
    val syncedUsers: Int,
    val errors: Int
)
