package com.example.smd_fyp.player.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.R
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.home.FavoriteAdapter
import com.example.smd_fyp.model.Favorite
import com.example.smd_fyp.model.GroundApi
import com.example.smd_fyp.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesFragment : Fragment() {

    private lateinit var rvFavorites: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvFavoriteCount: TextView
    private lateinit var adapter: FavoriteAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize database
        LocalDatabaseHelper.initialize(requireContext())

        // Find views
        rvFavorites = view.findViewById(R.id.rvFavorites)
        llEmptyState = view.findViewById(R.id.llEmptyState)
        tvFavoriteCount = view.findViewById(R.id.tvFavoriteCount)

        // Setup RecyclerView
        rvFavorites.layoutManager = LinearLayoutManager(requireContext())
        adapter = FavoriteAdapter(emptyList(), emptyMap()) {
            // Refresh when favorite is removed
            loadFavorites()
        }
        rvFavorites.adapter = adapter

        // Load favorites
        loadFavorites()
    }

    private fun loadFavorites() {
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        val context = context ?: return

        if (currentUser == null) {
            if (isAdded) {
                updateEmptyState(emptyList())
            }
            return
        }

        // Observe local database (offline support)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                LocalDatabaseHelper.getFavoritesByUser(currentUser.uid)?.collect { localFavorites ->
                    if (!isAdded) return@collect
                    
                    // Load ground details for each favorite
                    val groundIds = localFavorites.map { it.groundId }
                    val groundsMap = mutableMapOf<String, GroundApi>()
                    
                    withContext(Dispatchers.IO) {
                        groundIds.forEach { groundId ->
                            val ground = LocalDatabaseHelper.getGround(groundId)
                            if (ground != null) {
                                groundsMap[groundId] = ground
                            }
                        }
                    }
                    
                    adapter.updateItems(localFavorites, groundsMap)
                    updateEmptyState(localFavorites)
                    tvFavoriteCount.text = "${localFavorites.size} favorites"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    updateEmptyState(emptyList())
                }
            }
        }

        // Fetch from API if online
        viewLifecycleOwner.lifecycleScope.launch {
            val currentContext = context ?: return@launch
            if (!isAdded) return@launch

            if (SyncManager.isOnline(currentContext)) {
                try {
                    val apiService = ApiClient.getPhpApiService(currentContext)
                    val response = apiService.getFavorites(currentUser.uid)

                    if (response.isSuccessful && response.body() != null) {
                        val apiFavorites = response.body()!!

                        // Save to local database
                        withContext(Dispatchers.IO) {
                            apiFavorites.forEach { favorite ->
                                LocalDatabaseHelper.addFavorite(favorite.copy(synced = true))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Don't show error, just use local data
                }
            }
        }
    }

    private fun updateEmptyState(favorites: List<Favorite>) {
        if (favorites.isEmpty()) {
            llEmptyState.visibility = View.VISIBLE
            rvFavorites.visibility = View.GONE
        } else {
            llEmptyState.visibility = View.GONE
            rvFavorites.visibility = View.VISIBLE
        }
    }
}


