package com.example.smd_fyp.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Weather API Service
 * Using OpenWeatherMap API (free tier available)
 * Sign up at: https://openweathermap.org/api
 */
interface WeatherApiService {
    
    companion object {
        const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
        // Get your API key from: https://openweathermap.org/api
        // Add to your app's build.gradle or use BuildConfig
        const val API_KEY = "YOUR_API_KEY_HERE" // Replace with your API key
    }
    
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String = API_KEY,
        @Query("units") units: String = "metric" // metric, imperial, kelvin
    ): Response<WeatherResponse>
    
    @GET("weather")
    suspend fun getWeatherByCoordinates(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String = API_KEY,
        @Query("units") units: String = "metric"
    ): Response<WeatherResponse>
    
    @GET("forecast")
    suspend fun getWeatherForecast(
        @Query("q") city: String,
        @Query("appid") apiKey: String = API_KEY,
        @Query("units") units: String = "metric",
        @Query("cnt") count: Int = 5 // Number of forecast entries
    ): Response<WeatherForecastResponse>
}

data class WeatherResponse(
    val coord: Coordinates?,
    val weather: List<Weather>?,
    val base: String?,
    val main: MainWeather?,
    val visibility: Int?,
    val wind: Wind?,
    val clouds: Clouds?,
    val dt: Long?,
    val sys: Sys?,
    val timezone: Int?,
    val id: Int?,
    val name: String?,
    val cod: Int?
)

data class Coordinates(
    val lon: Double,
    val lat: Double
)

data class Weather(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class MainWeather(
    val temp: Double,
    val feels_like: Double,
    val temp_min: Double,
    val temp_max: Double,
    val pressure: Int,
    val humidity: Int
)

data class Wind(
    val speed: Double,
    val deg: Int?
)

data class Clouds(
    val all: Int
)

data class Sys(
    val type: Int?,
    val id: Int?,
    val country: String?,
    val sunrise: Long?,
    val sunset: Long?
)

data class WeatherForecastResponse(
    val cod: String?,
    val message: Int?,
    val cnt: Int?,
    val list: List<ForecastItem>?,
    val city: City?
)

data class ForecastItem(
    val dt: Long,
    val main: MainWeather,
    val weather: List<Weather>,
    val clouds: Clouds,
    val wind: Wind,
    val visibility: Int?,
    val pop: Double?,
    val sys: Sys?,
    val dt_txt: String
)

data class City(
    val id: Int,
    val name: String,
    val coord: Coordinates,
    val country: String,
    val population: Int?,
    val timezone: Int?,
    val sunrise: Long?,
    val sunset: Long?
)
