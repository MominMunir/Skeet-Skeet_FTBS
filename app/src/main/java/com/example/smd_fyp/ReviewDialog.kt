package com.example.smd_fyp

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.model.Booking
import com.example.smd_fyp.model.Review
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ReviewDialog : DialogFragment() {

    private var booking: Booking? = null
    private var onReviewSubmitted: ((Booking, Float, String) -> Unit)? = null

    companion object {
        fun newInstance(booking: Booking, onReviewSubmitted: (Booking, Float, String) -> Unit): ReviewDialog {
            return ReviewDialog().apply {
                this.booking = booking
                this.onReviewSubmitted = onReviewSubmitted
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_review, null)

        val tvGroundName: TextView = view.findViewById(R.id.tvGroundName)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBar)
        val etReview: TextInputEditText = view.findViewById(R.id.etReview)
        val btnSubmit: Button = view.findViewById(R.id.btnSubmit)
        val btnCancel: Button = view.findViewById(R.id.btnCancel)

        booking?.let {
            tvGroundName.text = it.groundName
        }

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating
            val reviewText = etReview.text.toString().trim()

            if (rating == 0f) {
                Toast.makeText(requireContext(), "Please provide a rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (reviewText.isEmpty()) {
                Toast.makeText(requireContext(), "Please write a review", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            booking?.let { booking ->
                val currentUser = FirebaseAuthHelper.getCurrentUser()
                if (currentUser == null) {
                    Toast.makeText(requireContext(), "Please login to submit a review", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                // Save review to database
                lifecycleScope.launch {
                    try {
                        val review = Review(
                            id = UUID.randomUUID().toString(),
                            userId = currentUser.uid,
                            groundId = booking.groundId,
                            bookingId = booking.id,
                            rating = rating,
                            reviewText = reviewText,
                            createdAt = System.currentTimeMillis(),
                            synced = false
                        )
                        
                        withContext(Dispatchers.IO) {
                            LocalDatabaseHelper.saveReview(review)
                            // Update ground rating
                            LocalDatabaseHelper.updateGroundRating(booking.groundId)
                            
                            // Sync review to PHP API if online
                            if (com.example.smd_fyp.sync.SyncManager.isOnline(requireContext())) {
                                com.example.smd_fyp.sync.SyncManager.syncReview(requireContext(), review)
                            }
                        }
                        
                        onReviewSubmitted?.invoke(booking, rating, reviewText)
                        Toast.makeText(requireContext(), "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                        dismiss()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(requireContext(), "Error submitting review: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        builder.setView(view)
        return builder.create()
    }
}
