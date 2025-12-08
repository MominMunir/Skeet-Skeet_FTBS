package com.example.smd_fyp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smd_fyp.R
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.model.BookingStatus
import com.example.smd_fyp.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminAnalyticsFragment : Fragment() {

    private lateinit var tvBookingSuccessRate: TextView
    private lateinit var tvAverageRating: TextView
    private lateinit var tvGroundUtilization: TextView
    private lateinit var tvAvgBookingTime: TextView
    private lateinit var tvBookingsGrowth: TextView
    private lateinit var tvUserAcquisition: TextView
    private lateinit var tvRevenueGrowth: TextView
    private lateinit var progressBookingsGrowth: ProgressBar
    private lateinit var progressUserAcquisition: ProgressBar
    private lateinit var progressRevenueGrowth: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize database
        LocalDatabaseHelper.initialize(requireContext())
        
        // Initialize views
        tvBookingSuccessRate = view.findViewById(R.id.tvBookingSuccessRate)
        tvAverageRating = view.findViewById(R.id.tvAverageRating)
        tvGroundUtilization = view.findViewById(R.id.tvGroundUtilization)
        tvAvgBookingTime = view.findViewById(R.id.tvAvgBookingTime)
        tvBookingsGrowth = view.findViewById(R.id.tvBookingsGrowth)
        tvUserAcquisition = view.findViewById(R.id.tvUserAcquisition)
        tvRevenueGrowth = view.findViewById(R.id.tvRevenueGrowth)
        progressBookingsGrowth = view.findViewById(R.id.progressBookingsGrowth)
        progressUserAcquisition = view.findViewById(R.id.progressUserAcquisition)
        progressRevenueGrowth = view.findViewById(R.id.progressRevenueGrowth)
        
        // Load analytics data
        loadAnalyticsData()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        if (::tvBookingSuccessRate.isInitialized) {
            loadAnalyticsData()
        }
    }
    
    private fun loadAnalyticsData() {
        lifecycleScope.launch {
            try {
                // Fetch from API first if online
                if (SyncManager.isOnline(requireContext())) {
                    val apiService = ApiClient.getPhpApiService(requireContext())
                    
                    try {
                        // Fetch bookings
                        val bookingsResponse = apiService.getBookings()
                        if (bookingsResponse.isSuccessful && bookingsResponse.body() != null) {
                            withContext(Dispatchers.IO) {
                                bookingsResponse.body()!!.forEach { booking ->
                                    LocalDatabaseHelper.saveBooking(booking.copy(synced = true))
                                }
                            }
                        }
                        
                        // Fetch users
                        val usersResponse = apiService.getUsers()
                        if (usersResponse.isSuccessful && usersResponse.body() != null) {
                            withContext(Dispatchers.IO) {
                                usersResponse.body()!!.forEach { user ->
                                    LocalDatabaseHelper.saveUser(user.copy(synced = true))
                                }
                            }
                        }
                        
                        // Fetch grounds
                        val groundsResponse = apiService.getGrounds()
                        if (groundsResponse.isSuccessful && groundsResponse.body() != null) {
                            withContext(Dispatchers.IO) {
                                LocalDatabaseHelper.saveGrounds(groundsResponse.body()!!.map { it.copy(synced = true) })
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AdminAnalytics", "Error fetching data from API", e)
                    }
                }
                
                // Load data from local database
                val allBookings = withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.getAllBookingsSync()
                }
                val allGrounds = withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.getAllGrounds()?.first() ?: emptyList()
                }
                val allUsers = withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.getAllUsersSync()
                }
                
                android.util.Log.d("AdminAnalytics", "Loaded ${allBookings.size} bookings, ${allGrounds.size} grounds, ${allUsers.size} users")
                
                // Calculate analytics
                calculateAndUpdateAnalytics(allBookings, allGrounds, allUsers)
            } catch (e: Exception) {
                android.util.Log.e("AdminAnalytics", "Error loading analytics data", e)
                e.printStackTrace()
            }
        }
    }
    
    private suspend fun calculateAndUpdateAnalytics(
        allBookings: List<com.example.smd_fyp.model.Booking>,
        allGrounds: List<com.example.smd_fyp.model.GroundApi>,
        allUsers: List<com.example.smd_fyp.model.User>
    ) {
        withContext(Dispatchers.IO) {
            // 1. Booking Success Rate: (CONFIRMED + COMPLETED) / Total bookings * 100
            val totalBookings = allBookings.size
            val successfulBookings = allBookings.count { 
                it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.COMPLETED 
            }
            val bookingSuccessRate = if (totalBookings > 0) {
                (successfulBookings.toDouble() / totalBookings * 100).toInt()
            } else {
                0
            }
            
            // 2. Average Rating: Average of all ground ratings
            val averageRating = if (allGrounds.isNotEmpty()) {
                val ratings = allGrounds.mapNotNull { it.rating }
                if (ratings.isNotEmpty()) {
                    ratings.average()
                } else {
                    0.0
                }
            } else {
                0.0
            }
            
            // 3. Ground Utilization: (Number of bookings / Total available time slots) * 100
            // Simplified: (Bookings count / (Grounds count * estimated slots per day * 30)) * 100
            val activeGrounds = allGrounds.count { it.available }
            val groundUtilization = if (activeGrounds > 0 && totalBookings > 0) {
                // Estimate: assume 8 slots per day per ground, 30 days
                val estimatedSlots = activeGrounds * 8 * 30
                minOf((totalBookings.toDouble() / estimatedSlots * 100).toInt(), 100)
            } else {
                0
            }
            
            // 4. Average Booking Time: Average duration of bookings
            val avgBookingTime = if (allBookings.isNotEmpty()) {
                allBookings.map { it.duration }.average().toInt()
            } else {
                0
            }
            
            // 5. Bookings Growth: Compare current month vs last month
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val lastMonth = if (currentMonth == 0) 11 else currentMonth - 1
            val lastMonthYear = if (currentMonth == 0) currentYear - 1 else currentYear
            
            val currentMonthBookings = allBookings.filter { booking ->
                try {
                    val bookingDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(booking.date)
                    if (bookingDate != null) {
                        val cal = Calendar.getInstance().apply { time = bookingDate }
                        cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }
            
            val lastMonthBookings = allBookings.filter { booking ->
                try {
                    val bookingDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(booking.date)
                    if (bookingDate != null) {
                        val cal = Calendar.getInstance().apply { time = bookingDate }
                        cal.get(Calendar.MONTH) == lastMonth && cal.get(Calendar.YEAR) == lastMonthYear
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }
            
            val bookingsGrowth = if (lastMonthBookings.isNotEmpty()) {
                ((currentMonthBookings.size - lastMonthBookings.size).toDouble() / lastMonthBookings.size * 100).toInt()
            } else {
                if (currentMonthBookings.isNotEmpty()) 100 else 0
            }
            
            // 6. User Acquisition: New users this month vs last month
            val currentMonthUsers = allUsers.filter { user ->
                val userDate = Calendar.getInstance().apply { timeInMillis = user.createdAt }
                userDate.get(Calendar.MONTH) == currentMonth && userDate.get(Calendar.YEAR) == currentYear
            }
            
            val lastMonthUsers = allUsers.filter { user ->
                val userDate = Calendar.getInstance().apply { timeInMillis = user.createdAt }
                userDate.get(Calendar.MONTH) == lastMonth && userDate.get(Calendar.YEAR) == lastMonthYear
            }
            
            val userAcquisition = if (lastMonthUsers.isNotEmpty()) {
                ((currentMonthUsers.size - lastMonthUsers.size).toDouble() / lastMonthUsers.size * 100).toInt()
            } else {
                if (currentMonthUsers.isNotEmpty()) 100 else 0
            }
            
            // 7. Revenue Growth: Current month revenue vs last month revenue
            val currentMonthRevenue = currentMonthBookings
                .filter { it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.COMPLETED }
                .sumOf { it.totalPrice }
            
            val lastMonthRevenue = lastMonthBookings
                .filter { it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.COMPLETED }
                .sumOf { it.totalPrice }
            
            val revenueGrowth = if (lastMonthRevenue > 0) {
                ((currentMonthRevenue - lastMonthRevenue) / lastMonthRevenue * 100).toInt()
            } else {
                if (currentMonthRevenue > 0) 100 else 0
            }
            
            android.util.Log.d("AdminAnalytics", "Booking Success Rate: $bookingSuccessRate%")
            android.util.Log.d("AdminAnalytics", "Average Rating: $averageRating")
            android.util.Log.d("AdminAnalytics", "Ground Utilization: $groundUtilization%")
            android.util.Log.d("AdminAnalytics", "Avg Booking Time: $avgBookingTime hours")
            android.util.Log.d("AdminAnalytics", "Bookings Growth: $bookingsGrowth%")
            android.util.Log.d("AdminAnalytics", "User Acquisition: $userAcquisition%")
            android.util.Log.d("AdminAnalytics", "Revenue Growth: $revenueGrowth%")
            
            // Update UI on main thread
            withContext(Dispatchers.Main) {
                tvBookingSuccessRate.text = "$bookingSuccessRate%"
                tvAverageRating.text = String.format("%.1f", averageRating)
                tvGroundUtilization.text = "$groundUtilization%"
                tvAvgBookingTime.text = "${avgBookingTime}hr"
                
                val bookingsGrowthText = if (bookingsGrowth >= 0) "+$bookingsGrowth%" else "$bookingsGrowth%"
                tvBookingsGrowth.text = bookingsGrowthText
                tvBookingsGrowth.setTextColor(
                    if (bookingsGrowth >= 0) {
                        requireContext().getColor(R.color.green_600)
                    } else {
                        requireContext().getColor(R.color.red_600)
                    }
                )
                progressBookingsGrowth.progress = minOf(Math.abs(bookingsGrowth), 100)
                
                val userAcquisitionText = if (userAcquisition >= 0) "+$userAcquisition%" else "$userAcquisition%"
                tvUserAcquisition.text = userAcquisitionText
                tvUserAcquisition.setTextColor(
                    if (userAcquisition >= 0) {
                        requireContext().getColor(R.color.green_600)
                    } else {
                        requireContext().getColor(R.color.red_600)
                    }
                )
                progressUserAcquisition.progress = minOf(Math.abs(userAcquisition), 100)
                
                val revenueGrowthText = if (revenueGrowth >= 0) "+$revenueGrowth%" else "$revenueGrowth%"
                tvRevenueGrowth.text = revenueGrowthText
                tvRevenueGrowth.setTextColor(
                    if (revenueGrowth >= 0) {
                        requireContext().getColor(R.color.green_600)
                    } else {
                        requireContext().getColor(R.color.red_600)
                    }
                )
                progressRevenueGrowth.progress = minOf(Math.abs(revenueGrowth), 100)
            }
        }
    }
}
