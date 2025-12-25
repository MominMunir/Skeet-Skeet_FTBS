package com.example.smd_fyp.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Open-Meteo daily forecast client (no API key required).
 * Fetches up to 7 days; we consume the first 5 for the UI.
 */
interface OpenMeteoService {
    @GET("forecast")
    suspend fun getDailyForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("daily") daily: String = "weathercode,precipitation_probability_max,precipitation_sum,temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoForecastResponse

    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/v1/"

        fun create(): OpenMeteoService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenMeteoService::class.java)
        }
    }
}

data class OpenMeteoForecastResponse(
    val latitude: Double?,
    val longitude: Double?,
    val generationtime_ms: Double?,
    val utc_offset_seconds: Int?,
    val timezone: String?,
    val timezone_abbreviation: String?,
    val elevation: Double?,
    val daily_units: DailyUnits?,
    val daily: DailyData?
)

data class DailyUnits(
    val time: String?,
    val weathercode: String?,
    val temperature_2m_max: String?,
    val temperature_2m_min: String?,
    val precipitation_probability_max: String?,
    val precipitation_sum: String?
)

data class DailyData(
    val time: List<String>?,
    val weathercode: List<Int>?,
    val temperature_2m_max: List<Double>?,
    val temperature_2m_min: List<Double>?,
    val precipitation_probability_max: List<Int>?,
    val precipitation_sum: List<Double>?
)
