package com.example.smd_fyp.player.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.R
import com.example.smd_fyp.model.Ground

class GroundAdapter(
    private val items: List<Ground>
) : RecyclerView.Adapter<GroundAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.ivGroundImage)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        val tvName: TextView = itemView.findViewById(R.id.tvGroundName)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvAmenity1: TextView = itemView.findViewById(R.id.tvAmenity1)
        val tvAmenity2: TextView = itemView.findViewById(R.id.tvAmenity2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ground_card, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.ivImage.setImageResource(item.imageResId)
        holder.tvRating.text = item.ratingText
        holder.tvName.text = item.name
        holder.tvLocation.text = item.location
        holder.tvPrice.text = item.priceText

        holder.tvAmenity1.visibility = if (item.hasFloodlights) View.VISIBLE else View.GONE
        holder.tvAmenity2.visibility = if (item.hasParking) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = items.size
}


