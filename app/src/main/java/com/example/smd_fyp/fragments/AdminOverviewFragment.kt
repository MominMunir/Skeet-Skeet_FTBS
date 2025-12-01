package com.example.smd_fyp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.smd_fyp.R

class AdminOverviewFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_overview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize views and set up click listeners here
        view.findViewById<View>(R.id.btnSystemSettings)?.setOnClickListener {
            // Load AdminSettingsFragment using FragmentManager
            val settingsFragment = AdminSettingsFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, settingsFragment)
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.btnAddNewGround)?.setOnClickListener {
            // Load AdminAddGroundFragment using FragmentManager
            val addGroundFragment = AdminAddGroundFragment()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, addGroundFragment)
                .addToBackStack(null)
                .commit()
        }
    }
}

