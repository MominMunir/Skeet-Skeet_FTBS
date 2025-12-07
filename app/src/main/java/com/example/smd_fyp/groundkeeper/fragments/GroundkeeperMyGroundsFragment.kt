package com.example.smd_fyp.groundkeeper.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.R
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.home.GroundkeeperGroundAdapter
import com.example.smd_fyp.model.GroundApi
import com.example.smd_fyp.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroundkeeperMyGroundsFragment : Fragment() {

    private lateinit var rvMyGrounds: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var groundAdapter: GroundkeeperGroundAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_groundkeeper_my_grounds, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize database
        LocalDatabaseHelper.initialize(requireContext())
        
        // Initialize views
        rvMyGrounds = view.findViewById(R.id.rvMyGrounds)
        llEmptyState = view.findViewById(R.id.llEmptyState)
        
        // Setup RecyclerView
        rvMyGrounds.layoutManager = LinearLayoutManager(requireContext())
        groundAdapter = GroundkeeperGroundAdapter(
            onViewClick = { ground ->
                Toast.makeText(requireContext(), "View: ${ground.name}", Toast.LENGTH_SHORT).show()
            },
            onEditClick = { ground ->
                Toast.makeText(requireContext(), "Edit: ${ground.name}", Toast.LENGTH_SHORT).show()
            },
            onStatusToggle = { ground, isChecked ->
                updateGroundStatus(ground, isChecked)
            }
        )
        rvMyGrounds.adapter = groundAdapter
        
        // Setup Add Ground button
        view.findViewById<View>(R.id.btnAddGround)?.setOnClickListener {
            // Navigate to add ground screen
            // TODO: Navigate to AdminAddGroundFragment or similar
            Toast.makeText(requireContext(), "Add Ground", Toast.LENGTH_SHORT).show()
        }
        
        // Load grounds from API
        loadMyGrounds()
    }
    
    private fun loadMyGrounds() {
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        if (currentUser == null) return
        
        lifecycleScope.launch {
            try {
                // Observe local database - filter by groundkeeper's grounds
                // Note: We need to add a userId field to GroundApi or filter by some criteria
                LocalDatabaseHelper.getAllGrounds()?.collect { allGrounds ->
                    // For now, show all grounds. In production, filter by groundkeeper's userId
                    val myGrounds = allGrounds // TODO: Filter by groundkeeper userId
                    groundAdapter.updateItems(myGrounds)
                    llEmptyState.visibility = if (myGrounds.isEmpty()) View.VISIBLE else View.GONE
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
                        
                        // Save to local database
                        withContext(Dispatchers.IO) {
                            LocalDatabaseHelper.saveGrounds(apiGrounds.map { it.copy(synced = true) })
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun updateGroundStatus(ground: GroundApi, isAvailable: Boolean) {
        lifecycleScope.launch {
            try {
                val updatedGround = ground.copy(available = isAvailable)
                
                // Update in local database
                withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.updateGround(updatedGround)
                }
                
                // Sync to API
                if (SyncManager.isOnline(requireContext())) {
                    SyncManager.syncGround(requireContext(), updatedGround)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error updating ground status", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


