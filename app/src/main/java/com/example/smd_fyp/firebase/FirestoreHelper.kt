package com.example.smd_fyp.firebase

import com.example.smd_fyp.model.Booking
import com.example.smd_fyp.model.GroundApi
import com.example.smd_fyp.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirestoreHelper {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    // Enable offline persistence (works without billing!)
    init {
        db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Enable offline persistence
            .build()
    }
    
    // Users Collection
    suspend fun saveUser(user: User): Result<User> {
        return try {
            db.collection("users")
                .document(user.id)
                .set(user, SetOptions.merge())
                .await()
            Result.success(user.copy(synced = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val document = db.collection("users")
                .document(userId)
                .get()
                .await()
            
            if (document.exists()) {
                Result.success(document.toObject(User::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            db.collection("users")
                .document(userId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Bookings Collection
    suspend fun saveBooking(booking: Booking): Result<Booking> {
        return try {
            db.collection("bookings")
                .document(booking.id)
                .set(booking, SetOptions.merge())
                .await()
            Result.success(booking.copy(synced = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getBookings(userId: String? = null): Result<List<Booking>> {
        return try {
            val query = if (userId != null) {
                db.collection("bookings")
                    .whereEqualTo("userId", userId)
            } else {
                db.collection("bookings")
            }
            
            val snapshot = query.get().await()
            val bookings = snapshot.documents.mapNotNull { it.toObject(Booking::class.java) }
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getBooking(bookingId: String): Result<Booking?> {
        return try {
            val document = db.collection("bookings")
                .document(bookingId)
                .get()
                .await()
            
            if (document.exists()) {
                Result.success(document.toObject(Booking::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteBooking(bookingId: String): Result<Unit> {
        return try {
            db.collection("bookings")
                .document(bookingId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Grounds Collection
    suspend fun saveGround(ground: GroundApi): Result<GroundApi> {
        return try {
            db.collection("grounds")
                .document(ground.id)
                .set(ground, SetOptions.merge())
                .await()
            Result.success(ground.copy(synced = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getGrounds(): Result<List<GroundApi>> {
        return try {
            val snapshot = db.collection("grounds")
                .get()
                .await()
            val grounds = snapshot.documents.mapNotNull { it.toObject(GroundApi::class.java) }
            Result.success(grounds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getGround(groundId: String): Result<GroundApi?> {
        return try {
            val document = db.collection("grounds")
                .document(groundId)
                .get()
                .await()
            
            if (document.exists()) {
                Result.success(document.toObject(GroundApi::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteGround(groundId: String): Result<Unit> {
        return try {
            db.collection("grounds")
                .document(groundId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
