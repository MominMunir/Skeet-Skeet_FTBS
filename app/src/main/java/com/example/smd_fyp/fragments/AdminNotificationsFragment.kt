package com.example.smd_fyp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.smd_fyp.R

class AdminNotificationsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        view.findViewById<View>(R.id.btnMarkAllRead)?.setOnClickListener {
            // Handle mark all as read
            markAllNotificationsAsRead()
        }
        
        // Load notifications
        loadNotifications()
    }

    private fun loadNotifications() {
        // Load notifications from data source
        // Update RecyclerView adapter
    }

    private fun markAllNotificationsAsRead() {
        // Mark all notifications as read
        // Update UI
    }
}

