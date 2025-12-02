package com.example.smd_fyp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.smd_fyp.R

class GroundkeeperMyGroundsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_groundkeeper_my_grounds, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup click listeners
        view.findViewById<View>(R.id.btnAddGround)?.setOnClickListener {
            // TODO: Navigate to add ground screen
            Toast.makeText(requireContext(), "Add Ground", Toast.LENGTH_SHORT).show()
        }
        
        // Ground toggle switches
        view.findViewById<Switch>(R.id.switchGround1)?.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update ground status
            Toast.makeText(requireContext(), "Ground 1: ${if (isChecked) "Active" else "Inactive"}", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<Switch>(R.id.switchGround2)?.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Update ground status
            Toast.makeText(requireContext(), "Ground 2: ${if (isChecked) "Active" else "Inactive"}", Toast.LENGTH_SHORT).show()
        }
        
        // View and Edit buttons
        view.findViewById<View>(R.id.btnView1)?.setOnClickListener {
            // TODO: View ground details
            Toast.makeText(requireContext(), "View Ground 1", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.btnEdit1)?.setOnClickListener {
            // TODO: Edit ground
            Toast.makeText(requireContext(), "Edit Ground 1", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.btnView2)?.setOnClickListener {
            // TODO: View ground details
            Toast.makeText(requireContext(), "View Ground 2", Toast.LENGTH_SHORT).show()
        }
        
        view.findViewById<View>(R.id.btnEdit2)?.setOnClickListener {
            // TODO: Edit ground
            Toast.makeText(requireContext(), "Edit Ground 2", Toast.LENGTH_SHORT).show()
        }
    }
}

