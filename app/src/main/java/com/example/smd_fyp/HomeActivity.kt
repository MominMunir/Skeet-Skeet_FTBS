package com.example.smd_fyp

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
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
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.auth.AuthActivity
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.fragments.NotificationsFragment
import com.example.smd_fyp.home.GroundAdapter
import com.example.smd_fyp.model.GroundApi
import com.example.smd_fyp.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var llFilterOptions: LinearLayout
    private lateinit var tvLocationFilter: TextView
    private lateinit var tvPriceFilter: TextView
    private lateinit var fragmentContainerNotifications: FragmentContainerView
    private var isFiltersVisible = false
    private var isNotificationsVisible = false

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
        
        val adapter = GroundAdapter(mutableListOf<GroundApi>()) { ground ->
            // Handle ground click - navigate to ground details
            // TODO: Navigate to ground details screen
            Toast.makeText(this, "Clicked: ${ground.name}", Toast.LENGTH_SHORT).show()
        }
        rv.adapter = adapter

        // Load grounds from API
        loadGrounds(adapter)

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

        // Setup Menu button click listener
        findViewById<View>(R.id.btnMenu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Setup Navigation Drawer menu items
        setupDrawerMenu()

        // Setup back button handling
        setupBackPressHandler()

        // TODO: Setup search functionality
        // TODO: Setup other top bar button click listeners
    }
    
    private fun loadGrounds(adapter: GroundAdapter) {
        lifecycleScope.launch {
            try {
                // First, observe local database (offline support)
                LocalDatabaseHelper.getAllGrounds()?.collect { localGrounds: List<GroundApi> ->
                    val availableGrounds: List<GroundApi> = localGrounds.filter { it.available }
                    adapter.updateItems(availableGrounds)
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

    private fun toggleFilters() {
        isFiltersVisible = !isFiltersVisible
        llFilterOptions.visibility = if (isFiltersVisible) View.VISIBLE else View.GONE
    }

    private fun showLocationFilterDialog() {
        val locations = arrayOf(
            "All Locations",
            "DHA Phase 5, Lahore",
            "Johar Town, Lahore",
            "Gulberg, Lahore",
            "Model Town, Lahore",
            "Faisalabad",
            "Karachi",
            "Islamabad"
        )

        AlertDialog.Builder(this)
            .setTitle("Select Location")
            .setItems(locations) { _, which ->
                tvLocationFilter.text = locations[which]
                // TODO: Apply location filter to grounds list
                Toast.makeText(this, "Filtered by: ${locations[which]}", Toast.LENGTH_SHORT).show()
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
                tvPriceFilter.text = prices[which]
                // TODO: Apply price filter to grounds list
                Toast.makeText(this, "Filtered by: ${prices[which]}", Toast.LENGTH_SHORT).show()
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
                } else if (isNotificationsVisible) {
                    hideNotifications()
                } else {
                    finish()
                }
            }
        })
    }
}
