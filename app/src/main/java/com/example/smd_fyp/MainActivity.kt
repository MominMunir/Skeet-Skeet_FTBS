package com.example.smd_fyp

import android.content.Intent
import android.os.Bundle
import android.graphics.Matrix
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smd_fyp.auth.AuthActivity

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
            // Optional: finish splash so Back won’t return here
            finish()
        }
    }
}