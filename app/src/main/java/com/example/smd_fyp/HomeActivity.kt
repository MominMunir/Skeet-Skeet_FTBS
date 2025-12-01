package com.example.smd_fyp

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.home.GroundAdapter
import com.example.smd_fyp.model.Ground

class HomeActivity : AppCompatActivity() {
    
    private lateinit var llFilterOptions: LinearLayout
    private lateinit var tvLocationFilter: TextView
    private lateinit var tvPriceFilter: TextView
    private var isFiltersVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize filter views
        llFilterOptions = findViewById(R.id.llFilterOptions)
        tvLocationFilter = findViewById(R.id.tvLocationFilter)
        tvPriceFilter = findViewById(R.id.tvPriceFilter)

        // Wire RecyclerView with mock data so cards render at runtime
        val rv = findViewById<RecyclerView>(R.id.rvFeaturedGrounds)
        rv.layoutManager = LinearLayoutManager(this)

        val items = listOf(
            Ground(
                name = getString(R.string.ground_rc_name),
                location = getString(R.string.ground_gv_location),
                priceText = getString(R.string.ground_rc_price),
                ratingText = getString(R.string.ground_rc_rating),
                imageResId = R.drawable.mock_ground1,
                hasFloodlights = true,
                hasParking = true
            ),
            Ground(
                name = getString(R.string.ground_gv_name),
                location = getString(R.string.ground_gv_location),
                priceText = getString(R.string.ground_gv_price),
                ratingText = getString(R.string.ground_gv_rating),
                imageResId = R.drawable.mock_ground2,
                hasFloodlights = true,
                hasParking = false
            )
        )

        rv.adapter = GroundAdapter(items)

        // Setup Filters button click listener
        findViewById<View>(R.id.btnFilters)?.setOnClickListener {
            toggleFilters()
        }

        // Setup Location filter click listener
        findViewById<View>(R.id.llLocationFilter)?.setOnClickListener {
            showLocationFilterDialog()
        }

        // Setup Price filter click listener
        findViewById<View>(R.id.llPriceFilter)?.setOnClickListener {
            showPriceFilterDialog()
        }

        // TODO: Setup search functionality
        // TODO: Setup navigation drawer/menu
        // TODO: Setup top bar button click listeners
    }

    private fun toggleFilters() {
        isFiltersVisible = !isFiltersVisible
        llFilterOptions.visibility = if (isFiltersVisible) View.VISIBLE else View.GONE
    }

    private fun showLocationFilterDialog() {
        val locations = arrayOf(
            "All Locations",
            "DHA Phase 5, Lahore",
            "Johar Town, Lahore",
            "Gulberg, Lahore",
            "Model Town, Lahore",
            "Faisalabad",
            "Karachi",
            "Islamabad"
        )

        AlertDialog.Builder(this)
            .setTitle("Select Location")
            .setItems(locations) { _, which ->
                tvLocationFilter.text = locations[which]
                // TODO: Apply location filter to grounds list
                Toast.makeText(this, "Filtered by: ${locations[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showPriceFilterDialog() {
        val prices = arrayOf(
            "All Prices",
            "Under Rs. 2000/hr",
            "Rs. 2000 - 3000/hr",
            "Rs. 3000 - 4000/hr",
            "Rs. 4000 - 5000/hr",
            "Above Rs. 5000/hr"
        )

        AlertDialog.Builder(this)
            .setTitle("Select Price Range")
            .setItems(prices) { _, which ->
                tvPriceFilter.text = prices[which]
                // TODO: Apply price filter to grounds list
                Toast.makeText(this, "Filtered by: ${prices[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}
