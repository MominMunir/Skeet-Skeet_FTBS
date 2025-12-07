package com.example.smd_fyp.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.R
import com.example.smd_fyp.model.GroundApi

class AdminGroundAdapter(
    private var items: MutableList<GroundApi> = mutableListOf(),
    private val onItemClick: ((GroundApi) -> Unit)? = null
) : RecyclerView.Adapter<AdminGroundAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGroundName: TextView = itemView.findViewById(R.id.tvGroundName)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        val tvBookings: TextView = itemView.findViewById(R.id.tvBookings)
        val tvRevenue: TextView = itemView.findViewById(R.id.tvRevenue)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvRank: TextView = itemView.findViewById(R.id.tvRank)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_ground_card, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        
        holder.tvGroundName.text = item.name
        holder.tvLocation.text = item.location
        holder.tvRating.text = item.ratingText.ifEmpty { String.format("%.1f", item.rating) }
        holder.tvPrice.text = item.priceText.ifEmpty { "Rs. ${item.price.toInt()}/hr" }
        
        // TODO: Calculate bookings and revenue from bookings table
        // For now, show placeholder
        holder.tvBookings.text = "0"
        holder.tvRevenue.text = "Rs. 0"
        holder.tvRank.text = "#${position + 1}"
        
        // Handle item click
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size
    
    fun updateItems(newItems: List<GroundApi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
