package com.example.smd_fyp

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import android.content.SharedPreferences
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.auth.AuthActivity
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.utils.GlideHelper
import com.example.smd_fyp.fragments.NotificationsFragment
import com.example.smd_fyp.home.GroundAdapter
import com.example.smd_fyp.model.GroundApi
import com.example.smd_fyp.sync.AutoBookingStatusWorker
import com.example.smd_fyp.sync.SyncManager
import com.example.smd_fyp.sync.WeatherNotificationWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class HomeActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var llFilterOptions: LinearLayout
    private lateinit var tvLocationFilter: TextView
    private lateinit var tvPriceFilter: TextView
    private lateinit var fragmentContainerNotifications: FragmentContainerView
    private var isFiltersVisible = false
    private var isNotificationsVisible = false
    
    // Filter state
    private var selectedLocation: String? = null
    private var selectedPriceRange: Pair<Double?, Double?>? = null
    private var allGrounds: List<GroundApi> = emptyList()
    private lateinit var adapter: GroundAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize database
        LocalDatabaseHelper.initialize(this)
        
        // Initialize views
        drawerLayout = findViewById(R.id.drawerLayout)
        llFilterOptions = findViewById(R.id.llFilterOptions)
        tvLocationFilter = findViewById(R.id.tvLocationFilter)
        tvPriceFilter = findViewById(R.id.tvPriceFilter)
        fragmentContainerNotifications = findViewById(R.id.fragmentContainerNotifications)

        // Setup RecyclerView
        val rv = findViewById<RecyclerView>(R.id.rvFeaturedGrounds)
        rv.layoutManager = LinearLayoutManager(this)
        
        adapter = GroundAdapter(mutableListOf<GroundApi>()) { ground ->
            // Navigate to ground details screen
            val intent = Intent(this, GroundDetailActivity::class.java).apply {
                putExtra("ground_id", ground.id)
            }
            startActivity(intent)
        }
        rv.adapter = adapter

        // Load grounds from API
        loadGrounds()
        
        // Schedule booking status auto-updates
        scheduleBookingStatusWorker()
        
        // Schedule weather notification checks
        scheduleWeatherNotificationWorker()

        // Setup Filters button click listener
        findViewById<View>(R.id.btnFilters)?.setOnClickListener {
            toggleFilters()
        }

        // Setup Location filter click listener
        findViewById<View>(R.id.llLocationFilter)?.setOnClickListener {
            showLocationFilterDialog()
        }

        // Setup Price filter click listener
        findViewById<View>(R.id.llPriceFilter)?.setOnClickListener {
            showPriceFilterDialog()
        }

        // Setup Notifications button click listener
        findViewById<View>(R.id.btnNotifications)?.setOnClickListener {
            toggleNotifications()
        }

        // Setup Profile button click listener
        findViewById<View>(R.id.btnProfile)?.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }

        // Weather button -> WeatherActivity
        findViewById<View>(R.id.btnCoin)?.setOnClickListener {
            val intent = Intent(this, WeatherActivity::class.java)
            startActivity(intent)
        }

        // Setup Menu button click listener
        findViewById<View>(R.id.btnMenu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Setup Navigation Drawer menu items
        setupDrawerMenu()

        // Load drawer profile data
        loadDrawerProfileData()

        // Setup back button handling
        setupBackPressHandler()

        // TODO: Setup search functionality
        // TODO: Setup other top bar button click listeners
    }
    
    private fun loadDrawerProfileData() {
        lifecycleScope.launch {
            try {
                val currentUser = FirebaseAuthHelper.getCurrentUser()
                if (currentUser == null) {
                    // Fallback to SharedPreferences
                    val sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE)
                    val fullName = sharedPreferences.getString("full_name", "User") ?: "User"
                    val email = sharedPreferences.getString("email", "email@example.com") ?: "email@example.com"
                    
                    findViewById<TextView>(R.id.tvUserName)?.text = fullName
                    findViewById<TextView>(R.id.tvUserEmail)?.text = email
                    
                    // Load profile image
                    val imageUrl = sharedPreferences.getString("profile_image_url", null)
                    if (!imageUrl.isNullOrEmpty()) {
                        val normalizedUrl = ApiClient.normalizeImageUrl(this@HomeActivity, imageUrl)
                        GlideHelper.loadImage(
                            context = this@HomeActivity,
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
                                val normalizedUrl = ApiClient.normalizeImageUrl(this@HomeActivity, imageUrl)
                                GlideHelper.loadImage(
                                    context = this@HomeActivity,
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
                        val sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE)
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
                val sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE)
                val fullName = sharedPreferences.getString("full_name", "User") ?: "User"
                val email = sharedPreferences.getString("email", "email@example.com") ?: "email@example.com"
                
                findViewById<TextView>(R.id.tvUserName)?.text = fullName
                findViewById<TextView>(R.id.tvUserEmail)?.text = email
                findViewById<ImageView>(R.id.ivProfilePicture)?.setImageResource(R.drawable.ic_person)
            }
        }
    }
    
    private fun loadGrounds() {
        lifecycleScope.launch {
            try {
                // First, observe local database (offline support)
                LocalDatabaseHelper.getAllGrounds()?.collect { localGrounds: List<GroundApi> ->
                    allGrounds = localGrounds.filter { it.available }
                    applyFilters()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Fetch from API if online
        lifecycleScope.launch {
            if (SyncManager.isOnline(this@HomeActivity)) {
                try {
                    val apiService = ApiClient.getPhpApiService(this@HomeActivity)
                    val response = apiService.getGrounds()
                    
                    if (response.isSuccessful && response.body() != null) {
                        val apiGrounds = response.body()!!
                        
                        // Save to local database
                        withContext(Dispatchers.IO) {
                            LocalDatabaseHelper.saveGrounds(apiGrounds.map { it.copy(synced = true) })
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Don't show error toast, just use local data
                }
            }
        }
    }
    
    private fun applyFilters() {
        var filtered = allGrounds
        
        // Apply location filter - use keyword/substring matching
        selectedLocation?.let { location ->
            if (location != "All Locations") {
                val searchKeyword = location.trim().lowercase()
                filtered = filtered.filter { ground ->
                    val groundLocation = ground.location?.trim()?.lowercase() ?: ""
                    groundLocation.contains(searchKeyword)
                }
            }
        }
        
        // Apply price filter
        selectedPriceRange?.let { (minPrice, maxPrice) ->
            filtered = filtered.filter { ground ->
                val price = ground.price
                val matchesMin = minPrice == null || price >= minPrice
                val matchesMax = maxPrice == null || price <= maxPrice
                matchesMin && matchesMax
            }
        }
        
        adapter.updateItems(filtered)
    }

    private fun scheduleBookingStatusWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val work = PeriodicWorkRequestBuilder<AutoBookingStatusWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "auto-booking-status",
            ExistingPeriodicWorkPolicy.UPDATE,
            work
        )
    }
    
    private fun scheduleWeatherNotificationWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Run once per day (24 hours) to check tomorrow's weather
        val work = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weather-notification",
            ExistingPeriodicWorkPolicy.UPDATE,
            work
        )
    }

    private fun toggleFilters() {
        isFiltersVisible = !isFiltersVisible
        llFilterOptions.visibility = if (isFiltersVisible) View.VISIBLE else View.GONE
    }

    private fun showLocationFilterDialog() {
        val locations = arrayOf(
            "All Locations",
            "Islamabad",
            "Lahore",
            "Karachi"
        )

        AlertDialog.Builder(this)
            .setTitle("Select Location")
            .setItems(locations) { _, which ->
                val selected = locations[which]
                tvLocationFilter.text = selected
                selectedLocation = if (selected == "All Locations") null else selected
                applyFilters()
            }
            .show()
    }

    private fun showPriceFilterDialog() {
        val prices = arrayOf(
            "All Prices",
            "Under Rs. 2000/hr",
            "Rs. 2000 - 3000/hr",
            "Rs. 3000 - 4000/hr",
            "Rs. 4000 - 5000/hr",
            "Above Rs. 5000/hr"
        )

        AlertDialog.Builder(this)
            .setTitle("Select Price Range")
            .setItems(prices) { _, which ->
                val selected = prices[which]
                tvPriceFilter.text = selected
                
                // Parse price range
                selectedPriceRange = when (which) {
                    0 -> null // All Prices
                    1 -> Pair(null, 1999.0) // Under Rs. 2000/hr
                    2 -> Pair(2000.0, 3000.0) // Rs. 2000 - 3000/hr
                    3 -> Pair(3000.0, 4000.0) // Rs. 3000 - 4000/hr
                    4 -> Pair(4000.0, 5000.0) // Rs. 4000 - 5000/hr
                    5 -> Pair(5000.0, null) // Above Rs. 5000/hr
                    else -> null
                }
                applyFilters()
            }
            .show()
    }

    private fun toggleNotifications() {
        isNotificationsVisible = !isNotificationsVisible
        
        if (isNotificationsVisible) {
            showNotifications()
        } else {
            hideNotifications()
        }
    }

    fun showNotifications() {
        isNotificationsVisible = true
        fragmentContainerNotifications.visibility = View.VISIBLE
        if (supportFragmentManager.findFragmentById(R.id.fragmentContainerNotifications) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerNotifications, NotificationsFragment())
                .commit()
        }
    }

    fun hideNotifications() {
        isNotificationsVisible = false
        fragmentContainerNotifications.visibility = View.GONE
    }

    private fun setupDrawerMenu() {
        // Home
        findViewById<View>(R.id.menuHome)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            // Already on home, just close drawer
        }

        // My Bookings
        findViewById<View>(R.id.menuMyBookings)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            val intent = Intent(this, MyBookingsActivity::class.java)
            startActivity(intent)
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

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                // Clear login state and Firebase session
                com.example.smd_fyp.auth.LoginStateManager.clearLoginState(this)
                com.example.smd_fyp.firebase.FirebaseAuthHelper.signOut()
                
                val intent = Intent(this, com.example.smd_fyp.auth.AuthActivity::class.java)
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
                } else if (isNotificationsVisible) {
                    hideNotifications()
                } else {
                    finish()
                }
            }
        })
    }
}
