package com.example.smd_fyp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.smd_fyp.R

class AdminBookingsFragment : Fragment() {

    private var currentFilter: BookingFilter = BookingFilter.ALL

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
        
        // Set up filter tab listeners
        view.findViewById<View>(R.id.tvAllBookings)?.setOnClickListener {
            updateFilter(BookingFilter.ALL)
        }
        view.findViewById<View>(R.id.tvPendingBookings)?.setOnClickListener {
            updateFilter(BookingFilter.PENDING)
        }
        view.findViewById<View>(R.id.tvConfirmedBookings)?.setOnClickListener {
            updateFilter(BookingFilter.CONFIRMED)
        }
        view.findViewById<View>(R.id.tvCancelledBookings)?.setOnClickListener {
            updateFilter(BookingFilter.CANCELLED)
        }
    }

    private fun updateFilter(filter: BookingFilter) {
        currentFilter = filter
        // Update UI and filter bookings list
        updateFilterTabs()
        // Load filtered bookings
    }

    private fun updateFilterTabs() {
        // Update tab styling based on current filter
    }
}

