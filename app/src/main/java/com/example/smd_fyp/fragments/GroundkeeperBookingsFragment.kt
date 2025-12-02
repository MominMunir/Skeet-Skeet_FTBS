package com.example.smd_fyp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.smd_fyp.R

class GroundkeeperBookingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_groundkeeper_bookings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Accept and Decline buttons for pending bookings
        view.findViewById<View>(R.id.btnAccept1)?.setOnClickListener {
            // TODO: Accept booking
            Toast.makeText(requireContext(), "Booking accepted", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.btnDecline1)?.setOnClickListener {
            // TODO: Decline booking
            Toast.makeText(requireContext(), "Booking declined", Toast.LENGTH_SHORT).show()
        }
    }
}

