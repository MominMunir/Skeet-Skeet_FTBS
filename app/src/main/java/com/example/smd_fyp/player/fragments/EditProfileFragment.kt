package com.example.smd_fyp.player.fragments

import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.smd_fyp.R
import com.google.android.material.textfield.TextInputEditText
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream

class EditProfileFragment : Fragment() {

    private lateinit var ivProfilePicture: ImageView
    private lateinit var etUsername: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etAge: TextInputEditText
    private lateinit var tvGender: TextView
    private lateinit var etLocation: TextInputEditText
    private lateinit var tvChangePassword: TextView
    
    private lateinit var sharedPreferences: SharedPreferences
    private var profileImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data?.data != null) {
                // Image from gallery
                val selectedImageUri: Uri = data.data!!
                profileImageUri = selectedImageUri
                loadImageIntoView(selectedImageUri)
            } else {
                // Image from camera
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    data?.extras?.getParcelable("data", Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    data?.extras?.getParcelable("data") as? Bitmap
                }
                if (bitmap != null) {
                    ivProfilePicture.scaleType = ImageView.ScaleType.CENTER_CROP
                    ivProfilePicture.setImageBitmap(bitmap)
                    saveBitmapToUri(bitmap)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("user_profile", 0)
        
        // Initialize views
        initializeViews()
        
        // Load saved profile data
        loadProfileData()
        
        // Setup click listeners
        setupClickListeners()
    }

    private fun initializeViews() {
        ivProfilePicture = requireView().findViewById(R.id.ivProfilePicture)
        etUsername = requireView().findViewById(R.id.etUsername)
        etFullName = requireView().findViewById(R.id.etFullName)
        etEmail = requireView().findViewById(R.id.etEmail)
        etPhone = requireView().findViewById(R.id.etPhone)
        etAge = requireView().findViewById(R.id.etAge)
        tvGender = requireView().findViewById(R.id.tvGender)
        etLocation = requireView().findViewById(R.id.etLocation)
        tvChangePassword = requireView().findViewById(R.id.tvChangePassword)
    }

    private fun setupClickListeners() {
        // Back button
        requireView().findViewById<View>(R.id.btnBack)?.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        // Change photo
        requireView().findViewById<View>(R.id.tvChangePhoto)?.setOnClickListener {
            showImagePickerDialog()
        }
        
        // Profile picture click
        ivProfilePicture.setOnClickListener {
            showImagePickerDialog()
        }
        
        // Gender selector
        requireView().findViewById<View>(R.id.llGender)?.setOnClickListener {
            showGenderDialog()
        }
        
        // Change password
        tvChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }
        
        // Save button
        requireView().findViewById<View>(R.id.btnSave)?.setOnClickListener {
            saveProfile()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Choose from Gallery", "Take Photo", "Cancel")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Profile Picture")
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
            
            if (bitmap != null) {
                ivProfilePicture.scaleType = ImageView.ScaleType.CENTER_CROP
                ivProfilePicture.setImageBitmap(bitmap)
                
                // Save image URI to preferences with current user's email for verification
                val currentEmail = sharedPreferences.getString("email", null)
                sharedPreferences.edit()
                    .putString("profile_image_uri", uri.toString())
                    .putString("profile_image_email", currentEmail)
                    .apply()
            } else {
                ivProfilePicture.setImageResource(R.drawable.ic_person)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            ivProfilePicture.setImageResource(R.drawable.ic_person)
        } catch (e: Exception) {
            e.printStackTrace()
            ivProfilePicture.setImageResource(R.drawable.ic_person)
        }
    }

    private fun loadImageFromBase64(base64String: String) {
        try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            if (bitmap != null) {
                ivProfilePicture.scaleType = ImageView.ScaleType.CENTER_CROP
                ivProfilePicture.setImageBitmap(bitmap)
                
                // Save email for verification
                val currentEmail = sharedPreferences.getString("email", null)
                sharedPreferences.edit()
                    .putString("profile_image_email", currentEmail)
                    .apply()
            } else {
                ivProfilePicture.setImageResource(R.drawable.ic_person)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ivProfilePicture.setImageResource(R.drawable.ic_person)
        }
    }

    private fun saveBitmapToUri(bitmap: Bitmap): Uri? {
        // Save bitmap as base64 string in SharedPreferences for simplicity
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
        sharedPreferences.edit().putString("profile_image_base64", base64String).apply()
        
        // Save to MediaStore using modern API
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "Profile_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SkeetSkeet")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            
            val uri = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                requireContext().contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    requireContext().contentResolver.update(it, contentValues, null, null)
                }
                
                it
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showGenderDialog() {
        val genders = arrayOf("Male", "Female", "Other", "Prefer not to say")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Gender")
            .setItems(genders) { _, which ->
                tvGender.text = genders[which]
            }
            .show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etCurrentPassword = dialogView.findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<TextInputEditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(requireContext())
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change") { _, _ ->
                val currentPassword = etCurrentPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (validatePasswordChange(currentPassword, newPassword, confirmPassword)) {
                    // TODO: Implement actual password change logic with backend
                    sharedPreferences.edit().putString("password", newPassword).apply()
                    Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun validatePasswordChange(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Boolean {
        if (currentPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter current password", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.isEmpty() || newPassword.length < 6) {
            Toast.makeText(requireContext(), "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }

        // TODO: Verify current password with backend
        return true
    }

    private fun loadProfileData() {
        // Load text fields
        etUsername.setText(sharedPreferences.getString("username", ""))
        etFullName.setText(sharedPreferences.getString("full_name", ""))
        etEmail.setText(sharedPreferences.getString("email", ""))
        etPhone.setText(sharedPreferences.getString("phone", ""))
        etAge.setText(sharedPreferences.getString("age", ""))
        tvGender.text = sharedPreferences.getString("gender", "Select Gender")
        etLocation.setText(sharedPreferences.getString("location", ""))

        // Load profile image - only if it belongs to current user
        val currentEmail = sharedPreferences.getString("email", null)
        val storedEmailForImage = sharedPreferences.getString("profile_image_email", null)
        
        // If emails don't match or no email stored, show default icon
        if (currentEmail == null || storedEmailForImage != currentEmail) {
            ivProfilePicture.setImageResource(R.drawable.ic_person)
            return
        }
        
        val imageUriString = sharedPreferences.getString("profile_image_uri", null)
        val imageBase64 = sharedPreferences.getString("profile_image_base64", null)
        
        if (imageUriString != null) {
            try {
                val uri = Uri.parse(imageUriString)
                profileImageUri = uri
                loadImageIntoView(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to base64 if URI fails
                if (imageBase64 != null) {
                    loadImageFromBase64(imageBase64)
                } else {
                    ivProfilePicture.setImageResource(R.drawable.ic_person)
                }
            }
        } else if (imageBase64 != null) {
            loadImageFromBase64(imageBase64)
        } else {
            ivProfilePicture.setImageResource(R.drawable.ic_person)
        }
    }

    private fun saveProfile() {
        val username = etUsername.text.toString().trim()
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val age = etAge.text.toString().trim()
        val gender = tvGender.text.toString()
        val location = etLocation.text.toString().trim()

        // Validation
        if (username.isEmpty()) {
            etUsername.error = "Username is required"
            etUsername.requestFocus()
            return
        }

        if (fullName.isEmpty()) {
            etFullName.error = "Full name is required"
            etFullName.requestFocus()
            return
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Valid email is required"
            etEmail.requestFocus()
            return
        }

        if (phone.isNotEmpty() && phone.length < 10) {
            etPhone.error = "Valid phone number is required"
            etPhone.requestFocus()
            return
        }

        if (age.isNotEmpty()) {
            val ageInt = age.toIntOrNull()
            if (ageInt == null || ageInt < 1 || ageInt > 150) {
                etAge.error = "Valid age is required"
                etAge.requestFocus()
                return
            }
        }

        // Save to SharedPreferences
        sharedPreferences.edit().apply {
            putString("username", username)
            putString("full_name", fullName)
            putString("email", email)
            putString("phone", phone)
            putString("age", age)
            putString("gender", if (gender != "Select Gender") gender else "")
            putString("location", location)
            if (profileImageUri != null) {
                putString("profile_image_uri", profileImageUri.toString())
            }
            apply()
        }

        // TODO: Save to backend/database
        
        // Update parent activity's header if it exists
        try {
            val activity = requireActivity() as? com.example.smd_fyp.player.UserProfileActivity
            activity?.refreshProfileData()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        Toast.makeText(requireContext(), "Profile saved successfully", Toast.LENGTH_SHORT).show()
        
        // Go back to settings
        requireActivity().supportFragmentManager.popBackStack()
    }
}


