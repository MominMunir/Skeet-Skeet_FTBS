package com.example.smd_fyp

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.smd_fyp.fragments.AdminAnalyticsFragment
import com.example.smd_fyp.fragments.AdminGroundsFragment
import com.example.smd_fyp.fragments.AdminOverviewFragment
import com.example.smd_fyp.fragments.AdminUsersFragment

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var tvOverviewTab: TextView
    private lateinit var tvGroundsTab: TextView
    private lateinit var tvUsersTab: TextView
    private lateinit var tvAnalyticsTab: TextView
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        setupTabs()
    setupBack()
        
        // Load default fragment (Overview)
        if (savedInstanceState == null) {
            loadFragment(AdminOverviewFragment())
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

