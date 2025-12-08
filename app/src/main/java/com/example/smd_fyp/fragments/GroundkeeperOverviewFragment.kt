package com.example.smd_fyp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.R
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.home.RecentBookingAdapter
import com.example.smd_fyp.model.Booking
import com.example.smd_fyp.model.BookingStatus
import com.example.smd_fyp.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GroundkeeperOverviewFragment : Fragment() {

    private lateinit var tvTodaysBookings: TextView
    private lateinit var tvMonthlyRevenue: TextView
    private lateinit var rvRecentBookings: RecyclerView
    private lateinit var recentBookingAdapter: RecentBookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_groundkeeper_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d("GroundkeeperOverview", "Fragment onViewCreated called")
        
        // Initialize database
        LocalDatabaseHelper.initialize(requireContext())
        
        // Initialize views
        tvTodaysBookings = view.findViewById(R.id.tvTodaysBookingsValue)
        tvMonthlyRevenue = view.findViewById(R.id.tvMonthlyRevenueValue)
        rvRecentBookings = view.findViewById(R.id.rvRecentBookings)
        
        // Setup RecyclerView for recent bookings
        rvRecentBookings.layoutManager = LinearLayoutManager(requireContext())
        recentBookingAdapter = RecentBookingAdapter(emptyList())
        rvRecentBookings.adapter = recentBookingAdapter
        
        android.util.Log.d("GroundkeeperOverview", "Views initialized, starting data fetch")
        
        // Fetch from API first, then observe local database
        fetchAndLoadData()
    }
    
    override fun onResume() {
        super.onResume()
        android.util.Log.d("GroundkeeperOverview", "Fragment onResume called")
        // Refresh data when fragment becomes visible
        if (::tvTodaysBookings.isInitialized) {
            fetchAndLoadData()
        }
    }
    
    private fun fetchAndLoadData() {
        android.util.Log.d("GroundkeeperOverview", "fetchAndLoadData called")
        lifecycleScope.launch {
            try {
                // Fetch from API first if online
                if (SyncManager.isOnline(requireContext())) {
                    val apiService = ApiClient.getPhpApiService(requireContext())
                    
                    try {
                        // Fetch bookings
                        android.util.Log.d("GroundkeeperOverview", "Fetching bookings from API...")
                        val bookingsResponse = withContext(Dispatchers.IO) {
                            apiService.getBookings()
                        }
                        android.util.Log.d("GroundkeeperOverview", "API Response Code: ${bookingsResponse.code()}")
                        
                        if (bookingsResponse.isSuccessful && bookingsResponse.body() != null) {
                            val apiBookings = bookingsResponse.body()!!
                            android.util.Log.d("GroundkeeperOverview", "Fetched ${apiBookings.size} bookings from API")
                            
                            if (apiBookings.isNotEmpty()) {
                                android.util.Log.d("GroundkeeperOverview", "First booking sample: id=${apiBookings[0].id}, date=${apiBookings[0].date}, status=${apiBookings[0].status}, price=${apiBookings[0].totalPrice}")
                            }
                            
                            withContext(Dispatchers.IO) {
                                apiBookings.forEach { booking ->
                                    LocalDatabaseHelper.saveBooking(booking.copy(synced = true))
                                }
                            }
                            android.util.Log.d("GroundkeeperOverview", "Saved ${apiBookings.size} bookings to local DB")
                        } else {
                            val errorBody = bookingsResponse.errorBody()?.string()
                            android.util.Log.e("GroundkeeperOverview", "API Error: ${bookingsResponse.code()} - ${bookingsResponse.message()}, Body: $errorBody")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GroundkeeperOverview", "Error fetching bookings from API", e)
                        e.printStackTrace()
                    }
                } else {
                    android.util.Log.d("GroundkeeperOverview", "Device is offline, using local data only")
                }
                
                // Load from local database directly
                val allBookings = withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.getAllBookingsSync()
                }
                
                android.util.Log.d("GroundkeeperOverview", "Loaded ${allBookings.size} bookings from local DB")
                
                if (allBookings.isNotEmpty()) {
                    allBookings.take(5).forEachIndexed { index, booking ->
                        android.util.Log.d("GroundkeeperOverview", "Booking $index: id=${booking.id}, date='${booking.date}', time='${booking.time}', status=${booking.status}, price=${booking.totalPrice}")
                    }
                } else {
                    android.util.Log.w("GroundkeeperOverview", "No bookings found in local database!")
                }
                
                // Calculate stats immediately
                calculateAndUpdateStats(allBookings)
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("GroundkeeperOverview", "Error loading overview data", e)
            }
        }
        
        // Observe for future updates in a separate coroutine
        lifecycleScope.launch {
            try {
                LocalDatabaseHelper.getAllBookings()?.collect { updatedBookings ->
                    android.util.Log.d("GroundkeeperOverview", "Flow emitted ${updatedBookings.size} bookings")
                    calculateAndUpdateStats(updatedBookings)
                }
            } catch (e: Exception) {
                android.util.Log.e("GroundkeeperOverview", "Error observing bookings", e)
            }
        }
    }
    
    private suspend fun calculateAndUpdateStats(allBookings: List<Booking>) {
        withContext(Dispatchers.IO) {
            android.util.Log.d("GroundkeeperOverview", "Calculating stats for ${allBookings.size} bookings")
            
            // Get today's date string in yyyy-MM-dd format
            val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
            
            // Log sample booking dates for debugging
            if (allBookings.isNotEmpty()) {
                allBookings.take(5).forEachIndexed { index, booking ->
                    android.util.Log.d("GroundkeeperOverview", "Booking $index - Date: '${booking.date}', Today: '$todayDateStr', Match: ${booking.date == todayDateStr}, Status: ${booking.status}, Price: ${booking.totalPrice}")
                }
            } else {
                android.util.Log.w("GroundkeeperOverview", "No bookings found in database!")
            }
            
            // Today's bookings - simple string comparison (exact match)
            val todaysBookings = allBookings.filter { booking ->
                val matches = booking.date == todayDateStr
                if (matches) {
                    android.util.Log.d("GroundkeeperOverview", "Found today's booking: ${booking.groundName}, date: ${booking.date}")
                }
                matches
            }
            
            // Monthly bookings - get current month/year
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            
            android.util.Log.d("GroundkeeperOverview", "Current month: $currentMonth, year: $currentYear")
            
            val monthBookings = allBookings.filter { booking ->
                try {
                    if (booking.date.isNotEmpty()) {
                        val bookingDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(booking.date)
                        if (bookingDate != null) {
                            val cal = Calendar.getInstance().apply { time = bookingDate }
                            val bookingMonth = cal.get(Calendar.MONTH)
                            val bookingYear = cal.get(Calendar.YEAR)
                            val matches = bookingMonth == currentMonth && bookingYear == currentYear
                            if (matches) {
                                android.util.Log.d("GroundkeeperOverview", "Found monthly booking: ${booking.groundName}, date: ${booking.date}, status: ${booking.status}, price: ${booking.totalPrice}")
                            }
                            matches
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GroundkeeperOverview", "Error parsing date: '${booking.date}'", e)
                    false
                }
            }
            
            // Calculate monthly revenue (from confirmed/completed bookings)
            val monthlyRevenue = monthBookings
                .filter { it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.COMPLETED }
                .sumOf { it.totalPrice }
            
            // Recent bookings (last 5, sorted by date/time descending)
            val recentBookings = allBookings
                .filter { it.date.isNotEmpty() && it.time.isNotEmpty() }
                .sortedByDescending { "${it.date} ${it.time}" }
                .take(5)
            
            android.util.Log.d("GroundkeeperOverview", "Today's date: $todayDateStr")
            android.util.Log.d("GroundkeeperOverview", "Total bookings: ${allBookings.size}")
            android.util.Log.d("GroundkeeperOverview", "Today's bookings: ${todaysBookings.size}")
            android.util.Log.d("GroundkeeperOverview", "Monthly bookings: ${monthBookings.size}")
            android.util.Log.d("GroundkeeperOverview", "Monthly revenue: $monthlyRevenue")
            android.util.Log.d("GroundkeeperOverview", "Recent bookings: ${recentBookings.size}")
            
            withContext(Dispatchers.Main) {
                updateOverviewStats(todaysBookings.size, monthlyRevenue, recentBookings)
            }
        }
    }
    
    private fun updateOverviewStats(todaysBookings: Int, monthlyRevenue: Double, recentBookings: List<Booking>) {
        android.util.Log.d("GroundkeeperOverview", "Updating UI - Today: $todaysBookings, Revenue: $monthlyRevenue, Recent: ${recentBookings.size}")
        
        tvTodaysBookings.text = todaysBookings.toString()
        tvMonthlyRevenue.text = "Rs. ${String.format("%,.0f", monthlyRevenue)}"
        recentBookingAdapter.updateItems(recentBookings)
        
        android.util.Log.d("GroundkeeperOverview", "UI Updated successfully")
    }
}
