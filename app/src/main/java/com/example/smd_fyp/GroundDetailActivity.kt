package com.example.smd_fyp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.smd_fyp.api.OpenMeteoService
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.model.Favorite
import com.example.smd_fyp.model.GroundApi
import com.example.smd_fyp.utils.GroundConditionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class GroundDetailActivity : AppCompatActivity() {

    private lateinit var ground: GroundApi
    private lateinit var btnFavorite: ImageButton
    private var isFavorite: Boolean = false
    private val weatherService = OpenMeteoService.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ground_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.groundDetailRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize database
        LocalDatabaseHelper.initialize(this)

        // Get ground ID from intent
        val groundId = intent.getStringExtra("ground_id")
        if (groundId == null) {
            Toast.makeText(this, "Ground not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load ground data
        loadGroundData(groundId)

        // Initialize views
        btnFavorite = findViewById(R.id.btnFavorite)
        
        // Setup back button
        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Setup Favorite button
        btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        // Setup Book Now button
        findViewById<View>(R.id.btnBookNow)?.setOnClickListener {
            if (::ground.isInitialized) {
                val intent = Intent(this, BookingActivity::class.java).apply {
                    putExtra("ground_id", ground.id)
                    putExtra("ground_name", ground.name)
                    putExtra("ground_price", ground.price)
                }
                startActivity(intent)
            }
        }
    }

    private fun loadGroundData(groundId: String) {
        lifecycleScope.launch {
            try {
                val loadedGround = withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.getGround(groundId)
                }

                if (loadedGround == null) {
                    Toast.makeText(this@GroundDetailActivity, "Ground not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@launch
                }

                ground = loadedGround
                displayGroundData()
                checkFavoriteStatus()
                loadGroundCondition()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@GroundDetailActivity, "Error loading ground: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun checkFavoriteStatus() {
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        if (currentUser == null) {
            btnFavorite.visibility = View.GONE
            return
        }
        
        lifecycleScope.launch {
            try {
                val favoriteCount = withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.isFavorite(currentUser.uid, ground.id)
                }
                isFavorite = favoriteCount > 0
                updateFavoriteButton()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun toggleFavorite() {
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please login to add favorites", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (isFavorite) {
                        // Remove from favorites
                        LocalDatabaseHelper.removeFavorite(currentUser.uid, ground.id)
                        isFavorite = false
                    } else {
                        // Add to favorites
                        val favorite = Favorite(
                            id = Favorite.createId(currentUser.uid, ground.id),
                            userId = currentUser.uid,
                            groundId = ground.id,
                            createdAt = System.currentTimeMillis(),
                            synced = false
                        )
                        LocalDatabaseHelper.addFavorite(favorite)
                        isFavorite = true
                    }
                }
                
                withContext(Dispatchers.Main) {
                    updateFavoriteButton()
                    Toast.makeText(
                        this@GroundDetailActivity,
                        if (isFavorite) "Added to favorites" else "Removed from favorites",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Sync to PHP API if online
                if (com.example.smd_fyp.sync.SyncManager.isOnline(this@GroundDetailActivity)) {
                    withContext(Dispatchers.IO) {
                        val favorite = Favorite(
                            id = Favorite.createId(currentUser.uid, ground.id),
                            userId = currentUser.uid,
                            groundId = ground.id,
                            createdAt = System.currentTimeMillis(),
                            synced = false
                        )
                        if (isFavorite) {
                            com.example.smd_fyp.sync.SyncManager.syncFavorite(this@GroundDetailActivity, favorite)
                        } else {
                            com.example.smd_fyp.sync.SyncManager.deleteFavorite(this@GroundDetailActivity, currentUser.uid, ground.id)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@GroundDetailActivity, "Error updating favorite: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateFavoriteButton() {
        if (isFavorite) {
            btnFavorite.setImageResource(R.drawable.ic_heart)
            btnFavorite.contentDescription = "Remove from favorites"
        } else {
            btnFavorite.setImageResource(R.drawable.ic_heart_outline)
            btnFavorite.contentDescription = "Add to favorites"
        }
    }

    private fun displayGroundData() {
        // Ground Image
        val ivGroundImage = findViewById<ImageView>(R.id.ivGroundImage)
        if (!ground.imageUrl.isNullOrEmpty()) {
            // Use Glide directly with proper lifecycle management to prevent image reset on scroll
            val normalizedUrl = com.example.smd_fyp.api.ApiClient.normalizeImageUrl(this, ground.imageUrl)
            if (!normalizedUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(normalizedUrl)
                    .placeholder(R.drawable.mock_ground1)
                    .error(R.drawable.mock_ground1)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .into(ivGroundImage)
            } else {
                ivGroundImage.setImageResource(R.drawable.mock_ground1)
            }
        } else {
            ivGroundImage.setImageResource(R.drawable.mock_ground1)
        }

        // Ground Name
        findViewById<TextView>(R.id.tvGroundName)?.text = ground.name

        // Location
        findViewById<TextView>(R.id.tvLocation)?.text = ground.location

        // Price
        findViewById<TextView>(R.id.tvPrice)?.text = ground.priceText.ifEmpty { "Rs. ${ground.price.toInt()}/hour" }

        // Rating
        findViewById<TextView>(R.id.tvRating)?.text = ground.ratingText.ifEmpty { String.format("%.1f", ground.rating) }

        // Description
        findViewById<TextView>(R.id.tvDescription)?.text = ground.description ?: "No description available"

        // Amenities
        findViewById<TextView>(R.id.tvAmenity1)?.visibility = if (ground.hasFloodlights) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.tvAmenity2)?.visibility = if (ground.hasParking) View.VISIBLE else View.GONE

        // Availability status
        if (!ground.available) {
            findViewById<View>(R.id.btnBookNow)?.isEnabled = false
            findViewById<TextView>(R.id.tvAvailability)?.apply {
                text = "Currently Unavailable"
                visibility = View.VISIBLE
            }
        } else {
            findViewById<TextView>(R.id.tvAvailability)?.visibility = View.GONE
        }
    }
    
    private fun loadGroundCondition() {
        // Check if manual condition is set
        ground.manualCondition?.let { manualConditionStr ->
            val manualCondition = when (manualConditionStr) {
                "EXCELLENT" -> GroundConditionHelper.GroundCondition.EXCELLENT
                "GOOD" -> GroundConditionHelper.GroundCondition.GOOD
                "MODERATE" -> GroundConditionHelper.GroundCondition.MODERATE
                "POOR" -> GroundConditionHelper.GroundCondition.POOR
                else -> null
            }
            if (manualCondition != null) {
                displayCondition(manualCondition)
                return
            }
        }
        
        val coords = getCityCoordinates(ground.location ?: "")
        if (coords == null) {
            findViewById<TextView>(R.id.tvCondition)?.visibility = View.GONE
            return
        }
        
        lifecycleScope.launch {
            try {
                val forecast = withContext(Dispatchers.IO) {
                    weatherService.getDailyForecast(coords.first, coords.second, forecastDays = 1)
                }
                val condition = GroundConditionHelper.calculateCondition(forecast)
                displayCondition(condition)
            } catch (e: Exception) {
                android.util.Log.e("GroundDetailActivity", "Error loading condition", e)
                findViewById<TextView>(R.id.tvCondition)?.visibility = View.GONE
            }
        }
    }
    
    private fun displayCondition(condition: GroundConditionHelper.GroundCondition) {
        val tvCondition = findViewById<TextView>(R.id.tvCondition) ?: return
        tvCondition.text = "Condition: ${GroundConditionHelper.getConditionText(condition)}"
        
        // Set background color based on condition
        val backgroundColorRes = when (condition) {
            GroundConditionHelper.GroundCondition.EXCELLENT -> android.R.color.holo_green_dark
            GroundConditionHelper.GroundCondition.GOOD -> android.R.color.holo_green_light
            GroundConditionHelper.GroundCondition.MODERATE -> android.R.color.holo_orange_light
            GroundConditionHelper.GroundCondition.POOR -> android.R.color.holo_red_dark
        }
        tvCondition.setBackgroundColor(getColor(backgroundColorRes))
        tvCondition.setTextColor(getColor(android.R.color.white))
        tvCondition.visibility = View.VISIBLE
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
}
