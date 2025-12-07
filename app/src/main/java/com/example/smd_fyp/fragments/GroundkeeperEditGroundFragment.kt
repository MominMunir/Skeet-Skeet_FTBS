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
import com.example.smd_fyp.api.ImageUploadHelper
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.model.GroundApi
import com.example.smd_fyp.sync.SyncManager
import com.example.smd_fyp.utils.GlideHelper
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.InputStream

class GroundkeeperEditGroundFragment : Fragment() {

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
    private var currentGround: GroundApi? = null
    private var imageChanged = false

    private val imagePickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data?.data != null) {
                val uri: Uri = data.data!!
                selectedImageUri = uri
                imageChanged = true
                loadImageIntoView(uri)
            } else {
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
                    imageChanged = true
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

        // Get ground ID from arguments
        val groundId = arguments?.getString("ground_id")
        if (groundId == null) {
            Toast.makeText(requireContext(), "Ground ID not found", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
            return
        }

        // Load ground from database
        loadGroundFromDatabase(groundId)

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

        // Change title
        view.findViewById<TextView>(R.id.lblGroundName)?.parent?.let { parent ->
            if (parent is ViewGroup) {
                parent.findViewById<TextView>(R.id.lblGroundName)?.let { title ->
                    title.text = "Edit Ground"
                }
            }
        }

        // Load ground data
        loadGroundData()

        // Image upload click listener
        view.findViewById<View>(R.id.cardImageUpload)?.setOnClickListener {
            showImagePickerDialog()
        }

        // Cancel button
        view.findViewById<View>(R.id.btnCancel)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Submit button
        view.findViewById<View>(R.id.btnSubmit)?.setOnClickListener {
            if (validateForm()) {
                updateGround()
            }
        }
    }

    private fun loadGroundFromDatabase(groundId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val ground = withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.getGround(groundId)
                }
                
                if (ground == null) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Ground not found", Toast.LENGTH_SHORT).show()
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                    return@launch
                }
                
                currentGround = ground
                loadGroundData()
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error loading ground: ${e.message}", Toast.LENGTH_SHORT).show()
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        }
    }
    
    private fun loadGroundData() {
        currentGround?.let { ground ->
            etGroundName.setText(ground.name)
            etLocation.setText(ground.location)
            etPrice.setText(ground.price.toInt().toString())
            etDescription.setText(ground.description ?: "")
            switchFloodlights.isChecked = ground.hasFloodlights
            switchParking.isChecked = ground.hasParking

            // Load image if exists
            if (!ground.imageUrl.isNullOrEmpty()) {
                GlideHelper.loadImage(
                    context = requireContext(),
                    imageUrl = ground.imageUrl,
                    imageView = ivImagePreview,
                    placeholder = R.drawable.ic_launcher_foreground,
                    errorDrawable = R.drawable.ic_launcher_foreground,
                    tag = "EditGround"
                )
                ivImagePreview.visibility = View.VISIBLE
                ivImagePlaceholder.visibility = View.GONE
                tvImagePlaceholder.visibility = View.GONE
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

    private fun updateGround() {
        val ground = currentGround ?: return
        val name = etGroundName.text?.toString()?.trim() ?: ""
        val location = etLocation.text?.toString()?.trim() ?: ""
        val price = etPrice.text?.toString()?.trim() ?: ""
        val description = etDescription.text?.toString()?.trim() ?: ""
        val hasFloodlights = switchFloodlights.isChecked
        val hasParking = switchParking.isChecked

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val context = this@GroundkeeperEditGroundFragment.context ?: return@launch
                if (!isAdded) return@launch

                view?.findViewById<View>(R.id.btnSubmit)?.isEnabled = false
                
                // Upload new image if changed
                var imageUrl = ground.imageUrl
                var imagePath = ground.imagePath
                
                if (imageChanged && selectedImageUri != null) {
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

                // Update ground object
                val priceValue = price.toDouble()
                val priceText = "Rs. ${priceValue.toInt()}/hour"
                
                val updatedGround = ground.copy(
                    name = name,
                    location = location,
                    price = priceValue,
                    priceText = priceText,
                    imageUrl = imageUrl,
                    imagePath = imagePath,
                    hasFloodlights = hasFloodlights,
                    hasParking = hasParking,
                    description = if (description.isNotEmpty()) description else null,
                    synced = false
                )

                // Update in local database
                withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.updateGround(updatedGround)
                }

                // Sync to API if online
                if (SyncManager.isOnline(context)) {
                    val syncResult = withContext(Dispatchers.IO) {
                        SyncManager.syncGround(context, updatedGround)
                    }
                    
                    if (syncResult.isSuccess) {
                        withContext(Dispatchers.Main) {
                            if (isAdded) {
                                Toast.makeText(
                                    context,
                                    "Ground updated successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                requireActivity().supportFragmentManager.popBackStack()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            if (isAdded) {
                                Toast.makeText(
                                    context,
                                    "Ground updated locally. Will sync when online.",
                                    Toast.LENGTH_LONG
                                ).show()
                                requireActivity().supportFragmentManager.popBackStack()
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            Toast.makeText(
                                context,
                                "Ground updated locally. Will sync when online.",
                                Toast.LENGTH_LONG
                            ).show()
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
                            "Error updating ground: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        view?.findViewById<View>(R.id.btnSubmit)?.isEnabled = true
                    }
                }
            }
        }
    }
}
