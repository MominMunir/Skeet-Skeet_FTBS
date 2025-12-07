package com.example.smd_fyp.database

import androidx.room.*
import com.example.smd_fyp.model.Notification
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM Notification WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotificationsByUser(userId: String): Flow<List<Notification>>
    
    @Query("SELECT * FROM Notification WHERE userId = :userId AND isRead = 0 ORDER BY createdAt DESC")
    fun getUnreadNotificationsByUser(userId: String): Flow<List<Notification>>
    
    @Query("SELECT COUNT(*) FROM Notification WHERE userId = :userId AND isRead = 0")
    suspend fun getUnreadCount(userId: String): Int
    
    @Query("SELECT * FROM Notification WHERE id = :id")
    suspend fun getNotification(id: String): Notification?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)
    
    @Update
    suspend fun updateNotification(notification: Notification)
    
    @Query("UPDATE Notification SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)
    
    @Query("UPDATE Notification SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)
    
    @Delete
    suspend fun deleteNotification(notification: Notification)
    
    @Query("SELECT * FROM Notification WHERE userId = :userId AND synced = 0")
    suspend fun getUnsyncedNotifications(userId: String): List<Notification>
}
