package com.example.smd_fyp.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.R
import com.example.smd_fyp.model.GroundApi

class GroundkeeperGroundAdapter(
    private var items: MutableList<GroundApi> = mutableListOf(),
    private val onViewClick: ((GroundApi) -> Unit)? = null,
    private val onEditClick: ((GroundApi) -> Unit)? = null,
    private val onStatusToggle: ((GroundApi, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<GroundkeeperGroundAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGroundName: TextView = itemView.findViewById(R.id.tvGroundName)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        val tvRating: TextView = itemView.findViewById(R.id.tvRating)
        val switchStatus: Switch = itemView.findViewById(R.id.switchStatus)
        val btnView: ImageButton = itemView.findViewById(R.id.btnView)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_groundkeeper_ground_card, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        
        holder.tvGroundName.text = item.name
        holder.tvLocation.text = item.location
        holder.tvPrice.text = item.priceText.ifEmpty { "Rs. ${item.price.toInt()}/hr" }
        holder.tvRating.text = item.ratingText.ifEmpty { String.format("%.1f", item.rating) }
        holder.switchStatus.isChecked = item.available
        
        // Handle status toggle
        holder.switchStatus.setOnCheckedChangeListener { _, isChecked ->
            onStatusToggle?.invoke(item, isChecked)
        }
        
        // Handle button clicks
        holder.btnView.setOnClickListener {
            onViewClick?.invoke(item)
        }
        
        holder.btnEdit.setOnClickListener {
            onEditClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size
    
    fun updateItems(newItems: List<GroundApi>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
