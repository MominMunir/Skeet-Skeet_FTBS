package com.example.smd_fyp
// Abdullah Azeem (22I-1186) Talha Khurram (22I-0709) Momin Munir (22I-0854)
// SMD-Project
import android.content.Intent
import android.os.Bundle
import android.graphics.Matrix
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.smd_fyp.auth.AuthActivity
import com.example.smd_fyp.auth.LoginStateManager
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Check if user should stay logged in
        checkAutoLogin()

        // Zoom only the logo image content (keep circular background size unchanged)
        val ivLogo = findViewById<ImageView>(R.id.ivLogo)
        ivLogo?.post {
            val d = ivLogo.drawable ?: return@post

            val vw = ivLogo.width - ivLogo.paddingLeft - ivLogo.paddingRight
            val vh = ivLogo.height - ivLogo.paddingTop - ivLogo.paddingBottom
            val dw = d.intrinsicWidth
            val dh = d.intrinsicHeight

            if (vw > 0 && vh > 0 && dw > 0 && dh > 0) {
                // Base scale that mimics centerCrop, then apply extra zoom
                val base = maxOf(vw.toFloat() / dw, vh.toFloat() / dh)
                val zoom = 1f // adjust 1.1 - 1.5 to taste
                val scale = base * zoom

                val dx = (vw - dw * scale) * 0.5f + ivLogo.paddingLeft
                val dy = (vh - dh * scale) * 0.5f + ivLogo.paddingTop

                val m = Matrix()
                m.setScale(scale, scale)
                m.postTranslate(dx, dy)

                ivLogo.scaleType = ImageView.ScaleType.MATRIX
                ivLogo.imageMatrix = m
            }
        }

        // Navigate to AuthActivity when user taps Get Started
        findViewById<android.view.View>(R.id.btnGetStarted)?.setOnClickListener {
            startActivity(Intent(this, AuthActivity::class.java))
            // Optional: finish splash so Back won't return here
            finish()
        }

        // Setup back button handling - exit app on splash screen
        setupBackPressHandler()
    }
    
    private fun checkAutoLogin() {
        if (LoginStateManager.shouldStayLoggedIn(this)) {
            val savedUserId = LoginStateManager.getSavedUserId(this)
            val savedUserRole = LoginStateManager.getSavedUserRole(this)
            
            if (!savedUserId.isNullOrEmpty() && !savedUserRole.isNullOrEmpty()) {
                // Check if Firebase user is still logged in
                val currentUser = FirebaseAuthHelper.getCurrentUser()
                if (currentUser != null && currentUser.uid == savedUserId) {
                    // User is still logged in, navigate to appropriate dashboard
                    lifecycleScope.launch {
                        try {
                            // Initialize database
                            LocalDatabaseHelper.initialize(this@MainActivity)
                            
                            // Get user from database
                            val user = withContext(Dispatchers.IO) {
                                LocalDatabaseHelper.getUser(savedUserId)
                            }
                            
                            // Navigate based on role
                            withContext(Dispatchers.Main) {
                                val intent = when (savedUserRole) {
                                    UserRole.ADMIN.name -> Intent(this@MainActivity, com.example.smd_fyp.AdminDashboardActivity::class.java)
                                    UserRole.GROUNDKEEPER.name -> Intent(this@MainActivity, com.example.smd_fyp.GroundkeeperDashboardActivity::class.java)
                                    else -> Intent(this@MainActivity, com.example.smd_fyp.HomeActivity::class.java)
                                }
                                startActivity(intent)
                                finish()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // If error, go to auth screen
                            startActivity(Intent(this@MainActivity, AuthActivity::class.java))
                            finish()
                        }
                    }
                    return
                } else {
                    // Firebase session expired, clear saved state
                    LoginStateManager.clearLoginState(this)
                }
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Exit app when back is pressed on splash screen
                finish()
            }
        })
    }
}
