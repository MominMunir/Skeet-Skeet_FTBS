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
                
                // Check if booking exists in PHP by trying to get it
                var bookingExists = false
                if (booking.id.isNotEmpty()) {
                    try {
                        val existingBookingResponse = apiService.getBooking(booking.id)
                        bookingExists = existingBookingResponse.isSuccessful && existingBookingResponse.body() != null
                    } catch (e: Exception) {
                        android.util.Log.d("SyncManager", "Could not check if booking exists, will try to create: ${e.message}")
                        bookingExists = false
                    }
                }
                
                // If booking exists in PHP, update it; otherwise create it
                val response = if (bookingExists) {
                    android.util.Log.d("SyncManager", "Booking exists in PHP, updating: ${booking.id}")
                    apiService.updateBooking(booking)
                } else {
                    android.util.Log.d("SyncManager", "Booking doesn't exist in PHP, creating: ${booking.id}")
                    apiService.createBooking(booking)
                }
                
                if (response.isSuccessful && response.body() != null) {
                    android.util.Log.d("SyncManager", "Successfully synced booking to PHP: ${response.body()!!.id}")
                    Result.success(response.body()!!)
                } else {
                    val errorBody = try {
                        response.errorBody()?.string() ?: "Unknown error"
                    } catch (e: Exception) {
                        "Could not read error body: ${e.message}"
                    }
                    android.util.Log.e("SyncManager", "PHP API failed: ${response.code()} - $errorBody")
                    Result.failure(Exception("PHP API failed: ${response.code()} - $errorBody"))
                }
            } catch (e: Exception) {
                android.util.Log.e("SyncManager", "PHP API exception: ${e.message}", e)
                Result.failure(Exception("PHP API error: ${e.message}", e))
            }
            
            // Also sync to Firestore as backup (optional - don't fail if Firestore is disabled)
            if (phpResult.isSuccess) {
                val syncedBooking = phpResult.getOrNull() ?: booking
                
                // Try to sync to Firestore, but don't fail if it's disabled
                try {
                    val firestoreResult = FirestoreHelper.saveBooking(syncedBooking.copy(synced = true))
                    if (firestoreResult.isFailure) {
                        val error = firestoreResult.exceptionOrNull()
                        if (error?.message?.contains("PERMISSION_DENIED") == true || 
                            error?.message?.contains("API has not been used") == true) {
                            android.util.Log.w("SyncManager", "Firestore API not enabled. Skipping Firestore sync.")
                        } else {
                            android.util.Log.w("SyncManager", "Firestore sync failed (non-critical): ${error?.message}")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManager", "Firestore sync error (non-critical): ${e.message}")
                }
                
                // Update local SQLite with synced status (PHP sync succeeded)
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
                
                // Check if ground exists in PHP by trying to get it
                var groundExists = false
                if (ground.id.isNotEmpty()) {
                    try {
                        val existingGroundResponse = apiService.getGround(ground.id)
                        groundExists = existingGroundResponse.isSuccessful && existingGroundResponse.body() != null
                    } catch (e: Exception) {
                        android.util.Log.d("SyncManager", "Could not check if ground exists, will try to create: ${e.message}")
                        groundExists = false
                    }
                }
                
                // If ground exists in PHP, update it; otherwise create it
                val response = if (groundExists) {
                    android.util.Log.d("SyncManager", "Ground exists in PHP, updating: ${ground.id}")
                    apiService.updateGround(ground)
                } else {
                    android.util.Log.d("SyncManager", "Ground doesn't exist in PHP, creating: ${ground.id}")
                    apiService.createGround(ground)
                }
                
                if (response.isSuccessful && response.body() != null) {
                    android.util.Log.d("SyncManager", "Successfully synced ground to PHP: ${response.body()!!.id}")
                    Result.success(response.body()!!)
                } else {
                    val errorBody = try {
                        response.errorBody()?.string() ?: "Unknown error"
                    } catch (e: Exception) {
                        "Could not read error body: ${e.message}"
                    }
                    android.util.Log.e("SyncManager", "PHP API failed: ${response.code()} - $errorBody")
                    Result.failure(Exception("PHP API failed: ${response.code()} - $errorBody"))
                }
            } catch (e: Exception) {
                android.util.Log.e("SyncManager", "PHP API exception: ${e.message}", e)
                Result.failure(Exception("PHP API error: ${e.message}", e))
            }
            
            // Also sync to Firestore (optional - don't fail if Firestore is disabled)
            if (phpResult.isSuccess) {
                val syncedGround = phpResult.getOrNull() ?: ground
                
                // Try to sync to Firestore, but don't fail if it's disabled
                try {
                    val firestoreResult = FirestoreHelper.saveGround(syncedGround.copy(synced = true))
                    if (firestoreResult.isFailure) {
                        val error = firestoreResult.exceptionOrNull()
                        if (error?.message?.contains("PERMISSION_DENIED") == true || 
                            error?.message?.contains("API has not been used") == true) {
                            android.util.Log.w("SyncManager", "Firestore API not enabled. Skipping Firestore sync.")
                        } else {
                            android.util.Log.w("SyncManager", "Firestore sync failed (non-critical): ${error?.message}")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManager", "Firestore sync error (non-critical): ${e.message}")
                }
                
                // Update local SQLite with synced status (PHP sync succeeded)
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
                
                // Check if user exists in PHP by trying to get it
                var userExists = false
                if (user.id.isNotEmpty()) {
                    try {
                        val existingUserResponse = apiService.getUser(user.id)
                        // User exists if response is successful and body is not null
                        userExists = existingUserResponse.isSuccessful && existingUserResponse.body() != null
                    } catch (e: Exception) {
                        // If GET fails, assume user doesn't exist and try to create
                        android.util.Log.d("SyncManager", "Could not check if user exists, will try to create: ${e.message}")
                        userExists = false
                    }
                }
                
                // If user exists in PHP, update it; otherwise create it
                val response = if (userExists) {
                    // User exists, update it
                    android.util.Log.d("SyncManager", "User exists in PHP, updating: ${user.id}")
                    apiService.updateUser(user)
                } else {
                    // User doesn't exist, create it
                    android.util.Log.d("SyncManager", "User doesn't exist in PHP, creating: ${user.id}")
                    apiService.createUser(user)
                }
                
                if (response.isSuccessful && response.body() != null) {
                    android.util.Log.d("SyncManager", "Successfully synced user to PHP: ${response.body()!!.id}")
                    Result.success(response.body()!!)
                } else {
                    val errorBody = try {
                        response.errorBody()?.string() ?: "Unknown error"
                    } catch (e: Exception) {
                        "Could not read error body: ${e.message}"
                    }
                    android.util.Log.e("SyncManager", "PHP API failed: ${response.code()} - $errorBody")
                    Result.failure(Exception("PHP API failed: ${response.code()} - $errorBody"))
                }
            } catch (e: Exception) {
                android.util.Log.e("SyncManager", "PHP API exception: ${e.message}", e)
                Result.failure(Exception("PHP API error: ${e.message}", e))
            }
            
            // Also sync to Firestore (optional - don't fail if Firestore is disabled)
            if (phpResult.isSuccess) {
                val syncedUser = phpResult.getOrNull() ?: user
                
                // Try to sync to Firestore, but don't fail if it's disabled
                try {
                    val firestoreResult = FirestoreHelper.saveUser(syncedUser.copy(synced = true))
                    if (firestoreResult.isFailure) {
                        val error = firestoreResult.exceptionOrNull()
                        if (error?.message?.contains("PERMISSION_DENIED") == true || 
                            error?.message?.contains("API has not been used") == true) {
                            android.util.Log.w("SyncManager", "Firestore API not enabled. Skipping Firestore sync. Enable it at: https://console.developers.google.com/apis/api/firestore.googleapis.com/overview?project=skeetskeet-5c074")
                        } else {
                            android.util.Log.w("SyncManager", "Firestore sync failed (non-critical): ${error?.message}")
                        }
                    } else {
                        android.util.Log.d("SyncManager", "Successfully synced user to Firestore: ${syncedUser.id}")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManager", "Firestore sync error (non-critical): ${e.message}")
                }
                
                // Update local SQLite with synced status (PHP sync succeeded)
                LocalDatabaseHelper.updateUser(syncedUser.copy(synced = true))
            } else {
                // Log the error for debugging
                android.util.Log.e("SyncManager", "Failed to sync user to PHP: ${phpResult.exceptionOrNull()?.message}")
            }
            
            phpResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete user from all sources (Local DB, PHP API, Firebase)
     */
    suspend fun deleteUser(context: Context, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Delete from local SQLite first
            val user = LocalDatabaseHelper.getUser(userId)
            user?.let {
                LocalDatabaseHelper.deleteUser(it)
            }
            
            // If offline, just return success (deleted locally)
            if (!isOnline(context)) {
                return@withContext Result.success(Unit)
            }
            
            // Delete from PHP API
            val phpResult = try {
                val apiService = ApiClient.getPhpApiService(context)
                val response = apiService.deleteUser(userId)
                if (response.isSuccessful) {
                    android.util.Log.d("SyncManager", "Successfully deleted user from PHP: $userId")
                    Result.success(Unit)
                } else {
                    val errorBody = try {
                        response.errorBody()?.string() ?: "Unknown error"
                    } catch (e: Exception) {
                        "Could not read error body: ${e.message}"
                    }
                    android.util.Log.e("SyncManager", "PHP API delete failed: ${response.code()} - $errorBody")
                    Result.failure(Exception("PHP API delete failed: ${response.code()} - $errorBody"))
                }
            } catch (e: Exception) {
                android.util.Log.e("SyncManager", "PHP API delete exception: ${e.message}", e)
                Result.failure(Exception("PHP API delete error: ${e.message}", e))
            }
            
            // Also delete from Firestore (optional - don't fail if Firestore is disabled)
            if (phpResult.isSuccess) {
                try {
                    val firestoreResult = FirestoreHelper.deleteUser(userId)
                    if (firestoreResult.isFailure) {
                        val error = firestoreResult.exceptionOrNull()
                        if (error?.message?.contains("PERMISSION_DENIED") == true || 
                            error?.message?.contains("API has not been used") == true) {
                            android.util.Log.w("SyncManager", "Firestore API not enabled. Skipping Firestore delete.")
                        } else {
                            android.util.Log.w("SyncManager", "Firestore delete failed (non-critical): ${error?.message}")
                        }
                    } else {
                        android.util.Log.d("SyncManager", "Successfully deleted user from Firestore: $userId")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManager", "Firestore delete error (non-critical): ${e.message}")
                }
            }
            
            phpResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete ground from all sources (Local DB, PHP API, Firebase)
     */
    suspend fun deleteGround(context: Context, groundId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Delete from local SQLite first
            val ground = LocalDatabaseHelper.getGround(groundId)
            ground?.let {
                LocalDatabaseHelper.deleteGround(it)
            }
            
            // If offline, just return success (deleted locally)
            if (!isOnline(context)) {
                return@withContext Result.success(Unit)
            }
            
            // Delete from PHP API
            val phpResult = try {
                val apiService = ApiClient.getPhpApiService(context)
                val response = apiService.deleteGround(groundId)
                if (response.isSuccessful) {
                    android.util.Log.d("SyncManager", "Successfully deleted ground from PHP: $groundId")
                    Result.success(Unit)
                } else {
                    val errorBody = try {
                        response.errorBody()?.string() ?: "Unknown error"
                    } catch (e: Exception) {
                        "Could not read error body: ${e.message}"
                    }
                    android.util.Log.e("SyncManager", "PHP API delete failed: ${response.code()} - $errorBody")
                    Result.failure(Exception("PHP API delete failed: ${response.code()} - $errorBody"))
                }
            } catch (e: Exception) {
                android.util.Log.e("SyncManager", "PHP API delete exception: ${e.message}", e)
                Result.failure(Exception("PHP API delete error: ${e.message}", e))
            }
            
            // Also delete from Firestore (optional - don't fail if Firestore is disabled)
            if (phpResult.isSuccess) {
                try {
                    val firestoreResult = FirestoreHelper.deleteGround(groundId)
                    if (firestoreResult.isFailure) {
                        val error = firestoreResult.exceptionOrNull()
                        if (error?.message?.contains("PERMISSION_DENIED") == true || 
                            error?.message?.contains("API has not been used") == true) {
                            android.util.Log.w("SyncManager", "Firestore API not enabled. Skipping Firestore delete.")
                        } else {
                            android.util.Log.w("SyncManager", "Firestore delete failed (non-critical): ${error?.message}")
                        }
                    } else {
                        android.util.Log.d("SyncManager", "Successfully deleted ground from Firestore: $groundId")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManager", "Firestore delete error (non-critical): ${e.message}")
                }
            }
            
            phpResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete booking from all sources (Local DB, PHP API, Firebase)
     */
    suspend fun deleteBooking(context: Context, bookingId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Delete from local SQLite first
            val booking = LocalDatabaseHelper.getBooking(bookingId)
            booking?.let {
                LocalDatabaseHelper.deleteBooking(it)
            }
            
            // If offline, just return success (deleted locally)
            if (!isOnline(context)) {
                return@withContext Result.success(Unit)
            }
            
            // Delete from PHP API
            val phpResult = try {
                val apiService = ApiClient.getPhpApiService(context)
                val response = apiService.deleteBooking(bookingId)
                if (response.isSuccessful) {
                    android.util.Log.d("SyncManager", "Successfully deleted booking from PHP: $bookingId")
                    Result.success(Unit)
                } else {
                    val errorBody = try {
                        response.errorBody()?.string() ?: "Unknown error"
                    } catch (e: Exception) {
                        "Could not read error body: ${e.message}"
                    }
                    android.util.Log.e("SyncManager", "PHP API delete failed: ${response.code()} - $errorBody")
                    Result.failure(Exception("PHP API delete failed: ${response.code()} - $errorBody"))
                }
            } catch (e: Exception) {
                android.util.Log.e("SyncManager", "PHP API delete exception: ${e.message}", e)
                Result.failure(Exception("PHP API delete error: ${e.message}", e))
            }
            
            // Also delete from Firestore (optional - don't fail if Firestore is disabled)
            if (phpResult.isSuccess) {
                try {
                    val firestoreResult = FirestoreHelper.deleteBooking(bookingId)
                    if (firestoreResult.isFailure) {
                        val error = firestoreResult.exceptionOrNull()
                        if (error?.message?.contains("PERMISSION_DENIED") == true || 
                            error?.message?.contains("API has not been used") == true) {
                            android.util.Log.w("SyncManager", "Firestore API not enabled. Skipping Firestore delete.")
                        } else {
                            android.util.Log.w("SyncManager", "Firestore delete failed (non-critical): ${error?.message}")
                        }
                    } else {
                        android.util.Log.d("SyncManager", "Successfully deleted booking from Firestore: $bookingId")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SyncManager", "Firestore delete error (non-critical): ${e.message}")
                }
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
