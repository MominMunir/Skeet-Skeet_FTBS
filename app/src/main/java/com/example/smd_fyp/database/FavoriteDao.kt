package com.example.smd_fyp.database

import androidx.room.*
import com.example.smd_fyp.model.Favorite
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM Favorite WHERE userId = :userId ORDER BY createdAt DESC")
    fun getFavoritesByUser(userId: String): Flow<List<Favorite>>
    
    @Query("SELECT * FROM Favorite WHERE userId = :userId AND groundId = :groundId")
    suspend fun getFavorite(userId: String, groundId: String): Favorite?
    
    @Query("SELECT COUNT(*) FROM Favorite WHERE userId = :userId AND groundId = :groundId")
    suspend fun isFavorite(userId: String, groundId: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)
    
    @Delete
    suspend fun deleteFavorite(favorite: Favorite)
    
    @Query("DELETE FROM Favorite WHERE userId = :userId AND groundId = :groundId")
    suspend fun deleteFavorite(userId: String, groundId: String)
    
    @Query("SELECT * FROM Favorite WHERE userId = :userId AND synced = 0")
    suspend fun getUnsyncedFavorites(userId: String): List<Favorite>
}
