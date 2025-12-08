package com.example.smd_fyp

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
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.smd_fyp.utils.GlideHelper
import com.example.smd_fyp.api.ApiClient
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.fragments.BookingsFragment
import com.example.smd_fyp.fragments.FavoritesFragment
import com.example.smd_fyp.fragments.SettingsFragment
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.model.User
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream

class UserProfileActivity : AppCompatActivity() {

    private lateinit var tvBookingsTab: TextView
    private lateinit var tvFavoritesTab: TextView
    private lateinit var tvSettingsTab: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var ivProfileIcon: ImageView
    
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
            } else {
                // Image from camera
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    data?.extras?.getParcelable("data", Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    data?.extras?.getParcelable("data") as? Bitmap
                }
                if (bitmap != null) {
                    ivProfileIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                    ivProfileIcon.setImageBitmap(bitmap)
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
        
        // Initialize database
        LocalDatabaseHelper.initialize(this)

        // Initialize views
        initializeViews()

        // Load profile data from PHP/Firebase
        loadProfileDataFromServer()

        // Setup tabs
        setupTabs()

        // Load default fragment based on intent or default to Bookings
        if (savedInstanceState == null) {
            val selectedTab = intent.getStringExtra("selected_tab")
            when (selectedTab) {
                "favorites" -> {
                    loadFragment(FavoritesFragment())
                    setActiveTab(tvFavoritesTab)
                }
                "settings" -> {
                    loadFragment(SettingsFragment())
                    setActiveTab(tvSettingsTab)
                }
                else -> {
                    loadFragment(BookingsFragment())
                    setActiveTab(tvBookingsTab)
                }
            }
        }
    }

    private fun initializeViews() {
        tvBookingsTab = findViewById(R.id.tvBookingsTab)
        tvFavoritesTab = findViewById(R.id.tvFavoritesTab)
        tvSettingsTab = findViewById(R.id.tvSettingsTab)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvUserRole = findViewById(R.id.tvUserRole)
        ivProfileIcon = findViewById(R.id.ivProfileIcon)
        
        // Back button
        findViewById<View>(R.id.llBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        // Profile icon click
        ivProfileIcon.setOnClickListener {
            showImagePickerDialog()
        }
    }

    private fun setupTabs() {
        tvBookingsTab.setOnClickListener { switchToFragment(BookingsFragment(), tvBookingsTab) }
        tvFavoritesTab.setOnClickListener { switchToFragment(FavoritesFragment(), tvFavoritesTab) }
        tvSettingsTab.setOnClickListener { switchToFragment(SettingsFragment(), tvSettingsTab) }

        // Set initial active tab
        setActiveTab(tvBookingsTab)
    }

    private fun switchToFragment(fragment: Fragment, activeTab: TextView) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()

        // Update tab states
        setActiveTab(activeTab)
    }

    private fun setActiveTab(activeTab: TextView) {
        // Reset all tabs
        tvBookingsTab.setBackgroundResource(R.drawable.bg_tab_inactive_dashboard)
        tvBookingsTab.setTextColor(getColor(R.color.text_primary))
        tvBookingsTab.setTypeface(null, android.graphics.Typeface.NORMAL)

        tvFavoritesTab.setBackgroundResource(R.drawable.bg_tab_inactive_dashboard)
        tvFavoritesTab.setTextColor(getColor(R.color.text_primary))
        tvFavoritesTab.setTypeface(null, android.graphics.Typeface.NORMAL)

        tvSettingsTab.setBackgroundResource(R.drawable.bg_tab_inactive_dashboard)
        tvSettingsTab.setTextColor(getColor(R.color.text_primary))
        tvSettingsTab.setTypeface(null, android.graphics.Typeface.NORMAL)

        // Set active tab
        activeTab.setBackgroundResource(R.drawable.bg_tab_active_dashboard)
        activeTab.setTextColor(getColor(R.color.green_600))
        activeTab.setTypeface(null, android.graphics.Typeface.BOLD)
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun loadProfileDataFromServer() {
        lifecycleScope.launch {
            try {
                val firebaseUser = FirebaseAuthHelper.getCurrentUser()
                if (firebaseUser == null) {
                    // Fallback to SharedPreferences
                    loadProfileDataFromPreferences()
                    return@launch
                }
                
                // Try to get user from local DB first
                var user: User? = withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.getUser(firebaseUser.uid)
                }
                
                // If not in local DB, fetch from PHP API
                if (user == null) {
                    withContext(Dispatchers.IO) {
                        try {
                            val apiService = ApiClient.getPhpApiService(this@UserProfileActivity)
                            val response = apiService.getUser(firebaseUser.uid)
                            if (response.isSuccessful && response.body() != null) {
                                user = response.body()
                                // Save to local DB
                                user?.let { LocalDatabaseHelper.saveUser(it) }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("UserProfile", "Error fetching user: ${e.message}", e)
                        }
                    }
                }
                
                // Update UI
                withContext(Dispatchers.Main) {
                    user?.let {
                        tvUserName.text = it.fullName
                        tvUserEmail.text = it.email
                        tvUserRole.text = it.role.name
                        
                        // Load profile image
                        it.profileImageUrl?.let { imageUrl ->
                            if (imageUrl.isNotBlank()) {
                                // Normalize URL to use correct IP
                                val normalizedUrl = ApiClient.normalizeImageUrl(this@UserProfileActivity, imageUrl)
                                android.util.Log.d("UserProfile", "Loading profile image from URL: $normalizedUrl (original: $imageUrl)")
                                loadImageWithGlide(normalizedUrl)
                            } else {
                                // Empty or blank URL, show default icon
                                android.util.Log.d("UserProfile", "Profile image URL is empty, showing default icon")
                                ivProfileIcon.setImageResource(R.drawable.ic_person)
                            }
                        } ?: run {
                            // No profile image URL, show default icon
                            android.util.Log.d("UserProfile", "No profile image URL, showing default icon")
                            ivProfileIcon.setImageResource(R.drawable.ic_person)
                        }
                    } ?: run {
                        // Fallback to SharedPreferences
                        loadProfileDataFromPreferences()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("UserProfile", "Error loading profile: ${e.message}", e)
                loadProfileDataFromPreferences()
            }
        }
    }
    
    private fun loadProfileDataFromPreferences() {
        val fullName = sharedPreferences.getString("full_name", "User") ?: "User"
        val email = sharedPreferences.getString("email", "email@example.com") ?: "email@example.com"
        val role = sharedPreferences.getString("role", "Player") ?: "Player"
        
        tvUserName.text = fullName
        tvUserEmail.text = email
        tvUserRole.text = role
        
        loadProfileImageFromPreferences()
    }
    
    private fun loadProfileImageFromPreferences() {
        // Only load from SharedPreferences if we can verify it belongs to the current user
        // Check if the stored email matches the current user's email
        val currentEmail = sharedPreferences.getString("email", null)
        val storedEmailForImage = sharedPreferences.getString("profile_image_email", null)
        
        // If emails don't match or no email stored, show default icon
        if (currentEmail == null || storedEmailForImage != currentEmail) {
            android.util.Log.d("UserProfile", "Profile image in preferences doesn't belong to current user, showing default icon")
            ivProfileIcon.setImageResource(R.drawable.ic_person)
            return
        }
        
        val imageUriString = sharedPreferences.getString("profile_image_uri", null)
        val imageBase64 = sharedPreferences.getString("profile_image_base64", null)
        val imageUrl = sharedPreferences.getString("profile_image_url", null)
        
        if (imageUrl != null && imageUrl.isNotBlank()) {
            // Normalize URL to use correct IP
            val normalizedUrl = ApiClient.normalizeImageUrl(this, imageUrl)
            android.util.Log.d("UserProfile", "Loading image from SharedPreferences URL: $normalizedUrl (original: $imageUrl)")
            loadImageWithGlide(normalizedUrl)
        } else if (imageUriString != null) {
            try {
                val uri = Uri.parse(imageUriString)
                profileImageUri = uri
                loadImageIntoView(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                if (imageBase64 != null) {
                    loadImageFromBase64(imageBase64)
                } else {
                    ivProfileIcon.setImageResource(R.drawable.ic_person)
                }
            }
        } else if (imageBase64 != null) {
            loadImageFromBase64(imageBase64)
        } else {
            // No image found, show default icon
            ivProfileIcon.setImageResource(R.drawable.ic_person)
        }
    }
    
    fun refreshProfileData() {
        loadProfileDataFromServer()
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
            
            if (bitmap != null) {
                ivProfileIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                ivProfileIcon.setImageBitmap(bitmap)
                
                // Save image URI to preferences with current user's email for verification
                val currentEmail = sharedPreferences.getString("email", null)
                sharedPreferences.edit()
                    .putString("profile_image_uri", uri.toString())
                    .putString("profile_image_email", currentEmail)
                    .apply()
            } else {
                ivProfileIcon.setImageResource(R.drawable.ic_person)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            ivProfileIcon.setImageResource(R.drawable.ic_person)
        } catch (e: Exception) {
            e.printStackTrace()
            ivProfileIcon.setImageResource(R.drawable.ic_person)
        }
    }

    private fun loadImageFromBase64(base64String: String) {
        try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            if (bitmap != null) {
                ivProfileIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                ivProfileIcon.setImageBitmap(bitmap)
                
                // Save email for verification
                val currentEmail = sharedPreferences.getString("email", null)
                sharedPreferences.edit()
                    .putString("profile_image_email", currentEmail)
                    .apply()
            } else {
                ivProfileIcon.setImageResource(R.drawable.ic_person)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ivProfileIcon.setImageResource(R.drawable.ic_person)
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
            
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(it, contentValues, null, null)
                }
                
                it
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun loadImageWithGlide(imageUrl: String?) {
        GlideHelper.loadImage(
            context = this,
            imageUrl = imageUrl,
            imageView = ivProfileIcon,
            placeholder = R.drawable.ic_person,
            errorDrawable = R.drawable.ic_person,
            tag = "UserProfile"
        )
    }
}
