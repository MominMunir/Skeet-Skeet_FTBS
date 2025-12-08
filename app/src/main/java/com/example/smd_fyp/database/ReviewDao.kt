package com.example.smd_fyp.database

import androidx.room.*
import com.example.smd_fyp.model.Review
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Query("SELECT * FROM Review WHERE groundId = :groundId ORDER BY createdAt DESC")
    fun getReviewsByGround(groundId: String): Flow<List<Review>>
    
    @Query("SELECT * FROM Review WHERE groundId = :groundId ORDER BY createdAt DESC")
    suspend fun getReviewsByGroundSync(groundId: String): List<Review>
    
    @Query("SELECT * FROM Review WHERE userId = :userId ORDER BY createdAt DESC")
    fun getReviewsByUser(userId: String): Flow<List<Review>>
    
    @Query("SELECT AVG(rating) FROM Review WHERE groundId = :groundId")
    suspend fun getAverageRating(groundId: String): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review)
    
    @Update
    suspend fun updateReview(review: Review)
    
    @Delete
    suspend fun deleteReview(review: Review)
    
    @Query("SELECT * FROM Review WHERE id = :id")
    suspend fun getReview(id: String): Review?
    
    @Query("SELECT * FROM Review")
    suspend fun getAllReviewsSync(): List<Review>
}
