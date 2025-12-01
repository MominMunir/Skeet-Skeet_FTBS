package com.example.smd_fyp.auth

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smd_fyp.R
import com.example.smd_fyp.auth.LoginFragment

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

        findViewById<TextView>(R.id.llBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
         }

        findViewById<TextView>(R.id.tvRememberPassword).setOnClickListener {
           startActivity(Intent(this, LoginFragment::class.java))
        }

        val email = findViewById<TextView>(R.id.etEmail)
        findViewById<TextView>(R.id.btnSendResetLink).setOnClickListener {}
    }
}