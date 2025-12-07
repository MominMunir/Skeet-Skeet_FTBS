package com.example.smd_fyp.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.R
import com.example.smd_fyp.model.Booking
import com.example.smd_fyp.model.BookingStatus

class BookingAdapter(
    private var items: MutableList<Booking> = mutableListOf(),
    private val onReceiptClick: ((Booking) -> Unit)? = null,
    private val onReviewClick: ((Booking) -> Unit)? = null
) : RecyclerView.Adapter<BookingAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGroundName: TextView = itemView.findViewById(R.id.tvGroundName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val btnReceipt: Button = itemView.findViewById(R.id.btnReceipt)
        val btnReview: Button = itemView.findViewById(R.id.btnReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking_card, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        
        holder.tvGroundName.text = item.groundName
        holder.tvStatus.text = item.status.name.lowercase().replaceFirstChar { it.uppercase() }
        holder.tvDateTime.text = "${item.date} • ${item.time}"
        holder.tvDuration.text = "${item.duration * 60} minutes"
        holder.tvPrice.text = "Rs. ${item.totalPrice.toInt()}"
        
        // Set status background color
        val statusBg = when (item.status) {
            BookingStatus.CONFIRMED -> R.drawable.bg_status_confirmed
            BookingStatus.PENDING -> R.drawable.bg_status_confirmed // Use same for now
            BookingStatus.CANCELLED -> R.drawable.bg_status_confirmed // Use same for now
            BookingStatus.COMPLETED -> R.drawable.bg_status_confirmed // Use same for now
        }
        holder.tvStatus.setBackgroundResource(statusBg)
        
        // Handle button clicks
        holder.btnReceipt.setOnClickListener {
            onReceiptClick?.invoke(item)
        }
        
        holder.btnReview.setOnClickListener {
            onReviewClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size
    
    fun updateItems(newItems: List<Booking>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
