package com.example.smd_fyp.player.fragments

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
import com.example.smd_fyp.home.BookingAdapter
import com.example.smd_fyp.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookingsFragment : Fragment() {

    private lateinit var rvBookings: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var tvBookingCount: TextView
    private lateinit var bookingAdapter: BookingAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bookings, container, false)
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
        bookingAdapter = BookingAdapter(
            onReceiptClick = { booking ->
                // Show receipt activity
                val intent = android.content.Intent(requireContext(), com.example.smd_fyp.ReceiptActivity::class.java)
                intent.putExtra("booking_id", booking.id)
                startActivity(intent)
            },
            onReviewClick = { booking ->
                // Show review dialog
                val reviewDialog = com.example.smd_fyp.ReviewDialog.newInstance(
                    booking,
                    onReviewSubmitted = { booking, rating, reviewText ->
                        // Review is already saved in ReviewDialog
                        android.util.Log.d("BookingsFragment", "Review submitted: Rating=$rating, Review=$reviewText")
                    }
                )
                reviewDialog.show(parentFragmentManager, "ReviewDialog")
            }
        )
        rvBookings.adapter = bookingAdapter

        // Load bookings from database
        loadBookings()
    }

    private fun loadBookings() {
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        val context = context ?: return
        
        if (currentUser == null) {
            if (isAdded) {
                Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                llEmptyState.visibility = View.VISIBLE
            }
            return
        }
        
        // Observe local database (offline support)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                LocalDatabaseHelper.getBookingsByUser(currentUser.uid)?.collect { localBookings ->
                    if (!isAdded) return@collect
                    bookingAdapter.updateItems(localBookings)
                    tvBookingCount.text = "${localBookings.size} ${if (localBookings.size == 1) "booking" else "bookings"}"
                    llEmptyState.visibility = if (localBookings.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    Toast.makeText(context, "Error loading bookings: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    val response = apiService.getBookings(currentUser.uid)
                    
                    if (response.isSuccessful && response.body() != null) {
                        val apiBookings = response.body()!!
                        
                        // Save to local database
                        withContext(Dispatchers.IO) {
                            apiBookings.forEach { booking ->
                                LocalDatabaseHelper.saveBooking(booking.copy(synced = true))
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
}


