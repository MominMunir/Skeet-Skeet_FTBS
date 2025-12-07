package com.example.smd_fyp.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

object FirebaseAuthHelper {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Get current authenticated user
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmailPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create new user account with email and password
     */
    suspend fun createUserWithEmailPassword(
        email: String,
        password: String,
        fullName: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            // Update user profile with display name
            result.user?.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build()
            )?.await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Delete current user account
     */
    suspend fun deleteUser(): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                user.delete().await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user password
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                user.updatePassword(newPassword).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Re-authenticate user (required for sensitive operations)
     */
    suspend fun reauthenticateUser(email: String, password: String): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user != null) {
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("No user logged in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
