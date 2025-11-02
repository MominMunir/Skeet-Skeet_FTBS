package com.example.smd_fyp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SwitchCompat
import com.example.smd_fyp.R

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
        
        view.findViewById<View>(R.id.btnChangePassword)?.setOnClickListener {
            // Handle change password
        }
        
        view.findViewById<View>(R.id.btnLogout)?.setOnClickListener {
            // Handle logout
        }
    }
}

