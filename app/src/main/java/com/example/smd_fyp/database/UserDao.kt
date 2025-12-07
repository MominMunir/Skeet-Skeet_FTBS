package com.example.smd_fyp.database

import androidx.room.*
import com.example.smd_fyp.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM User WHERE id = :userId")
    suspend fun getUser(userId: String): User?
    
    @Query("SELECT * FROM User WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?
    
    @Query("SELECT * FROM User WHERE synced = 0")
    suspend fun getUnsyncedUsers(): List<User>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    
    @Update
    suspend fun updateUser(user: User)
    
    @Delete
    suspend fun deleteUser(user: User)
    
    @Query("SELECT * FROM User")
    fun getAllUsers(): Flow<List<User>>
}
