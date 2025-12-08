package com.example.smd_fyp.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.R
import com.example.smd_fyp.model.Booking
import com.example.smd_fyp.model.BookingStatus
import java.text.SimpleDateFormat
import java.util.Locale

class RecentBookingAdapter(
    private var items: List<Booking> = emptyList()
) : RecyclerView.Adapter<RecentBookingAdapter.VH>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun updateItems(newItems: List<Booking>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_booking, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvGroundName: TextView = itemView.findViewById(R.id.tvGroundName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)

        fun bind(booking: Booking) {
            tvGroundName.text = booking.groundName.ifEmpty { "Unknown Ground" }
            
            // Format date and time
            val dateStr = try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(booking.date)
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
            } catch (e: Exception) {
                booking.date
            }
            val timeStr = booking.time
            tvDateTime.text = "$dateStr • $timeStr"
            
            tvPrice.text = "Rs. ${booking.totalPrice.toInt()}"
            
            // Set status
            val statusText = booking.status.name.lowercase().replaceFirstChar { it.uppercase() }
            tvStatus.text = statusText
            
            val ctx = itemView.context
            val statusColor = when (booking.status) {
                BookingStatus.CONFIRMED, BookingStatus.COMPLETED -> ContextCompat.getColor(ctx, R.color.green_600)
                BookingStatus.PENDING -> ContextCompat.getColor(ctx, R.color.yellow_600)
                BookingStatus.CANCELLED -> ContextCompat.getColor(ctx, R.color.red_600)
            }
            
            val statusBg = when (booking.status) {
                BookingStatus.CONFIRMED, BookingStatus.COMPLETED -> R.drawable.bg_status_confirmed
                BookingStatus.PENDING -> R.drawable.bg_status_pending
                BookingStatus.CANCELLED -> R.drawable.bg_status_confirmed
            }
            
            tvStatus.setTextColor(statusColor)
            tvStatus.setBackgroundResource(statusBg)
        }
    }
}
