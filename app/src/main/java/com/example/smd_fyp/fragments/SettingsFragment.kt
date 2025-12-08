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
import com.example.smd_fyp.fragments.EditProfileFragment

class SettingsFragment : Fragment() {

    private lateinit var switchPushNotifications: Switch
    private lateinit var switchWeatherAlerts: Switch
    private lateinit var switchBookingReminders: Switch

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        switchPushNotifications = view.findViewById(R.id.switchPushNotifications)
        switchWeatherAlerts = view.findViewById(R.id.switchWeatherAlerts)
        switchBookingReminders = view.findViewById(R.id.switchBookingReminders)
        
        // Load saved preferences
        val prefs = requireContext().getSharedPreferences("user_settings", 0)
        switchPushNotifications.isChecked = prefs.getBoolean("push_notifications", true)
        switchWeatherAlerts.isChecked = prefs.getBoolean("weather_alerts", true)
        switchBookingReminders.isChecked = prefs.getBoolean("booking_reminders", true)
        
        // Setup click listeners
        view.findViewById<View>(R.id.llEditProfile)?.setOnClickListener {
            // Navigate to edit profile fragment
            val editProfileFragment = EditProfileFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, editProfileFragment)
                .addToBackStack("edit_profile")
                .commit()
        }
        
        view.findViewById<View>(R.id.llPhoneNumber)?.setOnClickListener {
            // TODO: Edit phone number
            Toast.makeText(requireContext(), "Phone Number", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.llEmailSettings)?.setOnClickListener {
            // TODO: Email settings
            Toast.makeText(requireContext(), "Email Settings", Toast.LENGTH_SHORT).show()
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
        
        switchWeatherAlerts.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("weather_alerts", isChecked).apply()
        }
        
        switchBookingReminders.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("booking_reminders", isChecked).apply()
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

