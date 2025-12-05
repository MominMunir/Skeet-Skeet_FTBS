package com.example.smd_fyp.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.smd_fyp.AdminDashboardActivity
import com.example.smd_fyp.GroundkeeperDashboardActivity
import com.example.smd_fyp.HomeActivity
import com.example.smd_fyp.R
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.model.User
import com.example.smd_fyp.model.UserRole
import com.example.smd_fyp.sync.SyncManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignupFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_signup, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Tabs: Login(inactive) | Sign Up(active)
        view.findViewById<TextView>(R.id.tvLoginTab)?.setOnClickListener {
            findNavController().navigate(R.id.action_signup_to_login)
        }
        // Back
        view.findViewById<View>(R.id.llBack)?.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Create Account button
        val etFullName = view.findViewById<TextInputEditText>(R.id.etFullName)
        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val spinner = view.findViewById<Spinner>(R.id.spinnerRegisterAs)
        
        view.findViewById<View>(R.id.btnCreateAccount)?.setOnClickListener {
            val fullName = etFullName?.text?.toString()?.trim() ?: ""
            val email = etEmail?.text?.toString()?.trim() ?: ""
            val password = etPassword?.text?.toString() ?: ""
            val selected = spinner?.selectedItem?.toString()?.trim() ?: ""
            
            // Validate inputs
            if (fullName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your full name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty() || password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), "Please select a role", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Show loading
            view.findViewById<View>(R.id.btnCreateAccount)?.isEnabled = false
            
            // Create account with Firebase
            lifecycleScope.launch {
                val result = FirebaseAuthHelper.createUserWithEmailPassword(email, password, fullName)
                result.fold(
                    onSuccess = { firebaseUser ->
                        try {
                            // Successfully created account
                            // Initialize database
                            LocalDatabaseHelper.initialize(requireContext())
                            
                            // Map selected role to UserRole enum
                            val adminLabel = getString(R.string.admin)
                            val playerLabel = getString(R.string.player)
                            val groundkeeperLabel = getString(R.string.groundkeeper)
                            
                            val userRole = when {
                                selected.equals(adminLabel, ignoreCase = true) -> UserRole.ADMIN
                                selected.equals(groundkeeperLabel, ignoreCase = true) -> UserRole.GROUNDKEEPER
                                selected.equals(playerLabel, ignoreCase = true) -> UserRole.PLAYER
                                else -> UserRole.PLAYER
                            }
                            
                            // Create user object
                            val user = User(
                                id = firebaseUser.uid,
                                email = firebaseUser.email ?: email,
                                fullName = fullName,
                                role = userRole,
                                createdAt = System.currentTimeMillis()
                            )
                            
                            // Sync user to PHP backend and Firestore (async, don't wait)
                            launch(Dispatchers.IO) {
                                try {
                                    SyncManager.syncUser(requireContext(), user)
                                } catch (e: Exception) {
                                    // Log error but don't block navigation
                                    e.printStackTrace()
                                }
                            }
                            
                            // Navigate based on role (on main thread)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Account created successfully!", Toast.LENGTH_SHORT).show()
                                
                                when {
                                    selected.equals(adminLabel, ignoreCase = true) -> {
                                        startActivity(Intent(requireContext(), AdminDashboardActivity::class.java))
                                        requireActivity().finish()
                                    }
                                    selected.equals(groundkeeperLabel, ignoreCase = true) -> {
                                        startActivity(Intent(requireContext(), GroundkeeperDashboardActivity::class.java))
                                        requireActivity().finish()
                                    }
                                    selected.equals(playerLabel, ignoreCase = true) -> {
                                        startActivity(Intent(requireContext(), HomeActivity::class.java))
                                        requireActivity().finish()
                                    }
                                    else -> {
                                        Toast.makeText(requireContext(), "Please select a valid role", Toast.LENGTH_SHORT).show()
                                        view.findViewById<View>(R.id.btnCreateAccount)?.isEnabled = true
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Handle any errors
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                view.findViewById<View>(R.id.btnCreateAccount)?.isEnabled = true
                            }
                        }
                    },
                    onFailure = { exception ->
                        // Handle error
                        val errorMessage = when {
                            exception.message?.contains("email") == true -> "Email is already in use or invalid"
                            exception.message?.contains("password") == true -> "Password is too weak"
                            exception.message?.contains("network") == true -> "Network error. Please check your connection"
                            else -> "Registration failed: ${exception.message}"
                        }
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                        view.findViewById<View>(R.id.btnCreateAccount)?.isEnabled = true
                    }
                )
            }
        }
    }
}
