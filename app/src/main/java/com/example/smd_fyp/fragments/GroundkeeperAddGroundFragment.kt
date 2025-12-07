package com.example.smd_fyp.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smd_fyp.R
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.api.ImageUploadHelper
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.model.GroundApi
import com.example.smd_fyp.sync.SyncManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.UUID

class GroundkeeperAddGroundFragment : Fragment() {

    private lateinit var etGroundName: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var etPrice: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var switchFloodlights: SwitchCompat
    private lateinit var switchParking: SwitchCompat
    private lateinit var ivImagePreview: ImageView
    private lateinit var ivImagePlaceholder: ImageView
    private lateinit var tvImagePlaceholder: TextView
    
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null

    private val imagePickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data?.data != null) {
                // Image from gallery
                val uri: Uri = data.data!!
                selectedImageUri = uri
                loadImageIntoView(uri)
            } else {
                // Image from camera
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    data?.extras?.getParcelable("data", Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    data?.extras?.getParcelable("data") as? Bitmap
                }
                if (bitmap != null) {
                    ivImagePreview.scaleType = ImageView.ScaleType.CENTER_CROP
                    ivImagePreview.setImageBitmap(bitmap)
                    ivImagePreview.visibility = View.VISIBLE
                    ivImagePlaceholder.visibility = View.GONE
                    tvImagePlaceholder.visibility = View.GONE
                    // Note: For camera images, we'll need to save to a file first
                    // For now, we'll handle it in the upload step
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_add_ground, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize database
        LocalDatabaseHelper.initialize(requireContext())

        // Initialize views
        etGroundName = view.findViewById(R.id.etGroundName)
        etLocation = view.findViewById(R.id.etLocation)
        etPrice = view.findViewById(R.id.etPrice)
        etDescription = view.findViewById(R.id.etDescription)
        switchFloodlights = view.findViewById(R.id.switchFloodlights)
        switchParking = view.findViewById(R.id.switchParking)
        ivImagePreview = view.findViewById(R.id.ivImagePreview)
        ivImagePlaceholder = view.findViewById(R.id.ivImagePlaceholder)
        tvImagePlaceholder = view.findViewById(R.id.tvImagePlaceholder)

        // Image upload click listener
        view.findViewById<View>(R.id.cardImageUpload)?.setOnClickListener {
            showImagePickerDialog()
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

    private fun showImagePickerDialog() {
        val options = arrayOf("Choose from Gallery", "Take Photo", "Cancel")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Ground Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageFromGallery()
                    1 -> takePhoto()
                    2 -> { /* Cancel */ }
                }
            }
            .show()
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imagePickerLauncher.launch(intent)
    }

    private fun loadImageIntoView(uri: Uri) {
        try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            ivImagePreview.scaleType = ImageView.ScaleType.CENTER_CROP
            ivImagePreview.setImageBitmap(bitmap)
            ivImagePreview.visibility = View.VISIBLE
            ivImagePlaceholder.visibility = View.GONE
            tvImagePlaceholder.visibility = View.GONE
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show()
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

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val context = this@GroundkeeperAddGroundFragment.context ?: return@launch
                if (!isAdded) return@launch
                
                // Show loading
                view?.findViewById<View>(R.id.btnSubmit)?.isEnabled = false
                
                // Upload image first if selected
                var imageUrl: String? = null
                var imagePath: String? = null
                
                if (selectedImageUri != null) {
                    withContext(Dispatchers.IO) {
                        val uploadResult = ImageUploadHelper.uploadImage(
                            context,
                            selectedImageUri!!,
                            "grounds"
                        )
                        if (uploadResult.isSuccess) {
                            imageUrl = uploadResult.getOrNull()
                            imagePath = uploadResult.getOrNull()
                        } else {
                            withContext(Dispatchers.Main) {
                                if (isAdded) {
                                    Toast.makeText(
                                        context,
                                        "Image upload failed: ${uploadResult.exceptionOrNull()?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                }

                // Create ground object
                val groundId = UUID.randomUUID().toString()
                val priceValue = price.toDouble()
                val priceText = "Rs. ${priceValue.toInt()}/hour"
                
                val ground = GroundApi(
                    id = groundId,
                    name = name,
                    location = location,
                    price = priceValue,
                    priceText = priceText,
                    rating = 0.0,
                    ratingText = "0.0",
                    imageUrl = imageUrl,
                    imagePath = imagePath,
                    hasFloodlights = hasFloodlights,
                    hasParking = hasParking,
                    description = if (description.isNotEmpty()) description else null,
                    available = true,
                    synced = false
                )

                // Save to local database first (offline support)
                withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.saveGround(ground)
                }

                // Sync to API if online
                if (SyncManager.isOnline(context)) {
                    val syncResult = withContext(Dispatchers.IO) {
                        SyncManager.syncGround(context, ground)
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            if (syncResult.isSuccess) {
                                Toast.makeText(
                                    context,
                                    "Ground added successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Ground saved locally. Will sync when online.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            
                            // Clear form
                            clearForm()
                            
                            // Navigate back to my grounds
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                    }
                } else {
                    // Offline - just saved locally
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            Toast.makeText(
                                context,
                                "Ground saved locally. Will sync when online.",
                                Toast.LENGTH_LONG
                            ).show()
                            
                            // Clear form
                            clearForm()
                            
                            // Navigate back
                            requireActivity().supportFragmentManager.popBackStack()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "Error adding ground: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        view?.findViewById<View>(R.id.btnSubmit)?.isEnabled = true
                    }
                }
            }
        }
    }

    private fun clearForm() {
        etGroundName.text?.clear()
        etLocation.text?.clear()
        etPrice.text?.clear()
        etDescription.text?.clear()
        switchFloodlights.isChecked = true
        switchParking.isChecked = true
        selectedImageUri = null
        uploadedImageUrl = null
        ivImagePreview.visibility = View.GONE
        ivImagePlaceholder.visibility = View.VISIBLE
        tvImagePlaceholder.visibility = View.VISIBLE
    }
}
