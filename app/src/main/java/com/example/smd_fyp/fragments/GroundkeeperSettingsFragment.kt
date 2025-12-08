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
            // Navigate to edit profile fragment
            val editProfileFragment = com.example.smd_fyp.fragments.EditProfileFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, editProfileFragment)
                .addToBackStack("edit_profile")
                .commit()
        }
        
        view.findViewById<View>(R.id.llPaymentSettings)?.setOnClickListener {
            // Navigate to payment settings (could be a fragment or activity)
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Payment Settings")
                .setMessage("Configure your payment methods and preferences here.")
                .setPositiveButton("OK", null)
                .show()
        }
        
        view.findViewById<View>(R.id.llGroundSettings)?.setOnClickListener {
            // Navigate to ground management
            val myGroundsFragment = com.example.smd_fyp.fragments.GroundkeeperMyGroundsFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, myGroundsFragment)
                .addToBackStack("ground_settings")
                .commit()
        }
        
        view.findViewById<View>(R.id.llHelpCenter)?.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Help Center")
                .setMessage("For help and support, please contact us at support@skeetskeet.com")
                .setPositiveButton("OK", null)
                .show()
        }
        
        view.findViewById<View>(R.id.llContactSupport)?.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Contact Support")
                .setMessage("Email: support@skeetskeet.com\nPhone: +92-XXX-XXXXXXX")
                .setPositiveButton("OK", null)
                .show()
        }
        
        view.findViewById<View>(R.id.llTermsPrivacy)?.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Terms & Privacy")
                .setMessage("By using this app, you agree to our Terms of Service and Privacy Policy.")
                .setPositiveButton("OK", null)
                .show()
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
                // Clear login state and Firebase session
                com.example.smd_fyp.auth.LoginStateManager.clearLoginState(requireContext())
                com.example.smd_fyp.firebase.FirebaseAuthHelper.signOut()
                
                val intent = Intent(requireContext(), AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

