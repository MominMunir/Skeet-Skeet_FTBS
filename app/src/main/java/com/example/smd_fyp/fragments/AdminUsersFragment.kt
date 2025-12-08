package com.example.smd_fyp.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.R
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.home.UserAdapter
import com.example.smd_fyp.model.User
import com.example.smd_fyp.sync.SyncManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminUsersFragment : Fragment() {

    private lateinit var rvUsers: RecyclerView
    private lateinit var etSearchUsers: TextInputEditText
    private lateinit var userAdapter: UserAdapter
    private val allUsers = mutableListOf<User>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize database
        LocalDatabaseHelper.initialize(requireContext())
        
        // Initialize views
        rvUsers = view.findViewById(R.id.rvUsers)
        etSearchUsers = view.findViewById(R.id.etSearchUsers)
        
        // Setup RecyclerView
        rvUsers.layoutManager = LinearLayoutManager(requireContext())
        userAdapter = UserAdapter { user ->
            // Handle user click
            Toast.makeText(requireContext(), "Clicked: ${user.fullName}", Toast.LENGTH_SHORT).show()
        }
        rvUsers.adapter = userAdapter
        
        // Setup search functionality
        etSearchUsers.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Load users from API
        loadUsers()
    }
    
    private fun loadUsers() {
        lifecycleScope.launch {
            try {
                // Fetch from API first if online
                if (SyncManager.isOnline(requireContext())) {
                    try {
                        val apiService = ApiClient.getPhpApiService(requireContext())
                        val response = apiService.getUsers()
                        
                        if (response.isSuccessful && response.body() != null) {
                            val apiUsers = response.body()!!
                            android.util.Log.d("AdminUsersFragment", "Fetched ${apiUsers.size} users from API")
                            
                            // Save to local database
                            withContext(Dispatchers.IO) {
                                apiUsers.forEach { user ->
                                    LocalDatabaseHelper.saveUser(user.copy(synced = true))
                                }
                            }
                        } else {
                            android.util.Log.e("AdminUsersFragment", "API Error: ${response.code()} - ${response.message()}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AdminUsersFragment", "Error fetching users from API", e)
                        e.printStackTrace()
                    }
                }
                
                // Observe local database for updates
                LocalDatabaseHelper.getAllUsers()?.collect { localUsers ->
                    allUsers.clear()
                    allUsers.addAll(localUsers)
                    android.util.Log.d("AdminUsersFragment", "Loaded ${localUsers.size} users from local DB")
                    filterUsers(etSearchUsers.text.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun filterUsers(query: String) {
        val filtered = if (query.isEmpty()) {
            allUsers
        } else {
            allUsers.filter {
                it.fullName.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true) ||
                it.role.name.contains(query, ignoreCase = true)
            }
        }
        userAdapter.updateItems(filtered)
    }
}

