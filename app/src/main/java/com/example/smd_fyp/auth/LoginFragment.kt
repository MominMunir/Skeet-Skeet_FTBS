package com.example.smd_fyp.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.smd_fyp.AdminDashboardActivity
import com.example.smd_fyp.GroundkeeperDashboardActivity
import com.example.smd_fyp.HomeActivity
import com.example.smd_fyp.R
import com.example.smd_fyp.auth.LoginStateManager
import com.example.smd_fyp.auth.ResetPasswordActivity
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FCMTokenHelper
import com.example.smd_fyp.firebase.FCMTokenManager
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.model.User
import com.example.smd_fyp.model.UserRole
import com.example.smd_fyp.sync.SyncManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Tabs: Login(active) | Sign Up(inactive)
        view.findViewById<TextView>(R.id.tvSignUpTab)?.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_signup)
        }
        // Back
        view.findViewById<View>(R.id.llBack)?.setOnClickListener {
            findNavController().navigateUp()
        }
        view.findViewById<TextView>(R.id.tvForgotPassword)?.setOnClickListener {
            launchForgotPassword()
        }

        // Sign In -> Navigate based on user's stored role
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val cbStayLoggedIn = view.findViewById<CheckBox>(R.id.cbStayLoggedIn)
        
        view.findViewById<View>(R.id.btnSignIn)?.setOnClickListener {
            val email = etEmail?.text?.toString()?.trim() ?: ""
            val password = etPassword?.text?.toString() ?: ""
            
            // Validate inputs
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Show loading (you can add a progress bar here)
            view.findViewById<View>(R.id.btnSignIn)?.isEnabled = false
            
            // Sign in with Firebase
            lifecycleScope.launch {
                val result = FirebaseAuthHelper.signInWithEmailPassword(email, password)
                result.fold(
                    onSuccess = { firebaseUser ->
                        try {
                            // Successfully signed in
                            // Initialize database
                            LocalDatabaseHelper.initialize(requireContext())
                            
                            // Get user from local database or fetch from PHP API
                            var user = withContext(Dispatchers.IO) {
                                LocalDatabaseHelper.getUser(firebaseUser.uid)
                            }
                            
                            // If user not in local DB, try to fetch from PHP API
                            if (user == null) {
                                try {
                                    val apiService = com.example.smd_fyp.api.ApiClient.getPhpApiService(requireContext())
                                    val response = apiService.getUser(firebaseUser.uid)
                                    if (response.isSuccessful && response.body() != null) {
                                        user = response.body()
                                        // Save to local database
                                        user?.let { LocalDatabaseHelper.saveUser(it) }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            
                            // If still no user found, create a default one (shouldn't happen if registration worked)
                            if (user == null) {
                                user = User(
                                    id = firebaseUser.uid,
                                    email = firebaseUser.email ?: email,
                                    fullName = firebaseUser.displayName ?: "",
                                    role = UserRole.PLAYER, // Default to PLAYER if not found
                                    createdAt = System.currentTimeMillis()
                                )
                                // Save to local database
                                withContext(Dispatchers.IO) {
                                    LocalDatabaseHelper.saveUser(user!!)
                                }
                            }
                            
                            // Sync user to PHP backend and Firestore (async, don't wait)
                            launch(Dispatchers.IO) {
                                try {
                                    user?.let { SyncManager.syncUser(requireContext(), it) }
                                } catch (e: Exception) {
                                    // Log error but don't block navigation
                                    e.printStackTrace()
                                }
                            }
                            
                            // Register FCM token for push notifications
                            launch(Dispatchers.IO) {
                                try {
                                    val tokenResult = FCMTokenHelper.getFCMToken()
                                    tokenResult.fold(
                                        onSuccess = { token ->
                                            FCMTokenHelper.saveTokenToPreferences(requireContext(), token)
                                            // Register token with server
                                            FCMTokenManager.registerToken(requireContext(), firebaseUser.uid, token)
                                        },
                                        onFailure = { e ->
                                            android.util.Log.e("Login", "Failed to get FCM token: ${e.message}")
                                        }
                                    )
                                } catch (e: Exception) {
                                    android.util.Log.e("Login", "Error registering FCM token: ${e.message}")
                                }
                            }
                            
                            // Save login state if checkbox is checked
                            val stayLoggedIn = cbStayLoggedIn?.isChecked ?: false
                            LoginStateManager.saveLoginState(
                                requireContext(),
                                firebaseUser.uid,
                                user?.role?.name ?: UserRole.PLAYER.name,
                                stayLoggedIn
                            )
                            
                            // Navigate based on user's stored role (on main thread)
                            withContext(Dispatchers.Main) {
                                when (user?.role) {
                                    UserRole.ADMIN -> {
                                        startActivity(Intent(requireContext(), AdminDashboardActivity::class.java))
                                        requireActivity().finish()
                                    }
                                    UserRole.GROUNDKEEPER -> {
                                        startActivity(Intent(requireContext(), GroundkeeperDashboardActivity::class.java))
                                        requireActivity().finish()
                                    }
                                    UserRole.PLAYER, null -> {
                                        startActivity(Intent(requireContext(), HomeActivity::class.java))
                                        requireActivity().finish()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Handle any errors
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                view.findViewById<View>(R.id.btnSignIn)?.isEnabled = true
                            }
                        }
                    },
                    onFailure = { exception ->
                        // Handle error
                        val errorMessage = when {
                            exception.message?.contains("email") == true -> "Invalid email address"
                            exception.message?.contains("password") == true -> "Invalid password"
                            exception.message?.contains("user") == true -> "User not found"
                            exception.message?.contains("network") == true -> "Network error. Please check your connection"
                            else -> "Login failed: ${exception.message}"
                        }
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                        view.findViewById<View>(R.id.btnSignIn)?.isEnabled = true
                    }
                )
            }
        }
    }
    private fun launchForgotPassword() {
       startActivity(Intent(requireContext(), ResetPasswordActivity::class.java))
    }
}
