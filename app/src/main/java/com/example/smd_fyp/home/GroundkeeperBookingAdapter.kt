package com.example.smd_fyp.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.R
import com.example.smd_fyp.model.Booking
import com.example.smd_fyp.model.BookingStatus

class GroundkeeperBookingAdapter(
    private var items: MutableList<Booking> = mutableListOf(),
    private val onAcceptClick: ((Booking) -> Unit)? = null,
    private val onDeclineClick: ((Booking) -> Unit)? = null
) : RecyclerView.Adapter<GroundkeeperBookingAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGroundName: TextView = itemView.findViewById(R.id.tvGroundName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvDateTime: TextView = itemView.findViewById(R.id.tvDateTime)
        val tvPayment: TextView = itemView.findViewById(R.id.tvPayment)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val llActions: LinearLayout = itemView.findViewById(R.id.llActions)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        val btnDecline: Button = itemView.findViewById(R.id.btnDecline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_groundkeeper_booking_card, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        
        holder.tvGroundName.text = item.groundName
        holder.tvStatus.text = item.status.name.lowercase().replaceFirstChar { it.uppercase() }
        holder.tvDateTime.text = "${item.date} • ${item.time} • ${item.duration * 60} min."
        holder.tvPayment.text = "Payment: ${item.paymentStatus.name}"
        holder.tvAmount.text = "Rs. ${item.totalPrice.toInt()}"
        
        // Set status background color
        val statusBg = when (item.status) {
            BookingStatus.CONFIRMED -> R.drawable.bg_status_confirmed
            BookingStatus.PENDING -> R.drawable.bg_status_pending
            BookingStatus.CANCELLED -> R.drawable.bg_status_confirmed // Use same for now
            BookingStatus.COMPLETED -> R.drawable.bg_status_confirmed // Use same for now
        }
        holder.tvStatus.setBackgroundResource(statusBg)
        
        // Show action buttons only for pending bookings
        if (item.status == BookingStatus.PENDING) {
            holder.llActions.visibility = View.VISIBLE
            holder.btnAccept.setOnClickListener {
                onAcceptClick?.invoke(item)
            }
            holder.btnDecline.setOnClickListener {
                onDeclineClick?.invoke(item)
            }
        } else {
            holder.llActions.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size
    
    fun updateItems(newItems: List<Booking>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
