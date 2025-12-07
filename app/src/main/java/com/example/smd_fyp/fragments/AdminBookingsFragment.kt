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
import com.example.smd_fyp.home.BookingAdapter
import com.example.smd_fyp.model.Booking
import com.example.smd_fyp.model.BookingStatus
import com.example.smd_fyp.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminBookingsFragment : Fragment() {

    private lateinit var rvBookings: RecyclerView
    private lateinit var tvAllBookings: TextView
    private lateinit var tvPendingBookings: TextView
    private lateinit var tvConfirmedBookings: TextView
    private lateinit var tvCancelledBookings: TextView
    private lateinit var bookingAdapter: BookingAdapter
    private var currentFilter: BookingFilter = BookingFilter.ALL
    private val allBookings = mutableListOf<Booking>()

    enum class BookingFilter {
        ALL, PENDING, CONFIRMED, CANCELLED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_bookings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize database
        LocalDatabaseHelper.initialize(requireContext())
        
        // Initialize views
        rvBookings = view.findViewById(R.id.rvBookings)
        tvAllBookings = view.findViewById(R.id.tvAllBookings)
        tvPendingBookings = view.findViewById(R.id.tvPendingBookings)
        tvConfirmedBookings = view.findViewById(R.id.tvConfirmedBookings)
        tvCancelledBookings = view.findViewById(R.id.tvCancelledBookings)
        
        // Setup RecyclerView
        rvBookings.layoutManager = LinearLayoutManager(requireContext())
        bookingAdapter = BookingAdapter(
            onReceiptClick = { booking ->
                Toast.makeText(requireContext(), "Receipt for ${booking.groundName}", Toast.LENGTH_SHORT).show()
            },
            onReviewClick = { booking ->
                Toast.makeText(requireContext(), "Review ${booking.groundName}", Toast.LENGTH_SHORT).show()
            }
        )
        rvBookings.adapter = bookingAdapter
        
        // Set up filter tab listeners
        tvAllBookings.setOnClickListener { updateFilter(BookingFilter.ALL) }
        tvPendingBookings.setOnClickListener { updateFilter(BookingFilter.PENDING) }
        tvConfirmedBookings.setOnClickListener { updateFilter(BookingFilter.CONFIRMED) }
        tvCancelledBookings.setOnClickListener { updateFilter(BookingFilter.CANCELLED) }
        
        // Load bookings from API
        loadBookings()
    }

    private fun updateFilter(filter: BookingFilter) {
        currentFilter = filter
        updateFilterTabs()
        filterBookings()
    }

    private fun updateFilterTabs() {
        // Reset all tabs
        tvAllBookings.setBackgroundResource(R.drawable.bg_tab_inactive_dashboard)
        tvAllBookings.setTextColor(requireContext().getColor(R.color.text_primary))
        tvPendingBookings.setBackgroundResource(R.drawable.bg_tab_inactive_dashboard)
        tvPendingBookings.setTextColor(requireContext().getColor(R.color.text_primary))
        tvConfirmedBookings.setBackgroundResource(R.drawable.bg_tab_inactive_dashboard)
        tvConfirmedBookings.setTextColor(requireContext().getColor(R.color.text_primary))
        tvCancelledBookings.setBackgroundResource(R.drawable.bg_tab_inactive_dashboard)
        tvCancelledBookings.setTextColor(requireContext().getColor(R.color.text_primary))
        
        // Set active tab
        val activeTab = when (currentFilter) {
            BookingFilter.ALL -> tvAllBookings
            BookingFilter.PENDING -> tvPendingBookings
            BookingFilter.CONFIRMED -> tvConfirmedBookings
            BookingFilter.CANCELLED -> tvCancelledBookings
        }
        activeTab.setBackgroundResource(R.drawable.bg_tab_active_dashboard)
        activeTab.setTextColor(requireContext().getColor(R.color.green_600))
    }
    
    private fun filterBookings() {
        val filtered = when (currentFilter) {
            BookingFilter.ALL -> allBookings
            BookingFilter.PENDING -> allBookings.filter { it.status == BookingStatus.PENDING }
            BookingFilter.CONFIRMED -> allBookings.filter { it.status == BookingStatus.CONFIRMED }
            BookingFilter.CANCELLED -> allBookings.filter { it.status == BookingStatus.CANCELLED }
        }
        bookingAdapter.updateItems(filtered)
    }
    
    private fun loadBookings() {
        lifecycleScope.launch {
            try {
                // Fetch from API if online
                if (SyncManager.isOnline(requireContext())) {
                    val apiService = ApiClient.getPhpApiService(requireContext())
                    val response = apiService.getBookings() // Get all bookings (no userId filter)
                    
                    if (response.isSuccessful && response.body() != null) {
                        val apiBookings = response.body()!!
                        
                        // Save to local database
                        withContext(Dispatchers.IO) {
                            apiBookings.forEach { booking ->
                                LocalDatabaseHelper.saveBooking(booking.copy(synced = true))
                            }
                        }
                        
                        allBookings.clear()
                        allBookings.addAll(apiBookings)
                        filterBookings()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error loading bookings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

