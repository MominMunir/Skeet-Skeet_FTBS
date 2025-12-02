package com.example.smd_fyp.player

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.smd_fyp.R
import com.example.smd_fyp.auth.AuthActivity

class MyBookingsActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_bookings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bookingsRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE)

        // Initialize views
        drawerLayout = findViewById(R.id.drawerLayout)

        // Setup Menu button click listener
        findViewById<View>(R.id.btnMenu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Load user profile data in drawer
        loadDrawerProfileData()

        // Setup Navigation Drawer menu items
        setupDrawerMenu()

        // Setup back button handling
        setupBackPressHandler()

        // Setup booking card click listeners
        setupBookingCards()
    }

    private fun loadDrawerProfileData() {
        val fullName = sharedPreferences.getString("full_name", "Ahmed Khan") ?: "Ahmed Khan"
        val email = sharedPreferences.getString("email", "ahmed@example.com") ?: "ahmed@example.com"
        
        findViewById<TextView>(R.id.tvUserName)?.text = fullName
        findViewById<TextView>(R.id.tvUserEmail)?.text = email
    }

    private fun setupDrawerMenu() {
        // Home
        findViewById<View>(R.id.menuHome)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // My Bookings
        findViewById<View>(R.id.menuMyBookings)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            // Already on My Bookings, just close drawer
        }

        // Favorites
        findViewById<View>(R.id.menuFavorites)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            // TODO: Navigate to Favorites screen
            Toast.makeText(this, "Favorites", Toast.LENGTH_SHORT).show()
        }

        // Settings
        findViewById<View>(R.id.menuSettings)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            // TODO: Navigate to Settings screen
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
        }

        // Help & Support
        findViewById<View>(R.id.menuHelp)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            // TODO: Navigate to Help & Support screen
            Toast.makeText(this, "Help & Support", Toast.LENGTH_SHORT).show()
        }

        // Logout
        findViewById<View>(R.id.menuLogout)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showLogoutDialog()
        }
    }

    private fun setupBookingCards() {
        // Receipt buttons
        findViewById<View>(R.id.btnReceipt1)?.setOnClickListener {
            // TODO: Show receipt
            Toast.makeText(this, "Receipt", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<View>(R.id.btnReceipt2)?.setOnClickListener {
            // TODO: Show receipt
            Toast.makeText(this, "Receipt", Toast.LENGTH_SHORT).show()
        }
        
        // Review button
        findViewById<View>(R.id.btnReview1)?.setOnClickListener {
            // TODO: Show review dialog
            Toast.makeText(this, "Review", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                // TODO: Clear user session/data
                val intent = Intent(this, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // Navigate back to Home
                    val intent = Intent(this@MyBookingsActivity, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            }
        })
    }
}


