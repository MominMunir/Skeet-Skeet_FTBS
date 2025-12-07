package com.example.smd_fyp.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.R
import com.example.smd_fyp.model.User
import com.example.smd_fyp.utils.GlideHelper

class UserAdapter(
    private var items: MutableList<User> = mutableListOf(),
    private val onItemClick: ((User) -> Unit)? = null
) : RecyclerView.Adapter<UserAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfile: ImageView = itemView.findViewById(R.id.ivProfile)
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvUserEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        val tvUserRole: TextView = itemView.findViewById(R.id.tvUserRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_card, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        
        // Load profile image
        if (!item.profileImageUrl.isNullOrEmpty()) {
            GlideHelper.loadImage(
                context = holder.itemView.context,
                imageUrl = item.profileImageUrl,
                imageView = holder.ivProfile,
                placeholder = R.drawable.logo,
                errorDrawable = R.drawable.logo,
                tag = "UserAdapter",
                useCircleCrop = true
            )
        } else {
            holder.ivProfile.setImageResource(R.drawable.logo)
        }
        
        holder.tvUserName.text = item.fullName.ifEmpty { "Unknown User" }
        holder.tvUserEmail.text = item.email
        holder.tvUserRole.text = item.role.name
        
        // Handle item click
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size
    
    fun updateItems(newItems: List<User>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    
    fun filter(query: String) {
        // This will be handled by the fragment
    }
}
