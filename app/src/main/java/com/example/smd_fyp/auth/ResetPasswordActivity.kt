package com.example.smd_fyp.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.smd_fyp.R
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ResetPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<LinearLayout>(R.id.llBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        findViewById<TextView>(R.id.tvRememberPassword).setOnClickListener {
            startActivity(Intent(this, AuthActivity::class.java))
        }

        val email = findViewById<TextInputEditText>(R.id.etEmail)
        val btnSendResetLink = findViewById<Button>(R.id.btnSendResetLink)
        
        btnSendResetLink.setOnClickListener {
            val emailText = email.text?.toString()?.trim() ?: ""
            
            if (emailText.isEmpty()) {
                Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Disable button to prevent multiple clicks
            btnSendResetLink.isEnabled = false
            btnSendResetLink.text = "Sending..."
            
            // Send password reset email
            lifecycleScope.launch {
                val result = FirebaseAuthHelper.sendPasswordResetEmail(emailText)
                result.fold(
                    onSuccess = {
                        Toast.makeText(
                            this@ResetPasswordActivity,
                            "Password reset email sent! Please check your inbox.",
                            Toast.LENGTH_LONG
                        ).show()
                        // Navigate back to login
                        startActivity(Intent(this@ResetPasswordActivity, AuthActivity::class.java))
                        finish()
                    },
                    onFailure = { exception ->
                        val errorMessage = when {
                            exception.message?.contains("email") == true -> "Email address not found"
                            exception.message?.contains("network") == true -> "Network error. Please check your connection"
                            else -> "Failed to send reset email: ${exception.message}"
                        }
                        Toast.makeText(this@ResetPasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
                        btnSendResetLink.isEnabled = true
                        btnSendResetLink.text = getString(R.string.send_reset_link)
                    }
                )
            }
        }
    }
}