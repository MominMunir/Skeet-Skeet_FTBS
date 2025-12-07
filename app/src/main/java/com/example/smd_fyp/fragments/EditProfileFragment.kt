package com.example.smd_fyp.fragments

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
import androidx.lifecycle.lifecycleScope
import com.example.smd_fyp.utils.GlideHelper
import com.example.smd_fyp.R
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.model.User
import com.example.smd_fyp.sync.SyncManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var currentUser: User? = null
    private var uploadedImageUrl: String? = null
    private var isNewImageSelected: Boolean = false

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
                    // Mark that a new image was selected
                    isNewImageSelected = true
                    uploadedImageUrl = null // Clear previous URL so it will be uploaded
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
        
        // Initialize database
        LocalDatabaseHelper.initialize(requireContext())
        
        // Initialize views
        initializeViews()
        
        // Load profile data from PHP/Firebase
        loadProfileDataFromServer()
        
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
            
            ivProfilePicture.scaleType = ImageView.ScaleType.CENTER_CROP
            ivProfilePicture.setImageBitmap(bitmap)
            
            // Mark that a new image was selected
            isNewImageSelected = true
            uploadedImageUrl = null // Clear previous URL so it will be uploaded
            
            // Save image URI to preferences
            sharedPreferences.edit().putString("profile_image_uri", uri.toString()).apply()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show()
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
                    changePassword(currentPassword, newPassword)
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

        return true
    }
    
    private fun changePassword(currentPassword: String, newPassword: String) {
        lifecycleScope.launch {
            try {
                val firebaseUser = FirebaseAuthHelper.getCurrentUser()
                if (firebaseUser == null || firebaseUser.email == null) {
                    Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Re-authenticate user first (required for password change)
                val reauthResult = withContext(Dispatchers.IO) {
                    FirebaseAuthHelper.reauthenticateUser(firebaseUser.email!!, currentPassword)
                }
                
                if (reauthResult.isFailure) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                // Update password
                val updateResult = withContext(Dispatchers.IO) {
                    FirebaseAuthHelper.updatePassword(newPassword)
                }
                
                withContext(Dispatchers.Main) {
                    updateResult.fold(
                        onSuccess = {
                            Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
                        },
                        onFailure = { error ->
                            android.util.Log.e("EditProfile", "Error changing password: ${error.message}", error)
                            Toast.makeText(requireContext(), "Error changing password: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("EditProfile", "Error changing password: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error changing password: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadProfileDataFromServer() {
        lifecycleScope.launch {
            try {
                val firebaseUser = FirebaseAuthHelper.getCurrentUser()
                if (firebaseUser == null) {
                    Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Try to get user from local DB first
                currentUser = withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.getUser(firebaseUser.uid)
                }
                
                // If not in local DB, fetch from PHP API
                if (currentUser == null) {
                    withContext(Dispatchers.IO) {
                        try {
                            val apiService = ApiClient.getPhpApiService(requireContext())
                            val response = apiService.getUser(firebaseUser.uid)
                            if (response.isSuccessful && response.body() != null) {
                                currentUser = response.body()
                                // Save to local DB
                                currentUser?.let { LocalDatabaseHelper.saveUser(it) }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("EditProfile", "Error fetching user: ${e.message}", e)
                        }
                    }
                }
                
                // Load data into UI
                withContext(Dispatchers.Main) {
                    currentUser?.let { user ->
                        etFullName.setText(user.fullName)
                        etEmail.setText(user.email)
                        etPhone.setText(user.phoneNumber ?: "")
                        
                        // Load additional fields from SharedPreferences (not synced to backend)
                        etUsername.setText(sharedPreferences.getString("username", ""))
                        etAge.setText(sharedPreferences.getString("age", ""))
                        tvGender.text = sharedPreferences.getString("gender", "Select Gender")
                        etLocation.setText(sharedPreferences.getString("location", ""))
                        
                        // Load profile image
                        user.profileImageUrl?.let { imageUrl ->
                            // Normalize URL to use correct IP
                            val normalizedUrl = ApiClient.normalizeImageUrl(requireContext(), imageUrl)
                            uploadedImageUrl = normalizedUrl
                            isNewImageSelected = false
                            android.util.Log.d("EditProfile", "Loading profile image from URL: $normalizedUrl (original: $imageUrl)")
                            loadImageWithGlide(normalizedUrl)
                        } ?: run {
                            // Fallback to SharedPreferences
                            uploadedImageUrl = null
                            isNewImageSelected = false
                            loadProfileImageFromPreferences()
                        }
                    } ?: run {
                        // Fallback to SharedPreferences if user not found
                        loadProfileImageFromPreferences()
                        etFullName.setText(sharedPreferences.getString("full_name", ""))
                        etEmail.setText(sharedPreferences.getString("email", ""))
                        etPhone.setText(sharedPreferences.getString("phone", ""))
                        etUsername.setText(sharedPreferences.getString("username", ""))
                        etAge.setText(sharedPreferences.getString("age", ""))
                        tvGender.text = sharedPreferences.getString("gender", "Select Gender")
                        etLocation.setText(sharedPreferences.getString("location", ""))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("EditProfile", "Error loading profile: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error loading profile data", Toast.LENGTH_SHORT).show()
                    // Fallback to SharedPreferences
                    loadProfileImageFromPreferences()
                }
            }
        }
    }
    
    private fun loadProfileImageFromPreferences() {
        val imageUriString = sharedPreferences.getString("profile_image_uri", null)
        val imageBase64 = sharedPreferences.getString("profile_image_base64", null)
        
        if (imageUriString != null) {
            try {
                val uri = Uri.parse(imageUriString)
                profileImageUri = uri
                loadImageIntoView(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                if (imageBase64 != null) {
                    loadImageFromBase64(imageBase64)
                }
            }
        } else if (imageBase64 != null) {
            loadImageFromBase64(imageBase64)
        }
    }

    private fun saveProfile() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val age = etAge.text.toString().trim()
        val gender = tvGender.text.toString()
        val location = etLocation.text.toString().trim()

        // Validation
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

        val firebaseUser = FirebaseAuthHelper.getCurrentUser()
        if (firebaseUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        view?.findViewById<View>(R.id.btnSave)?.isEnabled = false

        lifecycleScope.launch {
            try {
                var imageUrl = uploadedImageUrl
                val emailChanged = firebaseUser.email != email
                
                // Check if email changed - if so, update Firebase Auth (requires re-authentication)
                if (emailChanged) {
                    android.util.Log.d("EditProfile", "Email changed from ${firebaseUser.email} to $email")
                    // Note: Firebase email update requires re-authentication
                    // For now, we'll update it in the database but not in Firebase Auth
                    // User can update Firebase Auth email separately through password change flow
                }
                
                // Upload image if a new one was selected
                if (isNewImageSelected && profileImageUri != null) {
                    android.util.Log.d("EditProfile", "Uploading new profile image...")
                    imageUrl = uploadProfileImage()
                    if (imageUrl != null) {
                        android.util.Log.d("EditProfile", "Image uploaded successfully: $imageUrl")
                        uploadedImageUrl = imageUrl
                        isNewImageSelected = false
                        
                        // Update image display immediately
                        withContext(Dispatchers.Main) {
                            loadImageWithGlide(imageUrl)
                        }
                    } else {
                        android.util.Log.e("EditProfile", "Image upload failed")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Failed to upload image. Profile will be saved without image.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                
                // Update user object
                val updatedUser = (currentUser ?: User(
                    id = firebaseUser.uid,
                    email = email, // Use the new email from form
                    fullName = fullName,
                    createdAt = System.currentTimeMillis()
                )).copy(
                    fullName = fullName,
                    email = email, // Update email in database
                    phoneNumber = phone.takeIf { it.isNotEmpty() },
                    profileImageUrl = imageUrl ?: currentUser?.profileImageUrl // Keep existing URL if upload failed
                )
                
                // Save to local DB first
                withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.saveUser(updatedUser.copy(synced = false))
                }
                
                // Sync to PHP and Firebase
                val syncResult = withContext(Dispatchers.IO) {
                    SyncManager.syncUser(requireContext(), updatedUser)
                }
                
                withContext(Dispatchers.Main) {
                    view?.findViewById<View>(R.id.btnSave)?.isEnabled = true
                    
                    syncResult.fold(
                        onSuccess = { syncedUser ->
                            currentUser = syncedUser
                            uploadedImageUrl = syncedUser.profileImageUrl // Update uploaded URL
                            
                            // Save to SharedPreferences for backward compatibility
                            sharedPreferences.edit().apply {
                                putString("full_name", syncedUser.fullName)
                                putString("email", syncedUser.email)
                                putString("phone", syncedUser.phoneNumber ?: "")
                                syncedUser.profileImageUrl?.let {
                                    putString("profile_image_url", it)
                                    android.util.Log.d("EditProfile", "Saved profile image URL: $it")
                                }
                                // Save additional fields (not synced to backend)
                                putString("username", username)
                                putString("age", age)
                                putString("gender", if (gender != "Select Gender") gender else "")
                                putString("location", location)
                                apply()
                            }
                            
                            // Update image display with the synced URL
                            syncedUser.profileImageUrl?.let { imageUrl ->
                                withContext(Dispatchers.Main) {
                                    val normalizedUrl = ApiClient.normalizeImageUrl(requireContext(), imageUrl)
                                    loadImageWithGlide(normalizedUrl)
                                }
                            }
                            
                            // Update parent activity
                            try {
                                val activity = requireActivity() as? com.example.smd_fyp.UserProfileActivity
                                activity?.refreshProfileData()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            
                            val message = if (emailChanged) {
                                "Profile saved successfully. Note: Email change in Firebase Auth requires re-authentication."
                            } else {
                                "Profile saved successfully"
                            }
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                            requireActivity().supportFragmentManager.popBackStack()
                        },
                        onFailure = { error ->
                            android.util.Log.e("EditProfile", "Error saving profile: ${error.message}", error)
                            Toast.makeText(requireContext(), "Error saving profile: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("EditProfile", "Error saving profile: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    view?.findViewById<View>(R.id.btnSave)?.isEnabled = true
                    Toast.makeText(requireContext(), "Error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private suspend fun uploadProfileImage(): String? = withContext(Dispatchers.IO) {
        try {
            val bitmap = if (profileImageUri != null) {
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(profileImageUri!!)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    bitmap
                } catch (e: Exception) {
                    android.util.Log.e("EditProfile", "Error reading image: ${e.message}", e)
                    null
                }
            } else {
                null
            }
            
            if (bitmap == null) {
                android.util.Log.e("EditProfile", "Bitmap is null, cannot upload")
                return@withContext null
            }
            
            // Convert to base64
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64Image = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            
            android.util.Log.d("EditProfile", "Image size: ${byteArray.size} bytes, Base64 length: ${base64Image.length}")
            
            // Upload to PHP API
            val apiService = ApiClient.getPhpApiService(requireContext())
            val response = apiService.uploadBase64Image(
                base64Image = base64Image,
                folder = "users",
                imageType = "jpg"
            )
            
            android.util.Log.d("EditProfile", "Upload response code: ${response.code()}")
            
            if (response.isSuccessful && response.body() != null) {
                val uploadResponse = response.body()!!
                android.util.Log.d("EditProfile", "Upload response: success=${uploadResponse.success}, url=${uploadResponse.url}")
                
                if (uploadResponse.success && uploadResponse.url != null) {
                    // Normalize the URL to use the correct IP address
                    ApiClient.normalizeImageUrl(requireContext(), uploadResponse.url)
                } else {
                    android.util.Log.e("EditProfile", "Image upload failed: ${uploadResponse.message}")
                    null
                }
            } else {
                val errorBody = try {
                    response.errorBody()?.string() ?: "Unknown error"
                } catch (e: Exception) {
                    "Could not read error body: ${e.message}"
                }
                android.util.Log.e("EditProfile", "Image upload failed: ${response.code()} - $errorBody")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("EditProfile", "Error uploading image: ${e.message}", e)
            null
        }
    }
    
    private fun loadImageWithGlide(imageUrl: String?) {
        GlideHelper.loadImage(
            context = requireContext(),
            imageUrl = imageUrl,
            imageView = ivProfilePicture,
            placeholder = R.drawable.ic_person,
            errorDrawable = R.drawable.ic_person,
            tag = "EditProfile"
        )
    }
}

