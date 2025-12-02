package com.example.smd_fyp.player.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.smd_fyp.R

class FavoritesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup click listeners for favorite items
        view.findViewById<View>(R.id.cardFavorite1)?.setOnClickListener {
            // TODO: Navigate to ground details
        }
        
        view.findViewById<View>(R.id.cardFavorite2)?.setOnClickListener {
            // TODO: Navigate to ground details
        }
        
        // Heart icon click listeners to remove from favorites
        view.findViewById<View>(R.id.ivHeart1)?.setOnClickListener {
            // TODO: Remove from favorites
        }
        
        view.findViewById<View>(R.id.ivHeart2)?.setOnClickListener {
            // TODO: Remove from favorites
        }
    }
}


