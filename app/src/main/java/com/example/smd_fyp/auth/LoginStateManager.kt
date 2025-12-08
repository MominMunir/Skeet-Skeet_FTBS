package com.example.smd_fyp.auth

import android.content.Context
import android.content.SharedPreferences

object LoginStateManager {
    private const val PREFS_NAME = "login_prefs"
    private const val KEY_STAY_LOGGED_IN = "stay_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_ROLE = "user_role"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Save login state
     */
    fun saveLoginState(context: Context, userId: String, userRole: String, stayLoggedIn: Boolean) {
        val prefs = getPrefs(context).edit()
        prefs.putBoolean(KEY_STAY_LOGGED_IN, stayLoggedIn)
        if (stayLoggedIn) {
            prefs.putString(KEY_USER_ID, userId)
            prefs.putString(KEY_USER_ROLE, userRole)
        } else {
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_ROLE)
        }
        prefs.apply()
    }

    /**
     * Check if user should stay logged in
     */
    fun shouldStayLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_STAY_LOGGED_IN, false)
    }

    /**
     * Get saved user ID
     */
    fun getSavedUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_ID, null)
    }

    /**
     * Get saved user role
     */
    fun getSavedUserRole(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_ROLE, null)
    }

    /**
     * Clear login state (on logout)
     */
    fun clearLoginState(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
