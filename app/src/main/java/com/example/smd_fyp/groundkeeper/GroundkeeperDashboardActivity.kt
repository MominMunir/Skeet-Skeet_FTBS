package com.example.smd_fyp.groundkeeper

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.smd_fyp.R
import com.example.smd_fyp.groundkeeper.fragments.GroundkeeperBookingsFragment
import com.example.smd_fyp.groundkeeper.fragments.GroundkeeperMyGroundsFragment
import com.example.smd_fyp.groundkeeper.fragments.GroundkeeperOverviewFragment
import com.example.smd_fyp.groundkeeper.fragments.GroundkeeperSettingsFragment

class GroundkeeperDashboardActivity : AppCompatActivity() {

    private lateinit var tvOverviewTab: TextView
    private lateinit var tvMyGroundsTab: TextView
    private lateinit var tvBookingsTab: TextView
    private lateinit var tvSettingsTab: TextView
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groundkeeper_dashboard)

        setupTabs()
        setupBack()
        
        // Load default fragment (Overview)
        if (savedInstanceState == null) {
            loadFragment(GroundkeeperOverviewFragment())
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
        btnBack = findViewById(R.id.btnBack)
        btnBack?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}


