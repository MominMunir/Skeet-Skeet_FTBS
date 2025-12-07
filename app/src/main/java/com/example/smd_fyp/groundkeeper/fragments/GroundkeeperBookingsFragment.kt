package com.example.smd_fyp.groundkeeper.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.R
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.home.GroundkeeperBookingAdapter
import com.example.smd_fyp.model.Booking
import com.example.smd_fyp.model.BookingStatus
import com.example.smd_fyp.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroundkeeperBookingsFragment : Fragment() {

    private lateinit var rvBookings: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvBookingCount: TextView
    private lateinit var bookingAdapter: GroundkeeperBookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_groundkeeper_bookings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize database
        LocalDatabaseHelper.initialize(requireContext())
        
        // Initialize views
        rvBookings = view.findViewById(R.id.rvBookings)
        llEmptyState = view.findViewById(R.id.llEmptyState)
        tvBookingCount = view.findViewById(R.id.tvBookingCount)
        
        // Setup RecyclerView
        rvBookings.layoutManager = LinearLayoutManager(requireContext())
        bookingAdapter = GroundkeeperBookingAdapter(
            onAcceptClick = { booking ->
                updateBookingStatus(booking, BookingStatus.CONFIRMED)
            },
            onDeclineClick = { booking ->
                updateBookingStatus(booking, BookingStatus.CANCELLED)
            }
        )
        rvBookings.adapter = bookingAdapter
        
        // Load bookings from API
        loadBookings()
    }
    
    private fun loadBookings() {
        lifecycleScope.launch {
            try {
                // Fetch from API if online
                if (SyncManager.isOnline(requireContext())) {
                    val apiService = ApiClient.getPhpApiService(requireContext())
                    // Get all bookings (groundkeeper sees all bookings for their grounds)
                    val response = apiService.getBookings() // No userId filter = all bookings
                    
                    if (response.isSuccessful && response.body() != null) {
                        val apiBookings = response.body()!!
                        
                        // Filter bookings for this groundkeeper's grounds
                        // TODO: Filter by groundkeeper's userId or ground ownership
                        val myBookings = apiBookings // For now, show all
                        
                        // Save to local database
                        withContext(Dispatchers.IO) {
                            myBookings.forEach { booking ->
                                LocalDatabaseHelper.saveBooking(booking.copy(synced = true))
                            }
                        }
                        
                        bookingAdapter.updateItems(myBookings)
                        tvBookingCount.text = "${myBookings.size} total"
                        llEmptyState.visibility = if (myBookings.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error loading bookings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateBookingStatus(booking: Booking, newStatus: BookingStatus) {
        lifecycleScope.launch {
            try {
                val updatedBooking = booking.copy(status = newStatus)
                
                // Update in local database
                withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.updateBooking(updatedBooking)
                }
                
                // Sync to API
                if (SyncManager.isOnline(requireContext())) {
                    SyncManager.syncBooking(requireContext(), updatedBooking)
                }
                
                Toast.makeText(requireContext(), "Booking ${newStatus.name.lowercase()}", Toast.LENGTH_SHORT).show()
                
                // Reload bookings
                loadBookings()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error updating booking", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

