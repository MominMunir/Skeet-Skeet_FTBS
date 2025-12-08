package com.example.smd_fyp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.smd_fyp.api.OpenMeteoService
import com.example.smd_fyp.database.LocalDatabaseHelper
import com.example.smd_fyp.firebase.FirebaseAuthHelper
import com.example.smd_fyp.model.Booking
import com.example.smd_fyp.model.BookingStatus
import com.example.smd_fyp.model.GroundApi
import com.example.smd_fyp.model.Notification
import com.example.smd_fyp.model.NotificationType
import com.example.smd_fyp.model.PaymentStatus
import com.example.smd_fyp.sync.SyncManager
import com.example.smd_fyp.utils.BookingConflictChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class BookingActivity : AppCompatActivity() {

    private var ground: GroundApi? = null
    private var selectedDate: Calendar = Calendar.getInstance()
    private var selectedTime: Calendar = Calendar.getInstance()
    private var selectedDuration: Int = 1 // hours

    private lateinit var tvGroundName: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvDuration: TextView
    private lateinit var tvTotalPrice: TextView
    private lateinit var tvWeatherSummary: TextView
    private lateinit var btnConfirmBooking: Button
    private lateinit var rgPaymentMethod: RadioGroup

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private val openMeteo by lazy { OpenMeteoService.create() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_booking)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bookingRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize database
        LocalDatabaseHelper.initialize(this)

        // Get ground data from intent
        val groundId = intent.getStringExtra("ground_id")
        val groundName = intent.getStringExtra("ground_name") ?: ""
        val groundPrice = intent.getDoubleExtra("ground_price", 0.0)

        if (groundId == null) {
            Toast.makeText(this, "Ground not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        initializeViews()

        // Load ground data
        loadGroundData(groundId, groundName, groundPrice)

        // Setup date picker
        findViewById<View>(R.id.llDatePicker)?.setOnClickListener {
            showDatePicker()
        }

        // Setup time picker
        findViewById<View>(R.id.llTimePicker)?.setOnClickListener {
            showTimePicker()
        }

        // Setup duration selector
        findViewById<View>(R.id.btnDurationMinus)?.setOnClickListener {
            if (selectedDuration > 1) {
                selectedDuration--
                updateDuration()
                updateTotalPrice()
            }
        }

        findViewById<View>(R.id.btnDurationPlus)?.setOnClickListener {
            if (selectedDuration < 12) { // Max 12 hours
                selectedDuration++
                updateDuration()
                updateTotalPrice()
            }
        }

        // Setup confirm booking button
        btnConfirmBooking.setOnClickListener {
                createBooking()
        }

        // Setup back button
        findViewById<View>(R.id.btnBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Initialize with current date/time
        updateDateDisplay()
        updateTimeDisplay()
        updateDuration()
        updateTotalPrice()
    }

    private fun initializeViews() {
        tvGroundName = findViewById(R.id.tvGroundName)
        tvDate = findViewById(R.id.tvDate)
        tvTime = findViewById(R.id.tvTime)
        tvDuration = findViewById(R.id.tvDuration)
        tvTotalPrice = findViewById(R.id.tvTotalPrice)
        tvWeatherSummary = findViewById(R.id.tvWeatherSummary)
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking)
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod)
    }

    private fun loadGroundData(groundId: String, groundName: String, groundPrice: Double) {
        lifecycleScope.launch {
            try {
                val loadedGround = withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.getGround(groundId)
                }

                if (loadedGround != null) {
                    ground = loadedGround
                    tvGroundName.text = loadedGround.name
                    loadWeatherForSelectedDate()
                } else {
                    // Create temporary ground object from intent data
                    ground = GroundApi(
                        id = groundId,
                        name = groundName,
                        price = groundPrice
                    )
                    tvGroundName.text = groundName
                    loadWeatherForSelectedDate()
                }
                updateTotalPrice()
            } catch (e: Exception) {
                e.printStackTrace()
                // Use intent data as fallback
                ground = GroundApi(
                    id = groundId,
                    name = groundName,
                    price = groundPrice
                )
                tvGroundName.text = groundName
                loadWeatherForSelectedDate()
            }
        }
    }

    private fun showDatePicker() {
        val year = selectedDate.get(Calendar.YEAR)
        val month = selectedDate.get(Calendar.MONTH)
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                updateDateDisplay()
                loadWeatherForSelectedDate()
            },
            year,
            month,
            day
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000 // Allow today
            show()
        }
    }

    private fun showTimePicker() {
        val hour = selectedTime.get(Calendar.HOUR_OF_DAY)
        val minute = selectedTime.get(Calendar.MINUTE)

        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                selectedTime.set(Calendar.HOUR_OF_DAY, selectedHour)
                selectedTime.set(Calendar.MINUTE, selectedMinute)
                updateTimeDisplay()
            },
            hour,
            minute,
            false // 24-hour format
        ).show()
    }

    private fun updateDateDisplay() {
        tvDate.text = dateFormat.format(selectedDate.time)
        loadWeatherForSelectedDate()
    }

    private fun updateTimeDisplay() {
        tvTime.text = timeFormat.format(selectedTime.time)
    }

    private fun updateDuration() {
        tvDuration.text = "$selectedDuration ${if (selectedDuration == 1) "hour" else "hours"}"
    }

    private fun updateTotalPrice() {
        ground?.let {
            val totalPrice = it.price * selectedDuration
            tvTotalPrice.text = "Rs. ${totalPrice.toInt()}"
        }
    }

    private fun loadWeatherForSelectedDate() {
        val currentGround = ground ?: return
        val city = currentGround.location ?: currentGround.name ?: ""
        val coords = cityToCoords(city)
        if (coords == null) {
            tvWeatherSummary.text = "Weather not available for this location."
            return
        }

        val targetDate = Calendar.getInstance().apply {
            set(selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        }
        val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(targetDate.time)

        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                openMeteo.getDailyForecast(coords.first, coords.second)
            }.onSuccess { resp ->
                val daily = resp.daily
                val index = daily?.time?.indexOf(isoDate) ?: -1
                val ui = if (index >= 0) {
                    val prob = daily?.precipitation_probability_max?.getOrNull(index) ?: 0
                    val sum = daily?.precipitation_sum?.getOrNull(index) ?: 0.0
                    val tMin = daily?.temperature_2m_min?.getOrNull(index) ?: 0.0
                    val tMax = daily?.temperature_2m_max?.getOrNull(index) ?: 0.0
                    val code = daily?.weathercode?.getOrNull(index) ?: 0
                    val isRain = isRainCode(code) || prob >= 30
                    "Weather on ${dateFormat.format(targetDate.time)}: Min ${tMin.toInt()}°, Max ${tMax.toInt()}°, Rain ${prob}% (${String.format("%.1f", sum)} mm) ${if (isRain) "• Rain likely" else ""}"
                } else {
                    "Weather data not available for selected date."
                }
                withContext(Dispatchers.Main) {
                    tvWeatherSummary.text = ui
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    tvWeatherSummary.text = "Unable to load weather."
                }
            }
        }
    }

    private fun cityToCoords(city: String): Pair<Double, Double>? {
        val normalized = city.lowercase(Locale.getDefault())
        return when {
            normalized.contains("islamabad") -> 33.7215 to 73.0433
            normalized.contains("lahore") -> 31.558 to 74.3507
            normalized.contains("karachi") -> 24.8608 to 67.0104
            else -> null
        }
    }

    private fun isRainCode(code: Int): Boolean {
        return (code in 51..67) || (code in 80..82)
    }

    private suspend fun validateBooking(): Boolean {
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        if (currentUser == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@BookingActivity, "Please login to book", Toast.LENGTH_SHORT).show()
            }
            return false
        }

        // Check if date is in the past
        val bookingDateTime = Calendar.getInstance().apply {
            set(selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, selectedTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, selectedTime.get(Calendar.MINUTE))
        }

        if (bookingDateTime.before(Calendar.getInstance())) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@BookingActivity, "Please select a future date and time", Toast.LENGTH_SHORT).show()
            }
            return false
        }

        // Check for booking conflicts
        val currentGround = ground
        if (currentGround != null) {
            val allBookings = withContext(Dispatchers.IO) {
                LocalDatabaseHelper.getAllBookingsSync()
            }
            val groundBookings = allBookings.filter { it.groundId == currentGround.id }
            
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(selectedTime.time)
            
            val newBooking = Booking(
                id = "",
                userId = currentUser.uid,
                groundId = currentGround.id,
                groundName = currentGround.name,
                date = dateStr,
                time = timeStr,
                duration = selectedDuration,
                totalPrice = 0.0,
                status = BookingStatus.PENDING
            )
            
            if (BookingConflictChecker.hasConflict(newBooking, groundBookings)) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@BookingActivity,
                        "This time slot is already booked. Please select another time.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return false
            }
        }

        return true
    }

    private fun createBooking() {
        val currentUser = FirebaseAuthHelper.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please login to book", Toast.LENGTH_SHORT).show()
            return
        }

        val currentGround = ground
        if (currentGround == null) {
            Toast.makeText(this, "Ground information not available", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // Validate booking (including conflict check)
                if (!validateBooking()) {
                    btnConfirmBooking.isEnabled = true
                    btnConfirmBooking.text = "Confirm Booking"
                    return@launch
                }
                
                btnConfirmBooking.isEnabled = false
                btnConfirmBooking.text = "Processing..."

                // Format date and time
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(selectedTime.time)

                // Calculate total price
                val totalPrice = currentGround.price * selectedDuration

                // Get selected payment method
                val selectedPaymentMethod = when (rgPaymentMethod.checkedRadioButtonId) {
                    R.id.rbOnSite -> "on_site"
                    R.id.rbEasyPaisa -> "easypaisa"
                    R.id.rbCard -> "card"
                    else -> "on_site"
                }

                // Create booking object
                val booking = Booking(
                    id = UUID.randomUUID().toString(),
                    userId = currentUser.uid,
                    groundId = currentGround.id,
                    groundName = currentGround.name,
                    date = dateStr,
                    time = timeStr,
                    duration = selectedDuration,
                    totalPrice = totalPrice,
                    status = BookingStatus.PENDING,
                    paymentStatus = PaymentStatus.PENDING,
                    paymentMethod = selectedPaymentMethod,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    synced = false
                )

                // Save to local database first (offline support)
                withContext(Dispatchers.IO) {
                    LocalDatabaseHelper.saveBooking(booking)
                    
                    // Create local notification
                    val notification = Notification(
                        id = UUID.randomUUID().toString(),
                        userId = currentUser.uid,
                        title = "Booking Created",
                        message = "Your booking at ${currentGround.name} has been created for $dateStr at $timeStr",
                        type = NotificationType.BOOKING,
                        relatedId = booking.id,
                        isRead = false,
                        createdAt = System.currentTimeMillis(),
                        synced = false
                    )
                    LocalDatabaseHelper.saveNotification(notification)
                    
                    // Sync notification if online
                    if (SyncManager.isOnline(this@BookingActivity)) {
                        SyncManager.syncNotification(this@BookingActivity, notification)
                    }
                }

                // Sync to API if online
                if (SyncManager.isOnline(this@BookingActivity)) {
                    val syncResult = withContext(Dispatchers.IO) {
                        SyncManager.syncBooking(this@BookingActivity, booking)
                    }

                    withContext(Dispatchers.Main) {
                        if (syncResult.isSuccess) {
                            Toast.makeText(
                                this@BookingActivity,
                                "Booking confirmed!",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        } else {
                            Toast.makeText(
                                this@BookingActivity,
                                "Booking saved locally. Will sync when online.",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@BookingActivity,
                            "Booking saved locally. Will sync when online.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                btnConfirmBooking.isEnabled = true
                btnConfirmBooking.text = "Confirm Booking"
                Toast.makeText(
                    this@BookingActivity,
                    "Error creating booking: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
