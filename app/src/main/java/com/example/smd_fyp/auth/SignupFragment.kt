package com.example.smd_fyp.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.smd_fyp.R

class SignupFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_signup, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Tabs: Login(inactive) | Sign Up(active)
        view.findViewById<TextView>(R.id.tvLoginTab)?.setOnClickListener {
            findNavController().navigate(R.id.action_signup_to_login)
        }
        // Back
        view.findViewById<View>(R.id.llBack)?.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}
