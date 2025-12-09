package com.example.smd_fyp.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.smd_fyp.R
import android.widget.ImageButton
import com.example.smd_fyp.api.OpenMeteoService
import com.example.smd_fyp.model.GroundApi
import com.example.smd_fyp.utils.GroundConditionHelper
import com.example.smd_fyp.utils.GroundConditionHelper.GroundCondition
import com.example.smd_fyp.utils.MapHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

class GroundAdapter(
    private var items: MutableList<GroundApi> = mutableListOf(),
    private val onItemClick: ((GroundApi) -> Unit)? = null
) : RecyclerView.Adapter<GroundAdapter.VH>() {
    
    private val weatherService = OpenMeteoService.create()
    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val conditionCache = mutableMapOf<String, GroundConditionHelper.GroundCondition>()
    
    fun updateItems(newItems: List<GroundApi>) {
        items.clear()
        items.addAll(newItems)
        conditionCache.clear() // Clear cache when items change
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
        val tvCondition: TextView = itemView.findViewById(R.id.tvCondition)
        val btnMapIcon: ImageButton = itemView.findViewById(R.id.btnMapIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ground_card, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        
        // Load image from URL using Glide
        // Normalize image URL to use current IP address
        val normalizedImageUrl = com.example.smd_fyp.api.ApiClient.normalizeImageUrl(
            holder.itemView.context,
            item.imageUrl
        )
        
        if (!normalizedImageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(normalizedImageUrl)
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
        
        // Setup map button click listener
        holder.btnMapIcon.setOnClickListener { view ->
            // Prevent the card click from firing
            view.isClickable = true
            openLocationInMaps(view.context, item)
        }
        
        // Load and display condition
        loadConditionForGround(holder, item)
        
        // Handle item click
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }
    
    private fun loadConditionForGround(holder: VH, ground: GroundApi) {
        // Check if manual condition is set
        ground.manualCondition?.let { manualConditionStr ->
            val manualCondition = when (manualConditionStr) {
                "EXCELLENT" -> GroundCondition.EXCELLENT
                "GOOD" -> GroundCondition.GOOD
                "MODERATE" -> GroundCondition.MODERATE
                "POOR" -> GroundCondition.POOR
                else -> null
            }
            if (manualCondition != null) {
                displayCondition(holder, manualCondition)
                return
            }
        }
        
        // Check cache first
        val cacheKey = ground.location ?: ground.id
        conditionCache[cacheKey]?.let { cachedCondition ->
            displayCondition(holder, cachedCondition)
            return
        }
        
        // Get coordinates for ground location
        val coords = getCityCoordinates(ground.location ?: "")
        if (coords == null) {
            holder.tvCondition.visibility = View.GONE
            return
        }
        
        // Fetch weather and calculate condition
        adapterScope.launch {
            try {
                val forecast = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    weatherService.getDailyForecast(coords.first, coords.second, forecastDays = 1)
                }
                val condition = GroundConditionHelper.calculateCondition(forecast)
                conditionCache[cacheKey] = condition
                displayCondition(holder, condition)
            } catch (e: Exception) {
                android.util.Log.e("GroundAdapter", "Error loading condition for ${ground.name}", e)
                holder.tvCondition.visibility = View.GONE
            }
        }
    }
    
    private fun displayCondition(holder: VH, condition: GroundConditionHelper.GroundCondition) {
        holder.tvCondition.text = GroundConditionHelper.getConditionText(condition)
        // Always use white text for readability on colored background
        holder.tvCondition.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.white))
        
        // Set background color based on condition
        val backgroundColorRes = when (condition) {
            GroundCondition.EXCELLENT -> android.R.color.holo_green_dark
            GroundCondition.GOOD -> android.R.color.holo_green_light
            GroundCondition.MODERATE -> android.R.color.holo_orange_light
            GroundCondition.POOR -> android.R.color.holo_red_dark
        }
        holder.tvCondition.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, backgroundColorRes))
        
        holder.tvCondition.visibility = View.VISIBLE
    }
    
    private fun getCityCoordinates(city: String): Pair<Double, Double>? {
        val normalized = city.lowercase(Locale.getDefault())
        return when {
            normalized.contains("islamabad") -> Pair(33.7215, 73.0433)
            normalized.contains("lahore") -> Pair(31.558, 74.3507)
            normalized.contains("karachi") -> Pair(24.8608, 67.0104)
            else -> null
        }
    }
    
    private fun openLocationInMaps(context: android.content.Context, ground: GroundApi) {
        val location = ground.location
        if (location.isNullOrEmpty()) {
            android.widget.Toast.makeText(context, "Location not available", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        
        // Search by place name directly for accurate results
        MapHelper.openInGoogleMaps(context, location, ground.name)
    }

    override fun getItemCount(): Int = items.size
}
