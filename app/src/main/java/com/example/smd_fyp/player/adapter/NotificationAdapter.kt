package com.example.smd_fyp.player.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.R
import com.example.smd_fyp.model.Notification
import com.example.smd_fyp.model.NotificationType

class NotificationAdapter(
    private var notifications: List<Notification>,
    private val onItemClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivNotificationIcon)
        val tvTitle: TextView = itemView.findViewById(R.id.tvNotificationTitle)
        val tvMessage: TextView = itemView.findViewById(R.id.tvNotificationMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvNotificationTime)
        val viewUnreadIndicator: View = itemView.findViewById(R.id.viewUnreadIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]

        holder.tvTitle.text = notification.title
        holder.tvMessage.text = notification.message
        holder.tvTime.text = notification.getFormattedTime()

        // Set icon based on notification type
        val iconRes = when (notification.type) {
            NotificationType.BOOKING -> R.drawable.ic_notifications
            NotificationType.PAYMENT -> R.drawable.ic_coin
            NotificationType.REMINDER -> R.drawable.ic_notifications
            NotificationType.WEATHER -> R.drawable.ic_notifications
            NotificationType.SYSTEM -> R.drawable.ic_notifications
        }
        holder.ivIcon.setImageResource(iconRes)

        // Show/hide unread indicator
        holder.viewUnreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE

        // Set click listener
        holder.itemView.setOnClickListener {
            onItemClick(notification)
        }
    }

    override fun getItemCount(): Int = notifications.size
    
    fun updateItems(newItems: List<Notification>) {
        notifications = newItems
        notifyDataSetChanged()
    }
}


