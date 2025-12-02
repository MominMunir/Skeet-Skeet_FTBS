package com.example.smd_fyp.player.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.smd_fyp.R

class BookingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bookings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup click listeners for booking cards
        view.findViewById<View>(R.id.btnReceipt1)?.setOnClickListener {
            // TODO: Show receipt
        }
        
        view.findViewById<View>(R.id.btnReview1)?.setOnClickListener {
            // TODO: Show review dialog
        }
        
        view.findViewById<View>(R.id.btnReceipt2)?.setOnClickListener {
            // TODO: Show receipt
        }
    }
}


