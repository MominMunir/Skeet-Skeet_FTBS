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

class AdminOverviewFragment : Fragment() {

    private lateinit var tvRevenueAmount: TextView
    private lateinit var tvTrendPercentage: TextView
    private lateinit var tvBookingRatio: TextView
    private lateinit var progressBooking: ProgressBar
    private lateinit var tvConfirmed: TextView
    private lateinit var tvPending: TextView
    private lateinit var tvCancelled: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize database
        LocalDatabaseHelper.initialize(requireContext())
        
        // Initialize views
        tvRevenueAmount = view.findViewById(R.id.tvRevenueAmount)
        tvTrendPercentage = view.findViewById(R.id.tvTrendPercentage)
        tvBookingRatio = view.findViewById(R.id.tvBookingRatio)
        progressBooking = view.findViewById(R.id.progressBooking)
        tvConfirmed = view.findViewById(R.id.tvConfirmed)
        tvPending = view.findViewById(R.id.tvPending)
        tvCancelled = view.findViewById(R.id.tvCancelled)
        
        // Setup quick action buttons
        view.findViewById<View>(R.id.btnSystemSettings)?.setOnClickListener {
            val settingsFragment = AdminSettingsFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, settingsFragment)
                .addToBackStack("admin_settings")
                .commit()
        }

        view.findViewById<View>(R.id.btnAddNewGround)?.setOnClickListener {
            val addGroundFragment = AdminAddGroundFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, addGroundFragment)
                .addToBackStack("add_ground")
                .commit()
        }
        
        view.findViewById<View>(R.id.btnViewReports)?.setOnClickListener {
            val analyticsFragment = AdminAnalyticsFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, analyticsFragment)
                .addToBackStack("analytics")
                .commit()
        }
        
        view.findViewById<View>(R.id.btnManageUsers)?.setOnClickListener {
            val usersFragment = AdminUsersFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, usersFragment)
                .addToBackStack("manage_users")
                .commit()
        }
        
        // Load real data from API
        loadOverviewData()
    }
    
    private fun loadOverviewData() {
        lifecycleScope.launch {
            try {
                // Load bookings from local DB first
                val allBookings = LocalDatabaseHelper.getAllBookingsSync()
                val allGrounds = withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.getAllGrounds()?.first() ?: emptyList()
                }
                val allUsers = LocalDatabaseHelper.getAllUsersSync()
                
                // Calculate current month stats
                val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val lastMonth = if (currentMonth == 0) 11 else currentMonth - 1
                val lastMonthYear = if (currentMonth == 0) currentYear - 1 else currentYear
                
                val currentMonthBookings = allBookings.filter { booking ->
                    try {
                        val bookingDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(booking.date)
                        val cal = Calendar.getInstance().apply { time = bookingDate }
                        cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                    } catch (e: Exception) {
                        false
                    }
                }
                
                val lastMonthBookings = allBookings.filter { booking ->
                    try {
                        val bookingDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(booking.date)
                        val cal = Calendar.getInstance().apply { time = bookingDate }
                        cal.get(Calendar.MONTH) == lastMonth && cal.get(Calendar.YEAR) == lastMonthYear
                    } catch (e: Exception) {
                        false
                    }
                }
                
                // Calculate revenue (from confirmed/completed bookings)
                val currentMonthRevenue = currentMonthBookings
                    .filter { it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.COMPLETED }
                    .sumOf { it.totalPrice }
                
                val lastMonthRevenue = lastMonthBookings
                    .filter { it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.COMPLETED }
                    .sumOf { it.totalPrice }
                
                val revenueChange = if (lastMonthRevenue > 0) {
                    ((currentMonthRevenue - lastMonthRevenue) / lastMonthRevenue * 100)
                } else if (currentMonthRevenue > 0) {
                    100.0
                } else {
                    0.0
                }
                
                // Booking statistics
                val confirmedCount = allBookings.count { it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.COMPLETED }
                val pendingCount = allBookings.count { it.status == BookingStatus.PENDING }
                val cancelledCount = allBookings.count { it.status == BookingStatus.CANCELLED }
                val totalBookings = allBookings.size
                
                val confirmedPercent = if (totalBookings > 0) (confirmedCount * 100 / totalBookings) else 0
                val pendingPercent = if (totalBookings > 0) (pendingCount * 100 / totalBookings) else 0
                
                // Active grounds
                val activeGrounds = allGrounds.count { it.available }
                
                // Total users
                val totalUsers = allUsers.size
                
                withContext(Dispatchers.Main) {
                    updateOverviewStats(
                        revenue = currentMonthRevenue,
                        revenueChange = revenueChange,
                        confirmedCount = confirmedCount,
                        pendingCount = pendingCount,
                        cancelledCount = cancelledCount,
                        totalBookings = totalBookings,
                        activeGrounds = activeGrounds,
                        totalUsers = totalUsers
                    )
                }
                
                // Fetch from API if online
                if (SyncManager.isOnline(requireContext())) {
                    val apiService = ApiClient.getPhpApiService(requireContext())
                    
                    // Fetch bookings
                    val bookingsResponse = apiService.getBookings()
                    if (bookingsResponse.isSuccessful && bookingsResponse.body() != null) {
                        withContext(Dispatchers.IO) {
                            bookingsResponse.body()!!.forEach { booking ->
                                LocalDatabaseHelper.saveBooking(booking.copy(synced = true))
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
                    
                    // Users are synced individually when they register/login
                    // So we'll use local database which should have all users
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun updateOverviewStats(
        revenue: Double,
        revenueChange: Double,
        confirmedCount: Int,
        pendingCount: Int,
        cancelledCount: Int,
        totalBookings: Int,
        activeGrounds: Int,
        totalUsers: Int
    ) {
        // Update revenue
        tvRevenueAmount.text = "Rs. ${String.format("%,.0f", revenue)}"
        
        // Update revenue trend
        val trendSign = if (revenueChange >= 0) "+" else ""
        tvTrendPercentage.text = "$trendSign${String.format("%.1f", revenueChange)}%"
        
        // Update booking ratio
        tvBookingRatio.text = "$confirmedCount/$totalBookings"
        
        // Update progress bar
        val confirmedPercent = if (totalBookings > 0) (confirmedCount * 100 / totalBookings) else 0
        val pendingPercent = if (totalBookings > 0) (pendingCount * 100 / totalBookings) else 0
        progressBooking.progress = confirmedPercent
        progressBooking.secondaryProgress = confirmedPercent + pendingPercent
        
        // Update booking counts
        tvConfirmed.text = "$confirmedCount Confirmed"
        tvPending.text = "$pendingCount Pending"
        tvCancelled.text = "$cancelledCount Cancelled"
    }
}

