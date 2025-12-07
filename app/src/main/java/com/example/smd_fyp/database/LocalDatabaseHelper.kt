package com.example.smd_fyp.database

import android.content.Context
import com.example.smd_fyp.model.Booking
import com.example.smd_fyp.model.GroundApi
import com.example.smd_fyp.model.Notification
import com.example.smd_fyp.model.Review
import com.example.smd_fyp.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Local Database Helper using SQLite (via Room)
 * 
 * Room is an abstraction layer over SQLite that provides:
 * - Type-safe database access
 * - Compile-time query verification
 * - Automatic migration support
 * - Coroutines and Flow support
 * 
 * The actual database file is stored at:
 * /data/data/com.example.smd_fyp/databases/skeetskeet_database
 */
object LocalDatabaseHelper {
    
    private var database: AppDatabase? = null
    
    /**
     * Initialize the database
     * Must be called before using any database operations
     */
    fun initialize(context: Context) {
        if (database == null) {
            database = AppDatabase.getDatabase(context)
        }
    }
    
    // ========== BOOKINGS ==========
    
    /**
     * Save booking to local SQLite database (works offline)
     */
    suspend fun saveBooking(booking: Booking) {
        database?.bookingDao()?.insertBooking(booking)
    }
    
    /**
     * Get all bookings for a user (from local SQLite)
     * Returns Flow for reactive updates
     */
    fun getBookingsByUser(userId: String): Flow<List<Booking>>? {
        return database?.bookingDao()?.getBookingsByUser(userId)
    }
    
    /**
     * Get single booking by ID (from local SQLite)
     */
    suspend fun getBooking(bookingId: String): Booking? {
        return database?.bookingDao()?.getBooking(bookingId)
    }
    
    /**
     * Get all unsynced bookings (for sync when online)
     */
    suspend fun getUnsyncedBookings(): List<Booking> {
        return database?.bookingDao()?.getUnsyncedBookings() ?: emptyList()
    }
    
    /**
     * Update booking in local SQLite
     */
    suspend fun updateBooking(booking: Booking) {
        database?.bookingDao()?.updateBooking(booking)
    }
    
    /**
     * Delete booking from local SQLite
     */
    suspend fun deleteBooking(booking: Booking) {
        database?.bookingDao()?.deleteBooking(booking)
    }
    
    /**
     * Mark booking as synced
     */
    suspend fun markBookingAsSynced(bookingId: String) {
        val booking = getBooking(bookingId)
        booking?.let {
            updateBooking(it.copy(synced = true))
        }
    }
    
    // ========== GROUNDS ==========
    
    /**
     * Save ground to local SQLite database (works offline)
     */
    suspend fun saveGround(ground: GroundApi) {
        database?.groundDao()?.insertGround(ground)
    }
    
    /**
     * Save multiple grounds (for initial sync)
     */
    suspend fun saveGrounds(grounds: List<GroundApi>) {
        database?.groundDao()?.insertGrounds(grounds)
    }
    
    /**
     * Get all available grounds (from local SQLite)
     * Returns Flow for reactive updates
     */
    fun getAvailableGrounds(): Flow<List<GroundApi>>? {
        return database?.groundDao()?.getAvailableGrounds()
    }
    
    /**
     * Get all grounds (from local SQLite)
     */
    fun getAllGrounds(): Flow<List<GroundApi>>? {
        return database?.groundDao()?.getAllGrounds()
    }
    
    /**
     * Get single ground by ID (from local SQLite)
     */
    suspend fun getGround(groundId: String): GroundApi? {
        return database?.groundDao()?.getGround(groundId)
    }
    
    /**
     * Get all unsynced grounds (for sync when online)
     */
    suspend fun getUnsyncedGrounds(): List<GroundApi> {
        return database?.groundDao()?.getUnsyncedGrounds() ?: emptyList()
    }
    
    /**
     * Update ground in local SQLite
     */
    suspend fun updateGround(ground: GroundApi) {
        database?.groundDao()?.updateGround(ground)
    }
    
    /**
     * Delete ground from local SQLite
     */
    suspend fun deleteGround(ground: GroundApi) {
        database?.groundDao()?.deleteGround(ground)
    }
    
    /**
     * Delete ground by ID from local SQLite
     */
    suspend fun deleteGroundById(groundId: String) {
        database?.groundDao()?.deleteGroundById(groundId)
    }
    
    /**
     * Mark ground as synced
     */
    suspend fun markGroundAsSynced(groundId: String) {
        val ground = getGround(groundId)
        ground?.let {
            updateGround(it.copy(synced = true))
        }
    }
    
    // ========== USERS ==========
    
    /**
     * Save user to local SQLite database (works offline)
     */
    suspend fun saveUser(user: User) {
        database?.userDao()?.insertUser(user)
    }
    
    /**
     * Get user by ID (from local SQLite)
     */
    suspend fun getUser(userId: String): User? {
        return database?.userDao()?.getUser(userId)
    }
    
    /**
     * Get user by email (from local SQLite)
     */
    suspend fun getUserByEmail(email: String): User? {
        return database?.userDao()?.getUserByEmail(email)
    }
    
    /**
     * Get all users (from local SQLite)
     * Returns Flow for reactive updates
     */
    fun getAllUsers(): Flow<List<User>>? {
        return database?.userDao()?.getAllUsers()
    }
    
