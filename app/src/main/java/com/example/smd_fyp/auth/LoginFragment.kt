package com.example.smd_fyp.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.smd_fyp.AdminDashboardActivity
import com.example.smd_fyp.HomeActivity
import com.example.smd_fyp.R

class LoginFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Tabs: Login(active) | Sign Up(inactive)
        view.findViewById<TextView>(R.id.tvSignUpTab)?.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_signup)
        }
        // Back
        view.findViewById<View>(R.id.llBack)?.setOnClickListener {
            findNavController().navigateUp()
        }

        // Sign In -> If role is Admin, go to Admin Dashboard; if Player, go to Home
        val spinner = view.findViewById<Spinner>(R.id.spinnerLoginAs)
        view.findViewById<View>(R.id.btnSignIn)?.setOnClickListener {
            val selected = spinner?.selectedItem?.toString()?.trim() ?: ""
            val adminLabel = getString(R.string.admin)
            val playerLabel = getString(R.string.player)
            
            when {
                selected.equals(adminLabel, ignoreCase = true) -> {
                    startActivity(Intent(requireContext(), AdminDashboardActivity::class.java))
                    requireActivity().finish()
                }
                selected.equals(playerLabel, ignoreCase = true) -> {
                    startActivity(Intent(requireContext(), HomeActivity::class.java))
                    requireActivity().finish()
                }
                else -> {
                    Toast.makeText(requireContext(), "Only Admin and Player flows are wired", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
