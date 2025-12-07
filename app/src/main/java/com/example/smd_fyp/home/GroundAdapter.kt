package com.example.smd_fyp.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.smd_fyp.R
import com.example.smd_fyp.model.GroundApi

class GroundAdapter(
    private var items: MutableList<GroundApi> = mutableListOf(),
    private val onItemClick: ((GroundApi) -> Unit)? = null
) : RecyclerView.Adapter<GroundAdapter.VH>() {
    
    fun updateItems(newItems: List<GroundApi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

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
        
        // Load image from URL using Glide
        if (!item.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.mock_ground1) // Fallback image
                .error(R.drawable.mock_ground1) // Error image
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(holder.ivImage)
        } else {
            // Use default image if no URL
            holder.ivImage.setImageResource(R.drawable.mock_ground1)
        }
        
        holder.tvRating.text = item.ratingText.ifEmpty { String.format("%.1f", item.rating) }
        holder.tvName.text = item.name
        holder.tvLocation.text = item.location
        holder.tvPrice.text = item.priceText.ifEmpty { "Rs. ${item.price.toInt()}/hour" }

        holder.tvAmenity1.visibility = if (item.hasFloodlights) View.VISIBLE else View.GONE
        holder.tvAmenity2.visibility = if (item.hasParking) View.VISIBLE else View.GONE
        
        // Handle item click
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
