package com.example.smd_fyp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SwitchCompat
import com.example.smd_fyp.R
import com.example.smd_fyp.auth.AuthActivity

class AdminSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val switchNotifications = view.findViewById<SwitchCompat>(R.id.switchNotifications)
        val switchEmail = view.findViewById<SwitchCompat>(R.id.switchEmail)
        val switchAutoApprove = view.findViewById<SwitchCompat>(R.id.switchAutoApprove)
        
        // Load saved preferences
        val prefs = requireContext().getSharedPreferences("admin_settings", 0)
        switchNotifications?.isChecked = prefs.getBoolean("push_notifications", true)
        switchEmail?.isChecked = prefs.getBoolean("email_notifications", true)
        switchAutoApprove?.isChecked = prefs.getBoolean("auto_approve", false)
        
        switchNotifications?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("push_notifications", isChecked).apply()
        }
        
        switchEmail?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("email_notifications", isChecked).apply()
        }
        
        switchAutoApprove?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_approve", isChecked).apply()
        }
        
        view.findViewById<View>(R.id.btnEditProfile)?.setOnClickListener {
            // Navigate to edit profile fragment
            val editProfileFragment = com.example.smd_fyp.fragments.EditProfileFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, editProfileFragment)
                .addToBackStack("edit_profile")
                .commit()
        }
        
        view.findViewById<View>(R.id.btnChangePassword)?.setOnClickListener {
            // Navigate to edit profile fragment where password change is available
            val editProfileFragment = com.example.smd_fyp.fragments.EditProfileFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, editProfileFragment)
                .addToBackStack("edit_profile")
                .commit()
        }

        view.findViewById<View>(R.id.btnLogout)?.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
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
}

