package com.example.smd_fyp

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream

class UserProfileActivity : AppCompatActivity() {

    private lateinit var ivProfilePicture: ImageView
    private lateinit var etUsername: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etAge: TextInputEditText
    private lateinit var tvGender: TextView
    private lateinit var etLocation: TextInputEditText
    private lateinit var tvChangePassword: TextView
    private lateinit var btnSave: View

    private lateinit var sharedPreferences: SharedPreferences
    private var profileImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            if (data?.data != null) {
                // Image from gallery
                val selectedImageUri: Uri = data.data!!
                profileImageUri = selectedImageUri
                loadImageIntoView(selectedImageUri)
            } else if (data?.extras?.get("data") != null) {
                // Image from camera
                val bitmap = data.extras?.get("data") as? Bitmap
                if (bitmap != null) {
                    ivProfilePicture.scaleType = ImageView.ScaleType.CENTER_CROP
                    ivProfilePicture.setImageBitmap(bitmap)
                    saveBitmapToUri(bitmap)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profileRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE)

        // Initialize views
        initializeViews()

        // Load saved profile data
        loadProfileData()

        // Setup click listeners
        setupClickListeners()
    }

    private fun initializeViews() {
        ivProfilePicture = findViewById(R.id.ivProfilePicture)
        etUsername = findViewById(R.id.etUsername)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etAge = findViewById(R.id.etAge)
        tvGender = findViewById(R.id.tvGender)
        etLocation = findViewById(R.id.etLocation)
        tvChangePassword = findViewById(R.id.tvChangePassword)
        btnSave = findViewById(R.id.btnSave)
    }

    private fun setupClickListeners() {
        // Back button
        findViewById<View>(R.id.llBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Change photo
        findViewById<View>(R.id.tvChangePhoto)?.setOnClickListener {
            showImagePickerDialog()
        }

        // Profile picture click
        ivProfilePicture.setOnClickListener {
            showImagePickerDialog()
        }

        // Gender selector
        findViewById<View>(R.id.llGender)?.setOnClickListener {
            showGenderDialog()
        }

        // Change password
        tvChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        // Save button
        btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Choose from Gallery", "Take Photo", "Cancel")
        AlertDialog.Builder(this)
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
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            ivProfilePicture.scaleType = ImageView.ScaleType.CENTER_CROP
            ivProfilePicture.setImageBitmap(bitmap)
            
            // Save image URI to preferences
            sharedPreferences.edit().putString("profile_image_uri", uri.toString()).apply()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadImageFromBase64(base64String: String) {
        try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            ivProfilePicture.scaleType = ImageView.ScaleType.CENTER_CROP
            ivProfilePicture.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveBitmapToUri(bitmap: Bitmap): Uri? {
        // Save bitmap as base64 string in SharedPreferences for simplicity
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
        sharedPreferences.edit().putString("profile_image_base64", base64String).apply()
        
        // Try to save to MediaStore for URI
        return try {
            val path = MediaStore.Images.Media.insertImage(
                contentResolver,
                bitmap,
                "Profile_${System.currentTimeMillis()}",
                "Profile Picture"
            )
            if (path != null) Uri.parse(path) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showGenderDialog() {
        val genders = arrayOf("Male", "Female", "Other", "Prefer not to say")
        AlertDialog.Builder(this)
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

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change") { _, _ ->
                val currentPassword = etCurrentPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (validatePasswordChange(currentPassword, newPassword, confirmPassword)) {
                    // TODO: Implement actual password change logic with backend
                    sharedPreferences.edit().putString("password", newPassword).apply()
                    Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Please enter current password", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword.isEmpty() || newPassword.length < 6) {
            Toast.makeText(this, "New password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return false
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
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

        // Load profile image
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
                }
            }
        } else if (imageBase64 != null) {
            loadImageFromBase64(imageBase64)
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
        Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show()
    }
}

