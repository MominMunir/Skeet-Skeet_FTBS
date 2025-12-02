package com.example.smd_fyp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.smd_fyp.R
import com.example.smd_fyp.auth.AuthActivity

class GroundkeeperSettingsFragment : Fragment() {

    private lateinit var switchPushNotifications: Switch
    private lateinit var switchBookingAlerts: Switch
    private lateinit var switchRevenueReports: Switch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_groundkeeper_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        switchPushNotifications = view.findViewById(R.id.switchPushNotifications)
        switchBookingAlerts = view.findViewById(R.id.switchBookingAlerts)
        switchRevenueReports = view.findViewById(R.id.switchRevenueReports)
        
        // Load saved preferences
        val prefs = requireContext().getSharedPreferences("groundkeeper_settings", 0)
        switchPushNotifications.isChecked = prefs.getBoolean("push_notifications", true)
        switchBookingAlerts.isChecked = prefs.getBoolean("booking_alerts", true)
        switchRevenueReports.isChecked = prefs.getBoolean("revenue_reports", true)
        
        // Setup click listeners
        view.findViewById<View>(R.id.llEditProfile)?.setOnClickListener {
            // TODO: Navigate to edit profile
            Toast.makeText(requireContext(), "Edit Profile", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.llPaymentSettings)?.setOnClickListener {
            // TODO: Payment settings
            Toast.makeText(requireContext(), "Payment Settings", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.llGroundSettings)?.setOnClickListener {
            // TODO: Ground settings
            Toast.makeText(requireContext(), "Ground Settings", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.llHelpCenter)?.setOnClickListener {
            // TODO: Open help center
            Toast.makeText(requireContext(), "Help Center", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.llContactSupport)?.setOnClickListener {
            // TODO: Contact support
            Toast.makeText(requireContext(), "Contact Support", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.llTermsPrivacy)?.setOnClickListener {
            // TODO: Show terms and privacy
            Toast.makeText(requireContext(), "Terms & Privacy", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.btnLogout)?.setOnClickListener {
            showLogoutDialog()
        }
        
        // Save switch states
        switchPushNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("push_notifications", isChecked).apply()
        }
        
        switchBookingAlerts.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("booking_alerts", isChecked).apply()
        }
        
        switchRevenueReports.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("revenue_reports", isChecked).apply()
        }
    }
    
    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                // Clear user session/data
                val intent = Intent(requireContext(), AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

