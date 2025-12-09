package com.example.smd_fyp.fragments

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

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
        swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)
        rvMyGrounds = view.findViewById(R.id.rvMyGrounds)
        llEmptyState = view.findViewById(R.id.llEmptyState)
        
        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
        
        // Setup RecyclerView
        rvMyGrounds.layoutManager = LinearLayoutManager(requireContext())
        groundAdapter = GroundkeeperGroundAdapter(
            onViewClick = { ground ->
                Toast.makeText(requireContext(), "View: ${ground.name}", Toast.LENGTH_SHORT).show()
            },
            onEditClick = { ground ->
                // Navigate to edit ground fragment
                val editFragment = GroundkeeperEditGroundFragment().apply {
                    arguments = Bundle().apply {
                        putString("ground_id", ground.id)
                    }
                }
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, editFragment)
                    .addToBackStack("edit_ground")
                    .commit()
            },
            onDeleteClick = { ground ->
                showDeleteConfirmation(ground)
            },
            onStatusToggle = { ground, isChecked ->
                updateGroundStatus(ground, isChecked)
            }
        )
        rvMyGrounds.adapter = groundAdapter
        
        // Setup Add Ground button
        view.findViewById<View>(R.id.btnAddGround)?.setOnClickListener {
            // Navigate to add ground fragment
            val addGroundFragment = GroundkeeperAddGroundFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, addGroundFragment)
                .addToBackStack("add_ground")
                .commit()
        }
        
        // Load grounds from API
        loadMyGrounds()
    }
    
    private fun refreshData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Sync all unsynced data
                if (SyncManager.isOnline(requireContext())) {
                    val syncResult = withContext(Dispatchers.IO) {
                        SyncManager.syncAll(requireContext())
                    }
                    
                    syncResult.fold(
                        onSuccess = { result ->
                            android.util.Log.d("GroundkeeperMyGroundsFragment", 
                                "Sync completed: ${result.syncedBookings} bookings, " +
                                "${result.syncedGrounds} grounds, ${result.syncedUsers} users")
                        },
                        onFailure = { exception ->
                            android.util.Log.e("GroundkeeperMyGroundsFragment", 
                                "Sync failed: ${exception.message}", exception)
                        }
                    )
                }
                
                // Reload grounds from API
                val currentUser = FirebaseAuthHelper.getCurrentUser()
                if (currentUser != null && SyncManager.isOnline(requireContext())) {
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
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Stop refresh animation
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private fun loadMyGrounds() {
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        if (currentUser == null) return
        
        lifecycleScope.launch {
            try {
                // Observe local database - filter by groundkeeper's grounds
                // Note: We need to add a userId field to GroundApi or filter by some criteria
                LocalDatabaseHelper.getAllGrounds()?.collect { allGrounds ->
                    // Normalize image URLs to use current IP address
                    val normalizedGrounds = ApiClient.normalizeGroundImageUrls(
                        requireContext(),
                        allGrounds
                    )
                    // For now, show all grounds. In production, filter by groundkeeper's userId
                    val myGrounds = normalizedGrounds // TODO: Filter by groundkeeper userId
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
    
    private fun updateGroundStatus(ground: GroundApi, isAvailable: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val context = this@GroundkeeperMyGroundsFragment.context ?: return@launch
                if (!isAdded) return@launch
                
                val updatedGround = ground.copy(available = isAvailable)
                
                // Update in local database
                withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.updateGround(updatedGround)
                }
                
                // Sync to API
                if (SyncManager.isOnline(context)) {
                    SyncManager.syncGround(context, updatedGround)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error updating ground status", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showDeleteConfirmation(ground: GroundApi) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Ground")
            .setMessage("Are you sure you want to delete ${ground.name}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteGround(ground)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteGround(ground: GroundApi) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val context = this@GroundkeeperMyGroundsFragment.context ?: return@launch
                if (!isAdded) return@launch
                
                // Delete using SyncManager
                val result = withContext(Dispatchers.IO) {
                    SyncManager.deleteGround(context, ground.id)
                }
                
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        if (result.isSuccess) {
                            Toast.makeText(context, "Ground deleted successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Error deleting ground: ${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error deleting ground: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

