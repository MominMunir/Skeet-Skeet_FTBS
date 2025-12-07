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
        
        switchNotifications?.setOnCheckedChangeListener { _, isChecked ->
            // Handle push notifications toggle
        }
        
        switchEmail?.setOnCheckedChangeListener { _, isChecked ->
            // Handle email notifications toggle
        }
        
        switchAutoApprove?.setOnCheckedChangeListener { _, isChecked ->
            // Handle auto-approve bookings toggle
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
            // Handle logout
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
        }
    }
}

