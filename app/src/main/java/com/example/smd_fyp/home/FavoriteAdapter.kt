package com.example.smd_fyp.home

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.GroundDetailActivity
import com.example.smd_fyp.R
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.model.Favorite
import com.example.smd_fyp.model.GroundApi
import com.example.smd_fyp.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoriteAdapter(
    private var favorites: List<Favorite>,
    private val grounds: Map<String, GroundApi>,
    private val onFavoriteRemoved: () -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGroundName: TextView = itemView.findViewById(R.id.tvGroundName)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val btnRemoveFavorite: ImageButton = itemView.findViewById(R.id.btnRemoveFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val favorite = favorites[position]
        val ground = grounds[favorite.groundId]

        if (ground != null) {
            holder.tvGroundName.text = ground.name
            holder.tvLocation.text = ground.location

            // Navigate to ground details on card click
            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, GroundDetailActivity::class.java).apply {
                    putExtra("ground_id", ground.id)
                }
                holder.itemView.context.startActivity(intent)
            }

            // Remove from favorites
            holder.btnRemoveFavorite.setOnClickListener {
                val currentUser = FirebaseAuthHelper.getCurrentUser()
                if (currentUser != null) {
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        LocalDatabaseHelper.removeFavorite(currentUser.uid, favorite.groundId)
                        
                        // Sync to PHP API if online
                        if (SyncManager.isOnline(holder.itemView.context)) {
                            SyncManager.deleteFavorite(holder.itemView.context, currentUser.uid, favorite.groundId)
                        }
                    }
                    onFavoriteRemoved()
                }
            }
        } else {
            // Ground not found, hide this item
            holder.itemView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = favorites.size

    fun updateItems(newFavorites: List<Favorite>, newGrounds: Map<String, GroundApi>) {
        favorites = newFavorites
        notifyDataSetChanged()
    }
}
