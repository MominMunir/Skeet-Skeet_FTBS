package com.example.smd_fyp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.smd_fyp.R
import com.google.android.material.textfield.TextInputEditText

class AdminAddGroundFragment : Fragment() {

    private lateinit var etGroundName: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var switchFloodlights: SwitchCompat
    private lateinit var switchParking: SwitchCompat

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_add_ground, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        etGroundName = view.findViewById(R.id.etGroundName)
        etLocation = view.findViewById(R.id.etLocation)
        etPrice = view.findViewById(R.id.etPrice)
        etDescription = view.findViewById(R.id.etDescription)
        switchFloodlights = view.findViewById(R.id.switchFloodlights)
        switchParking = view.findViewById(R.id.switchParking)

        // Image upload click listener
        view.findViewById<View>(R.id.cardImageUpload)?.setOnClickListener {
            // TODO: Implement image picker functionality
            Toast.makeText(requireContext(), "Image upload feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // Cancel button - go back to previous fragment
        view.findViewById<View>(R.id.btnCancel)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Submit button
        view.findViewById<View>(R.id.btnSubmit)?.setOnClickListener {
            if (validateForm()) {
                submitGround()
            }
        }
    }

    private fun validateForm(): Boolean {
        val name = etGroundName.text?.toString()?.trim()
        val location = etLocation.text?.toString()?.trim()
        val price = etPrice.text?.toString()?.trim()

        if (name.isNullOrEmpty()) {
            etGroundName.error = "Ground name is required"
            etGroundName.requestFocus()
            return false
        }

        if (location.isNullOrEmpty()) {
            etLocation.error = "Location is required"
            etLocation.requestFocus()
            return false
        }

        if (price.isNullOrEmpty()) {
            etPrice.error = "Price is required"
            etPrice.requestFocus()
            return false
        }

        try {
            val priceValue = price.toDouble()
            if (priceValue <= 0) {
                etPrice.error = "Price must be greater than 0"
                etPrice.requestFocus()
                return false
            }
        } catch (e: NumberFormatException) {
            etPrice.error = "Please enter a valid price"
            etPrice.requestFocus()
            return false
        }

        return true
    }

    private fun submitGround() {
        val name = etGroundName.text?.toString()?.trim() ?: ""
        val location = etLocation.text?.toString()?.trim() ?: ""
        val price = etPrice.text?.toString()?.trim() ?: ""
        val description = etDescription.text?.toString()?.trim() ?: ""
        val hasFloodlights = switchFloodlights.isChecked
        val hasParking = switchParking.isChecked

        // TODO: Implement API call to save ground data
        // For now, just show a success message
        Toast.makeText(
            requireContext(),
            "Ground added successfully!\nName: $name\nLocation: $location\nPrice: Rs. $price",
            Toast.LENGTH_LONG
        ).show()

        // Clear form
        clearForm()

        // Navigate back to overview
        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun clearForm() {
        etGroundName.text?.clear()
        etLocation.text?.clear()
        etPrice.text?.clear()
        etDescription.text?.clear()
        switchFloodlights.isChecked = true
        switchParking.isChecked = true
    }
}

