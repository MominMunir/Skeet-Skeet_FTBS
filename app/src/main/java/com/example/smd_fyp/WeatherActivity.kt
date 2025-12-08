package com.example.smd_fyp

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.api.OpenMeteoService
import com.example.smd_fyp.home.WeatherAdapter
import com.example.smd_fyp.home.WeatherDayUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WeatherActivity : AppCompatActivity() {

    private val weatherService by lazy { OpenMeteoService.create() }
    private val adapter = WeatherAdapter()

    private val cities = listOf(
        CityCoord("Islamabad", 33.7215, 73.0433),
        CityCoord("Lahore", 31.558, 74.3507),
        CityCoord("Karachi", 24.8608, 67.0104)
    )
    private val allItems = mutableListOf<WeatherDayUi>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val rv = findViewById<RecyclerView>(R.id.rvWeather)
        val progress = findViewById<ProgressBar>(R.id.progressBar)
        val tvError = findViewById<TextView>(R.id.tvError)
        val spCity = findViewById<Spinner>(R.id.spCity)

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        btnBack.setOnClickListener { finish() }

        // City selector
        val cityNames = listOf("All Cities") + cities.map { it.name }
        spCity.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cityNames)
        spCity.setSelection(0)
        spCity.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterByCity(if (position == 0) null else cityNames[position])
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                filterByCity(null)
            }
        })

        loadForecasts(progress, tvError)
    }

    private fun loadForecasts(progress: ProgressBar, tvError: TextView) {
        lifecycleScope.launch {
            progress.visibility = View.VISIBLE
            tvError.visibility = View.GONE

            val results = withContext(Dispatchers.IO) {
                cities.map { city ->
                    async {
                        runCatching {
                            weatherService.getDailyForecast(city.lat, city.lon)
                        }.map { response ->
                            mapToUi(city.name, response)
                        }
                    }
                }.awaitAll()
            }

            progress.visibility = View.GONE

            val uiItems = results.flatMap { it.getOrElse { emptyList() } }
            val errors = results.filter { it.isFailure }

            allItems.clear()
            allItems.addAll(uiItems)

            if (allItems.isNotEmpty()) {
                adapter.update(allItems)
            } else {
                tvError.visibility = View.VISIBLE
                tvError.text = "Unable to load weather right now."
            }

            if (errors.isNotEmpty()) {
                tvError.visibility = View.VISIBLE
                tvError.text = "Some locations failed to load."
            }
        }
    }

    private fun mapToUi(cityName: String, response: com.example.smd_fyp.api.OpenMeteoForecastResponse): List<WeatherDayUi> {
        val daily = response.daily ?: return emptyList()
        val formatter = DateTimeFormatter.ofPattern("EEE, MMM d")

        val times = daily.time ?: return emptyList()
        val codes = daily.weathercode ?: emptyList()
        val tempMax = daily.temperature_2m_max ?: emptyList()
        val tempMin = daily.temperature_2m_min ?: emptyList()
        val precipProb = daily.precipitation_probability_max ?: emptyList()
        val precipSum = daily.precipitation_sum ?: emptyList()

        val size = listOf(times.size, codes.size, tempMax.size, tempMin.size, precipProb.size, precipSum.size).minOrNull() ?: 0
        val days = size.coerceAtMost(5) // show 5 days

        val items = mutableListOf<WeatherDayUi>()
        for (i in 0 until days) {
            val dateLabel = runCatching { LocalDate.parse(times[i]).format(formatter) }.getOrDefault(times[i])
            val rainProb = precipProb.getOrNull(i) ?: 0
            val code = codes.getOrNull(i) ?: 0
            val isRainy = isRainCode(code) || rainProb >= 30

            items.add(
                WeatherDayUi(
                    city = cityName,
                    dateLabel = dateLabel,
                    tempMin = tempMin.getOrNull(i) ?: 0.0,
                    tempMax = tempMax.getOrNull(i) ?: 0.0,
                    precipitationProbability = rainProb,
                    precipitationSum = precipSum.getOrNull(i) ?: 0.0,
                    weatherCode = code,
                    isRainy = isRainy
                )
            )
        }
        return items
    }

    private fun isRainCode(code: Int): Boolean {
        return (code in 51..67) || (code in 80..82)
    }

    data class CityCoord(val name: String, val lat: Double, val lon: Double)

    private fun filterByCity(cityName: String?) {
        if (cityName.isNullOrBlank()) {
            adapter.update(allItems)
        } else {
            adapter.update(allItems.filter { it.city == cityName })
        }
    }
}
