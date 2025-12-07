package com.example.smd_fyp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.model.Booking
import com.example.smd_fyp.model.BookingStatus
import com.example.smd_fyp.model.PaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class ReceiptActivity : AppCompatActivity() {

    private lateinit var tvBookingId: TextView
    private lateinit var tvGroundName: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var tvPaymentMethod: TextView
    private lateinit var tvPaymentStatus: TextView
    private lateinit var tvBookingStatus: TextView
    private lateinit var btnClose: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_receipt)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.receiptRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get booking ID from intent
        val bookingId = intent.getStringExtra("booking_id")
        if (bookingId == null) {
            finish()
            return
        }

        // Load booking from database
        var booking: Booking? = null
        lifecycleScope.launch {
            booking = withContext(Dispatchers.IO) {
                LocalDatabaseHelper.getBooking(bookingId)
            }
            if (booking == null) {
                finish()
                return@launch
            }
            displayReceipt(booking!!)
        }

        initializeViews()

        // Setup back button
        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnClose.setOnClickListener {
            finish()
        }
    }

    private fun initializeViews() {
        tvBookingId = findViewById(R.id.tvBookingId)
        tvGroundName = findViewById(R.id.tvGroundName)
        tvDate = findViewById(R.id.tvDate)
        tvTime = findViewById(R.id.tvTime)
        tvDuration = findViewById(R.id.tvDuration)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod)
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus)
        tvBookingStatus = findViewById(R.id.tvBookingStatus)
        btnClose = findViewById(R.id.btnClose)
    }

    private fun displayReceipt(booking: Booking) {
        tvBookingId.text = "Booking ID: ${booking.id.take(8)}"
        tvGroundName.text = booking.groundName
        tvDate.text = formatDate(booking.date)
        tvTime.text = booking.time
        tvDuration.text = "${booking.duration} ${if (booking.duration == 1) "hour" else "hours"}"
        tvTotalPrice.text = "Rs. ${booking.totalPrice.toInt()}"

        // Payment method
        val paymentMethodText = when (booking.paymentMethod) {
            "on_site" -> "On-Site Payment"
            "easypaisa" -> "EasyPaisa"
            "card" -> "Card Payment"
            else -> "Not specified"
        }
        tvPaymentMethod.text = paymentMethodText

        // Payment status
        tvPaymentStatus.text = booking.paymentStatus.name.lowercase().replaceFirstChar { it.uppercase() }
        val paymentStatusColor = when (booking.paymentStatus) {
            PaymentStatus.PAID -> getColor(R.color.green_600)
            PaymentStatus.PENDING -> getColor(R.color.yellow_600)
            PaymentStatus.FAILED -> getColor(R.color.red_600)
            PaymentStatus.REFUNDED -> getColor(R.color.text_secondary)
        }
        tvPaymentStatus.setTextColor(paymentStatusColor)

        // Booking status
        tvBookingStatus.text = booking.status.name.lowercase().replaceFirstChar { it.uppercase() }
        val bookingStatusColor = when (booking.status) {
            BookingStatus.CONFIRMED -> getColor(R.color.green_600)
            BookingStatus.PENDING -> getColor(R.color.yellow_600)
            BookingStatus.CANCELLED -> getColor(R.color.text_secondary)
            BookingStatus.COMPLETED -> getColor(R.color.green_600)
        }
        tvBookingStatus.setTextColor(bookingStatusColor)
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            date?.let { outputFormat.format(it) } ?: dateStr
        } catch (e: Exception) {
            dateStr
        }
    }
}
