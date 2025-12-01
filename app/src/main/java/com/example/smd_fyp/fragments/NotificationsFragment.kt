package com.example.smd_fyp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.HomeActivity
import com.example.smd_fyp.R
import com.example.smd_fyp.home.NotificationAdapter
import com.example.smd_fyp.model.Notification
import com.example.smd_fyp.model.NotificationType

class NotificationsFragment : Fragment() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var adapter: NotificationAdapter
    private val notifications = mutableListOf<Notification>()

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

        // Setup RecyclerView
        rvNotifications.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotificationAdapter(notifications) { notification ->
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
        // TODO: Load notifications from data source/API
        // For now, using mock data
        notifications.clear()
        notifications.addAll(getMockNotifications())
        adapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun getMockNotifications(): List<Notification> {
        return listOf(
            Notification(
                id = "1",
                title = "Booking Confirmed",
                message = "Your booking at Royal Football Club has been confirmed for tomorrow at 6:00 PM",
                time = "2 hours ago",
                isRead = false,
                type = NotificationType.BOOKING
            ),
            Notification(
                id = "2",
                title = "Payment Received",
                message = "Payment of Rs. 3,000 has been received for your booking",
                time = "5 hours ago",
                isRead = false,
                type = NotificationType.PAYMENT
            ),
            Notification(
                id = "3",
                title = "Reminder",
                message = "You have a booking at Green Valley Sports in 1 hour",
                time = "1 day ago",
                isRead = true,
                type = NotificationType.REMINDER
            ),
            Notification(
                id = "4",
                title = "Booking Cancelled",
                message = "Your booking at Royal Football Club has been cancelled",
                time = "2 days ago",
                isRead = true,
                type = NotificationType.BOOKING
            )
        )
    }

    private fun onNotificationClick(notification: Notification) {
        // Mark as read
        if (!notification.isRead) {
            notification.isRead = true
            adapter.notifyItemChanged(notifications.indexOf(notification))
            updateEmptyState()
        }

        // TODO: Navigate to relevant screen based on notification type
    }

    private fun markAllAsRead() {
        notifications.forEach { it.isRead = true }
        adapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val hasUnread = notifications.any { !it.isRead }
        if (notifications.isEmpty()) {
            llEmptyState.visibility = View.VISIBLE
            rvNotifications.visibility = View.GONE
        } else {
            llEmptyState.visibility = View.GONE
            rvNotifications.visibility = View.VISIBLE
        }
    }
}

