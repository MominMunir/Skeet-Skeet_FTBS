package com.example.smd_fyp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.R
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.home.AdminGroundAdapter
import com.example.smd_fyp.model.GroundApi
import com.example.smd_fyp.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminGroundsFragment : Fragment() {

    private lateinit var rvGrounds: RecyclerView
    private lateinit var tvGroundCount: TextView
    private lateinit var groundAdapter: AdminGroundAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_grounds, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize database
        LocalDatabaseHelper.initialize(requireContext())
        
        // Initialize views
        tvGroundCount = view.findViewById(R.id.tvGroundCount)
        rvGrounds = view.findViewById(R.id.rvAdminGrounds)
        
        // Setup RecyclerView
        rvGrounds.layoutManager = LinearLayoutManager(requireContext())
        groundAdapter = AdminGroundAdapter { ground ->
            Toast.makeText(requireContext(), "Clicked: ${ground.name}", Toast.LENGTH_SHORT).show()
        }
        rvGrounds.adapter = groundAdapter
        
        // Load grounds from API
        loadGrounds()
    }
    
    private fun loadGrounds() {
        lifecycleScope.launch {
            try {
                // Observe local database
                LocalDatabaseHelper.getAllGrounds()?.collect { localGrounds ->
                    // Normalize image URLs to use current IP address
                    val normalizedGrounds = ApiClient.normalizeGroundImageUrls(
                        requireContext(),
                        localGrounds
                    )
                    groundAdapter.updateItems(normalizedGrounds)
                    tvGroundCount.text = "${normalizedGrounds.size} grounds"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Fetch from API if online
        lifecycleScope.launch {
            if (SyncManager.isOnline(requireContext())) {
                try {
                    val apiService = ApiClient.getPhpApiService(requireContext())
                    val response = apiService.getGrounds()
                    
                    if (response.isSuccessful && response.body() != null) {
                        val apiGrounds = response.body()!!
                        
                        // Normalize image URLs before saving to database
                        val normalizedGrounds = ApiClient.normalizeGroundImageUrls(
                            requireContext(),
                            apiGrounds
                        )
                        
                        // Save to local database
                        withContext(Dispatchers.IO) {
                            LocalDatabaseHelper.saveGrounds(normalizedGrounds.map { ground: GroundApi -> 
                                ground.copy(synced = true) 
                            })
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

