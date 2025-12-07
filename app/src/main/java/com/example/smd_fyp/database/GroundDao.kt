package com.example.smd_fyp.database

import androidx.room.*
import com.example.smd_fyp.model.GroundApi
import kotlinx.coroutines.flow.Flow

@Dao
interface GroundDao {
    @Query("SELECT * FROM GroundApi WHERE available = 1")
    fun getAvailableGrounds(): Flow<List<GroundApi>>
    
    @Query("SELECT * FROM GroundApi")
    fun getAllGrounds(): Flow<List<GroundApi>>
    
    @Query("SELECT * FROM GroundApi WHERE id = :groundId")
    suspend fun getGround(groundId: String): GroundApi?
    
    @Query("SELECT * FROM GroundApi WHERE synced = 0")
    suspend fun getUnsyncedGrounds(): List<GroundApi>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGround(ground: GroundApi)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrounds(grounds: List<GroundApi>)
    
    @Update
    suspend fun updateGround(ground: GroundApi)
    
    @Delete
    suspend fun deleteGround(ground: GroundApi)
    
    @Query("DELETE FROM GroundApi WHERE id = :groundId")
    suspend fun deleteGroundById(groundId: String)
}
