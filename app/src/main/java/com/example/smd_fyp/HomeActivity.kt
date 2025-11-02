package com.example.smd_fyp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.home.GroundAdapter
import com.example.smd_fyp.model.Ground

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homeRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        // TODO: Setup search functionality
        // TODO: Setup filter functionality
        // TODO: Setup navigation drawer/menu
        // TODO: Setup top bar button click listeners
    }
}
