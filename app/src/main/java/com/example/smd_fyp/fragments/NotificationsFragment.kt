package com.example.smd_fyp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.HomeActivity
import com.example.smd_fyp.MyBookingsActivity
import com.example.smd_fyp.R
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.home.NotificationAdapter
import com.example.smd_fyp.model.Notification
import com.example.smd_fyp.model.NotificationType
import com.example.smd_fyp.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsFragment : Fragment() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvNotifications = view.findViewById(R.id.rvNotifications)
        llEmptyState = view.findViewById(R.id.llEmptyState)

        // Initialize database
        LocalDatabaseHelper.initialize(requireContext())
        
        // Setup RecyclerView
        rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotificationAdapter(emptyList()) { notification ->
            // Handle notification click
            onNotificationClick(notification)
        }
        rvNotifications.adapter = adapter

        // Setup Back button
        view.findViewById<View>(R.id.btnBack)?.setOnClickListener {
            // Hide notifications fragment
            if (requireActivity() is HomeActivity) {
                (requireActivity() as HomeActivity).hideNotifications()
            }
        }

        // Setup Mark All Read button
        view.findViewById<View>(R.id.btnMarkAllRead)?.setOnClickListener {
            markAllAsRead()
        }

        // Load notifications
        loadNotifications()
    }

    private fun loadNotifications() {
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        val context = context ?: return
        
        if (currentUser == null) {
            if (isAdded) {
                updateEmptyState(emptyList())
            }
            return
        }
        
        // Observe local database (offline support)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val notificationsFlow = LocalDatabaseHelper.getNotificationsByUser(currentUser.uid)
                if (notificationsFlow != null) {
                    notificationsFlow.collect { localNotifications ->
                        if (!isAdded) return@collect
                        android.util.Log.d("NotificationsFragment", "Loaded ${localNotifications.size} notifications")
                        adapter.updateItems(localNotifications)
                        updateEmptyState(localNotifications)
                    }
                } else {
                    android.util.Log.e("NotificationsFragment", "Failed to get notifications flow")
                    if (isAdded) {
                        updateEmptyState(emptyList())
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationsFragment", "Error loading notifications: ${e.message}", e)
                e.printStackTrace()
                if (isAdded) {
                    updateEmptyState(emptyList())
                }
            }
        }
        
        // Fetch from API if online
        viewLifecycleOwner.lifecycleScope.launch {
            val currentContext = context ?: return@launch
            if (!isAdded) return@launch
            
            if (SyncManager.isOnline(currentContext)) {
                try {
                    val apiService = ApiClient.getPhpApiService(currentContext)
                    val response = apiService.getNotifications(currentUser.uid)
                    
                    if (response.isSuccessful && response.body() != null) {
                        val apiNotifications = response.body()!!
                        
                        // Save to local database
                        withContext(Dispatchers.IO) {
                            apiNotifications.forEach { notification ->
                                LocalDatabaseHelper.saveNotification(notification.copy(synced = true))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Don't show error, just use local data
                }
            }
        }
    }

    private fun onNotificationClick(notification: Notification) {
        // Mark as read
        if (!notification.isRead) {
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.markNotificationAsRead(notification.id)
                }
            }
        }

        // Navigate to relevant screen based on notification type
        when (notification.type) {
            NotificationType.BOOKING -> {
                // Navigate to bookings
                val intent = Intent(requireContext(), MyBookingsActivity::class.java)
                startActivity(intent)
            }
            NotificationType.PAYMENT -> {
                // Navigate to bookings (payments are related to bookings)
                val intent = Intent(requireContext(), MyBookingsActivity::class.java)
                startActivity(intent)
            }
            NotificationType.REMINDER -> {
                // Navigate to bookings
                val intent = Intent(requireContext(), MyBookingsActivity::class.java)
                startActivity(intent)
            }
            NotificationType.SYSTEM -> {
                // Stay on current screen or navigate to home
            }
        }
    }

    private fun markAllAsRead() {
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        if (currentUser == null) return
        
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                LocalDatabaseHelper.markAllNotificationsAsRead(currentUser.uid)
            }
        }
    }

    private fun updateEmptyState(notifications: List<Notification>) {
        if (notifications.isEmpty()) {
            llEmptyState.visibility = View.VISIBLE
            rvNotifications.visibility = View.GONE
        } else {
            llEmptyState.visibility = View.GONE
            rvNotifications.visibility = View.VISIBLE
        }
    }
}