    /**
     * Get all unsynced users (for sync when online)
     */
    suspend fun getUnsyncedUsers(): List<User> {
        return database?.userDao()?.getUnsyncedUsers() ?: emptyList()
    }
    
    /**
     * Update user in local SQLite
     */
    suspend fun updateUser(user: User) {
        database?.userDao()?.updateUser(user)
    }
    
    /**
     * Delete user from local SQLite
     */
    suspend fun deleteUser(user: User) {
        database?.userDao()?.deleteUser(user)
    }
    
    /**
     * Mark user as synced
     */
    suspend fun markUserAsSynced(userId: String) {
        val user = getUser(userId)
        user?.let {
            updateUser(it.copy(synced = true))
        }
    }
    
    // ========== REVIEWS ==========
    
    /**
     * Save review to local SQLite database
     */
    suspend fun saveReview(review: Review) {
        database?.reviewDao()?.insertReview(review)
    }
    
    /**
     * Get reviews for a ground
     */
    fun getReviewsByGround(groundId: String): Flow<List<Review>>? {
        return database?.reviewDao()?.getReviewsByGround(groundId)
    }
    
    /**
     * Get reviews for a ground (synchronous)
     */
    suspend fun getReviewsByGroundSync(groundId: String): List<Review> {
        return database?.reviewDao()?.getReviewsByGroundSync(groundId) ?: emptyList()
    }
    
    /**
     * Get average rating for a ground
     */
    suspend fun getAverageRating(groundId: String): Double {
        return database?.reviewDao()?.getAverageRating(groundId) ?: 0.0
    }
    
    /**
     * Calculate and update ground rating from reviews
     */
    suspend fun updateGroundRating(groundId: String) {
        val averageRating = getAverageRating(groundId)
        val ground = getGround(groundId)
        ground?.let {
            val updatedGround = it.copy(
                rating = averageRating,
                ratingText = String.format("%.1f", averageRating)
            )
            saveGround(updatedGround)
        }
    }
    
    // ========== NOTIFICATIONS ==========
    
    /**
     * Save notification to local SQLite database
     */
    suspend fun saveNotification(notification: Notification) {
        try {
            database?.notificationDao()?.insertNotification(notification)
            android.util.Log.d("LocalDatabaseHelper", "Notification saved: ${notification.id} - ${notification.title}")
        } catch (e: Exception) {
            android.util.Log.e("LocalDatabaseHelper", "Error saving notification: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Get notifications for a user
     */
    fun getNotificationsByUser(userId: String): Flow<List<Notification>>? {
        return try {
            database?.notificationDao()?.getNotificationsByUser(userId)
        } catch (e: Exception) {
            android.util.Log.e("LocalDatabaseHelper", "Error getting notifications: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get unread notifications for a user
     */
    fun getUnreadNotificationsByUser(userId: String): Flow<List<Notification>>? {
        return database?.notificationDao()?.getUnreadNotificationsByUser(userId)
    }
    
    /**
     * Get unread notification count
     */
    suspend fun getUnreadNotificationCount(userId: String): Int {
        return database?.notificationDao()?.getUnreadCount(userId) ?: 0
    }
    
    /**
     * Mark notification as read
     */
    suspend fun markNotificationAsRead(notificationId: String) {
        database?.notificationDao()?.markAsRead(notificationId)
    }
    
    /**
     * Mark all notifications as read for a user
     */
    suspend fun markAllNotificationsAsRead(userId: String) {
        database?.notificationDao()?.markAllAsRead(userId)
    }
    
    /**
     * Get unsynced notifications
     */
    suspend fun getUnsyncedNotifications(userId: String): List<Notification> {
        return database?.notificationDao()?.getUnsyncedNotifications(userId) ?: emptyList()
    }
    
    // ========== FAVORITES ==========
    
    /**
     * Add favorite to local SQLite database
     */
    suspend fun addFavorite(favorite: Favorite) {
        database?.favoriteDao()?.insertFavorite(favorite)
    }
    
    /**
     * Remove favorite from local SQLite database
     */
    suspend fun removeFavorite(userId: String, groundId: String) {
        database?.favoriteDao()?.deleteFavorite(userId, groundId)
    }
    
    /**
     * Check if ground is favorite
     */
    suspend fun isFavorite(userId: String, groundId: String): Int {
        return database?.favoriteDao()?.isFavorite(userId, groundId) ?: 0
    }
    
    /**
     * Get favorites for a user
     */
    fun getFavoritesByUser(userId: String): kotlinx.coroutines.flow.Flow<List<Favorite>>? {
        return try {
            database?.favoriteDao()?.getFavoritesByUser(userId)
        } catch (e: Exception) {
            android.util.Log.e("LocalDatabaseHelper", "Error getting favorites: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get favorite by user and ground
     */
    suspend fun getFavorite(userId: String, groundId: String): Favorite? {
        return database?.favoriteDao()?.getFavorite(userId, groundId)
    }
    
    /**
     * Get unsynced favorites
     */
    suspend fun getUnsyncedFavorites(userId: String): List<Favorite> {
        return database?.favoriteDao()?.getUnsyncedFavorites(userId) ?: emptyList()
    }
}
