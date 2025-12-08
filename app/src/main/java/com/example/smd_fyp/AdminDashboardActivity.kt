package com.example.smd_fyp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.fragments.AdminAnalyticsFragment
import com.example.smd_fyp.fragments.AdminGroundsFragment
import com.example.smd_fyp.fragments.AdminOverviewFragment
import com.example.smd_fyp.fragments.AdminUsersFragment
import com.example.smd_fyp.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var tvOverviewTab: TextView
    private lateinit var tvGroundsTab: TextView
    private lateinit var tvUsersTab: TextView
    private lateinit var tvAnalyticsTab: TextView
    private lateinit var btnBack: View
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvActiveGrounds: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Initialize database
        LocalDatabaseHelper.initialize(this)

        setupTabs()
        setupBack()
        loadDashboardStats()
        
        // Load default fragment (Overview)
        if (savedInstanceState == null) {
            loadFragment(AdminOverviewFragment())
        }
    }
    
    private fun loadDashboardStats() {
        // Initialize views
        tvTotalUsers = findViewById(R.id.tvTotalUsers)
        tvActiveGrounds = findViewById(R.id.tvActiveGrounds)
        
        lifecycleScope.launch {
            try {
                // Load users from local DB
                LocalDatabaseHelper.getAllUsers()?.collect { users ->
                    tvTotalUsers.text = users.size.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        lifecycleScope.launch {
            try {
                // Load grounds from local DB
                LocalDatabaseHelper.getAllGrounds()?.collect { grounds ->
                    val activeGrounds = grounds.count { it.available }
                    tvActiveGrounds.text = activeGrounds.toString()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Fetch from API if online
        lifecycleScope.launch {
            if (SyncManager.isOnline(this@AdminDashboardActivity)) {
                try {
                    val apiService = ApiClient.getPhpApiService(this@AdminDashboardActivity)
                    
                    // Fetch users
                    try {
                        val usersResponse = apiService.getUsers()
                        if (usersResponse.isSuccessful && usersResponse.body() != null) {
                            withContext(Dispatchers.IO) {
                                usersResponse.body()!!.forEach { user ->
                                    LocalDatabaseHelper.saveUser(user.copy(synced = true))
                                }
                            }
                            android.util.Log.d("AdminDashboard", "Fetched ${usersResponse.body()!!.size} users from API")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AdminDashboard", "Error fetching users from API", e)
                    }
                    
                    // Fetch grounds
                    val groundsResponse = apiService.getGrounds()
                    if (groundsResponse.isSuccessful && groundsResponse.body() != null) {
                        withContext(Dispatchers.IO) {
                            LocalDatabaseHelper.saveGrounds(groundsResponse.body()!!.map { it.copy(synced = true) })
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun setupTabs() {
        tvOverviewTab = findViewById(R.id.tvOverviewTab)
        tvGroundsTab = findViewById(R.id.tvGroundsTab)
        tvUsersTab = findViewById(R.id.tvUsersTab)
        tvAnalyticsTab = findViewById(R.id.tvAnalyticsTab)


        tvOverviewTab.setOnClickListener { switchToFragment(AdminOverviewFragment(), tvOverviewTab) }
        tvGroundsTab.setOnClickListener { switchToFragment(AdminGroundsFragment(), tvGroundsTab) }
        tvUsersTab.setOnClickListener { switchToFragment(AdminUsersFragment(), tvUsersTab) }
        tvAnalyticsTab.setOnClickListener { switchToFragment(AdminAnalyticsFragment(), tvAnalyticsTab) }

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

        tvGroundsTab.setBackgroundResource(R.drawable.bg_tab_inactive_dashboard)
        tvGroundsTab.setTextColor(getColor(R.color.text_primary))
        tvGroundsTab.setTypeface(null, android.graphics.Typeface.NORMAL)

        tvUsersTab.setBackgroundResource(R.drawable.bg_tab_inactive_dashboard)
        tvUsersTab.setTextColor(getColor(R.color.text_primary))
        tvUsersTab.setTypeface(null, android.graphics.Typeface.NORMAL)

        tvAnalyticsTab.setBackgroundResource(R.drawable.bg_tab_inactive_dashboard)
        tvAnalyticsTab.setTextColor(getColor(R.color.text_primary))
        tvAnalyticsTab.setTypeface(null, android.graphics.Typeface.NORMAL)

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

