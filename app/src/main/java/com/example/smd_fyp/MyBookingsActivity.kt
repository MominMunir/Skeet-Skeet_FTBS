package com.example.smd_fyp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.ImageView
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.auth.AuthActivity
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.home.BookingAdapter
import com.example.smd_fyp.model.Booking
import com.example.smd_fyp.sync.SyncManager
import com.example.smd_fyp.utils.GlideHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyBookingsActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var rvBookings: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvBookingCount: TextView
    private lateinit var bookingAdapter: BookingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_bookings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bookingsRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize database
        LocalDatabaseHelper.initialize(this)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE)

        // Initialize views
        drawerLayout = findViewById(R.id.drawerLayout)
        rvBookings = findViewById(R.id.rvBookings)
        llEmptyState = findViewById(R.id.llEmptyState)
        tvBookingCount = findViewById(R.id.tvBookingCount)

        // Setup RecyclerView
        rvBookings.layoutManager = LinearLayoutManager(this)
        bookingAdapter = BookingAdapter(
            onReceiptClick = { booking ->
                // Show receipt activity
                val intent = android.content.Intent(this, com.example.smd_fyp.ReceiptActivity::class.java)
                intent.putExtra("booking_id", booking.id)
                startActivity(intent)
            },
            onReviewClick = { booking ->
                // Show review dialog using fragment manager
                val reviewDialog = com.example.smd_fyp.ReviewDialog.newInstance(
                    booking,
                    onReviewSubmitted = { booking, rating, reviewText ->
                        // Review is already saved in ReviewDialog
                        android.util.Log.d("MyBookingsActivity", "Review submitted: Rating=$rating, Review=$reviewText")
                    }
                )
                // Use supportFragmentManager since we're in an Activity
                reviewDialog.show(supportFragmentManager, "ReviewDialog")
            }
        )
        rvBookings.adapter = bookingAdapter

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

        // Load bookings from API
        loadBookings()
    }

    private fun loadDrawerProfileData() {
        lifecycleScope.launch {
            try {
                val currentUser = FirebaseAuthHelper.getCurrentUser()
                if (currentUser == null) {
                    // Fallback to SharedPreferences
                    val fullName = sharedPreferences.getString("full_name", "User") ?: "User"
                    val email = sharedPreferences.getString("email", "email@example.com") ?: "email@example.com"
                    
                    findViewById<TextView>(R.id.tvUserName)?.text = fullName
                    findViewById<TextView>(R.id.tvUserEmail)?.text = email
                    
                    // Load profile image
                    val imageUrl = sharedPreferences.getString("profile_image_url", null)
                    if (!imageUrl.isNullOrEmpty()) {
                        val normalizedUrl = ApiClient.normalizeImageUrl(this@MyBookingsActivity, imageUrl)
                        GlideHelper.loadImage(
                            context = this@MyBookingsActivity,
                            imageUrl = normalizedUrl,
                            imageView = findViewById(R.id.ivProfilePicture),
                            placeholder = R.drawable.ic_person,
                            errorDrawable = R.drawable.ic_person,
                            tag = "Drawer",
                            useCircleCrop = true
                        )
                    } else {
                        findViewById<ImageView>(R.id.ivProfilePicture)?.setImageResource(R.drawable.ic_person)
                    }
                    return@launch
                }
                
                // Get user from database
                val user = withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.getUser(currentUser.uid)
                }
                
                withContext(Dispatchers.Main) {
                    if (user != null) {
                        findViewById<TextView>(R.id.tvUserName)?.text = user.fullName
                        findViewById<TextView>(R.id.tvUserEmail)?.text = user.email
                        
                        // Load profile image
                        user.profileImageUrl?.let { imageUrl ->
                            if (imageUrl.isNotBlank()) {
                                val normalizedUrl = ApiClient.normalizeImageUrl(this@MyBookingsActivity, imageUrl)
                                GlideHelper.loadImage(
                                    context = this@MyBookingsActivity,
                                    imageUrl = normalizedUrl,
                                    imageView = findViewById(R.id.ivProfilePicture),
                                    placeholder = R.drawable.ic_person,
                                    errorDrawable = R.drawable.ic_person,
                                    tag = "Drawer",
                                    useCircleCrop = true
                                )
                            } else {
                                findViewById<ImageView>(R.id.ivProfilePicture)?.setImageResource(R.drawable.ic_person)
                            }
                        } ?: run {
                            findViewById<ImageView>(R.id.ivProfilePicture)?.setImageResource(R.drawable.ic_person)
                        }
                    } else {
                        // Fallback to SharedPreferences
                        val fullName = sharedPreferences.getString("full_name", "User") ?: "User"
                        val email = sharedPreferences.getString("email", "email@example.com") ?: "email@example.com"
                        
                        findViewById<TextView>(R.id.tvUserName)?.text = fullName
                        findViewById<TextView>(R.id.tvUserEmail)?.text = email
                        findViewById<ImageView>(R.id.ivProfilePicture)?.setImageResource(R.drawable.ic_person)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to SharedPreferences on error
                val fullName = sharedPreferences.getString("full_name", "User") ?: "User"
                val email = sharedPreferences.getString("email", "email@example.com") ?: "email@example.com"
                
                findViewById<TextView>(R.id.tvUserName)?.text = fullName
                findViewById<TextView>(R.id.tvUserEmail)?.text = email
                findViewById<ImageView>(R.id.ivProfilePicture)?.setImageResource(R.drawable.ic_person)
            }
        }
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
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("selected_tab", "favorites")
            startActivity(intent)
        }

        // Settings
        findViewById<View>(R.id.menuSettings)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.putExtra("selected_tab", "settings")
            startActivity(intent)
        }

        // Help & Support
        findViewById<View>(R.id.menuHelp)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showHelpSupportDialog()
        }

        // Logout
        findViewById<View>(R.id.menuLogout)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            showLogoutDialog()
        }
    }

    private fun loadBookings() {
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                // First, observe local database (offline support)
                LocalDatabaseHelper.getBookingsByUser(currentUser.uid)?.collect { localBookings ->
                    bookingAdapter.updateItems(localBookings)
                    tvBookingCount.text = "${localBookings.size} bookings"
                    llEmptyState.visibility = if (localBookings.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Fetch from API if online
        lifecycleScope.launch {
            if (SyncManager.isOnline(this@MyBookingsActivity)) {
                try {
                    val apiService = ApiClient.getPhpApiService(this@MyBookingsActivity)
                    val response = apiService.getBookings(currentUser.uid)
                    
                    if (response.isSuccessful && response.body() != null) {
                        val apiBookings = response.body()!!
                        
                        // Save to local database
                        withContext(Dispatchers.IO) {
                            apiBookings.forEach { booking ->
                                LocalDatabaseHelper.saveBooking(booking.copy(synced = true))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Don't show error, just use local data
                }
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                // Clear login state and Firebase session
                com.example.smd_fyp.auth.LoginStateManager.clearLoginState(this)
                com.example.smd_fyp.firebase.FirebaseAuthHelper.signOut()
                
                val intent = Intent(this, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showHelpSupportDialog() {
        AlertDialog.Builder(this)
            .setTitle("Help & Support")
            .setMessage("For assistance, please contact us:\n\n" +
                    "Email: support@skeetskeet.com\n" +
                    "Phone: +92 300 1234567\n\n" +
                    "Our support team is available 24/7 to help you.")
            .setPositiveButton("OK", null)
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

