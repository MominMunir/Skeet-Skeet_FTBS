package com.example.smd_fyp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.fragments.GroundkeeperBookingsFragment
import com.example.smd_fyp.fragments.GroundkeeperMyGroundsFragment
import com.example.smd_fyp.fragments.GroundkeeperOverviewFragment
import com.example.smd_fyp.fragments.GroundkeeperSettingsFragment
import com.example.smd_fyp.model.BookingStatus
import com.example.smd_fyp.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GroundkeeperDashboardActivity : AppCompatActivity() {

    private lateinit var tvOverviewTab: TextView
    private lateinit var tvMyGroundsTab: TextView
    private lateinit var tvBookingsTab: TextView
    private lateinit var tvSettingsTab: TextView
    private lateinit var btnBack: View
    private lateinit var tvTodaysBookings: TextView
    private lateinit var tvMonthlyRevenue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groundkeeper_dashboard)

        // Initialize database
        LocalDatabaseHelper.initialize(this)
        
        // Initialize header views
        tvTodaysBookings = findViewById(R.id.tvTodaysBookings)
        tvMonthlyRevenue = findViewById(R.id.tvMonthlyRevenue)

        setupTabs()
        setupBack()
        
        // Load dashboard stats
        loadDashboardStats()
        
        // Load default fragment (Overview)
        if (savedInstanceState == null) {
            loadFragment(GroundkeeperOverviewFragment())
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh stats when activity resumes
        if (::tvTodaysBookings.isInitialized) {
            loadDashboardStats()
        }
    }
    
    private fun loadDashboardStats() {
        lifecycleScope.launch {
            try {
                // Fetch from API first if online
                if (SyncManager.isOnline(this@GroundkeeperDashboardActivity)) {
                    val apiService = ApiClient.getPhpApiService(this@GroundkeeperDashboardActivity)
                    
                    try {
                        android.util.Log.d("GroundkeeperDashboard", "Fetching bookings from API...")
                        val bookingsResponse = withContext(Dispatchers.IO) {
                            apiService.getBookings()
                        }
                        
                        if (bookingsResponse.isSuccessful && bookingsResponse.body() != null) {
                            val apiBookings = bookingsResponse.body()!!
                            android.util.Log.d("GroundkeeperDashboard", "Fetched ${apiBookings.size} bookings from API")
                            
                            withContext(Dispatchers.IO) {
                                apiBookings.forEach { booking ->
                                    LocalDatabaseHelper.saveBooking(booking.copy(synced = true))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GroundkeeperDashboard", "Error fetching bookings from API", e)
                    }
                }
                
                // Load from local database
                val allBookings = withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.getAllBookingsSync()
                }
                
                android.util.Log.d("GroundkeeperDashboard", "Loaded ${allBookings.size} bookings from local DB")
                
                // Get today's date string in yyyy-MM-dd format
                val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
                
                // Today's bookings - simple string comparison
                val todaysBookings = allBookings.filter { booking ->
                    booking.date == todayDateStr
                }
                
                // Monthly bookings - get current month/year
                val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                
                val monthBookings = allBookings.filter { booking ->
                    try {
                        if (booking.date.isNotEmpty()) {
                            val bookingDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(booking.date)
                            if (bookingDate != null) {
                                val cal = Calendar.getInstance().apply { time = bookingDate }
                                cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false
                    }
                }
                
                // Calculate monthly revenue (from confirmed/completed bookings)
                val monthlyRevenue = monthBookings
                    .filter { it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.COMPLETED }
                    .sumOf { it.totalPrice }
                
                android.util.Log.d("GroundkeeperDashboard", "Today's bookings: ${todaysBookings.size}, Monthly revenue: $monthlyRevenue")
                
                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    tvTodaysBookings.text = todaysBookings.size.toString()
                    tvMonthlyRevenue.text = "Rs. ${String.format("%,.0f", monthlyRevenue)}"
                }
            } catch (e: Exception) {
                android.util.Log.e("GroundkeeperDashboard", "Error loading dashboard stats", e)
            }
        }
    }

    private fun setupTabs() {
        tvOverviewTab = findViewById(R.id.tvOverviewTab)
        tvMyGroundsTab = findViewById(R.id.tvMyGroundsTab)
        tvBookingsTab = findViewById(R.id.tvBookingsTab)
        tvSettingsTab = findViewById(R.id.tvSettingsTab)

        tvOverviewTab.setOnClickListener { switchToFragment(GroundkeeperOverviewFragment(), tvOverviewTab) }
        tvMyGroundsTab.setOnClickListener { switchToFragment(GroundkeeperMyGroundsFragment(), tvMyGroundsTab) }
        tvBookingsTab.setOnClickListener { switchToFragment(GroundkeeperBookingsFragment(), tvBookingsTab) }
        tvSettingsTab.setOnClickListener { switchToFragment(GroundkeeperSettingsFragment(), tvSettingsTab) }

        // Set initial active tab
        setActiveTab(tvOverviewTab)
    }

    private fun switchToFragment(fragment: Fragment, activeTab: TextView) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()

        // Update tab states
        setActiveTab(activeTab)
    }

    private fun setActiveTab(activeTab: TextView) {
        // Reset all tabs
        tvOverviewTab.setBackgroundResource(R.drawable.bg_tab_inactive_dashboard)
        tvOverviewTab.setTextColor(getColor(R.color.text_primary))
        tvOverviewTab.setTypeface(null, android.graphics.Typeface.NORMAL)

        tvMyGroundsTab.setBackgroundResource(R.drawable.bg_tab_inactive_dashboard)
        tvMyGroundsTab.setTextColor(getColor(R.color.text_primary))
        tvMyGroundsTab.setTypeface(null, android.graphics.Typeface.NORMAL)

        tvBookingsTab.setBackgroundResource(R.drawable.bg_tab_inactive_dashboard)
        tvBookingsTab.setTextColor(getColor(R.color.text_primary))
        tvBookingsTab.setTypeface(null, android.graphics.Typeface.NORMAL)

        tvSettingsTab.setBackgroundResource(R.drawable.bg_tab_inactive_dashboard)
        tvSettingsTab.setTextColor(getColor(R.color.text_primary))
        tvSettingsTab.setTypeface(null, android.graphics.Typeface.NORMAL)

        // Set active tab
        activeTab.setBackgroundResource(R.drawable.bg_tab_active_dashboard)
        activeTab.setTextColor(getColor(R.color.green_600))
        activeTab.setTypeface(null, android.graphics.Typeface.BOLD)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun setupBack() {
        btnBack = findViewById(R.id.llBack)
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}

