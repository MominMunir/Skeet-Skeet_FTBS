package com.example.smd_fyp.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smd_fyp.R

data class WeatherDayUi(
    val city: String,
    val dateLabel: String,
    val tempMin: Double,
    val tempMax: Double,
    val precipitationProbability: Int,
    val precipitationSum: Double,
    val weatherCode: Int,
    val isRainy: Boolean
)

class WeatherAdapter(
    private var items: List<WeatherDayUi> = emptyList()
) : RecyclerView.Adapter<WeatherAdapter.VH>() {

    fun update(newItems: List<WeatherDayUi>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_weather_day, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: CardView = itemView.findViewById(R.id.container)
        private val tvCity: TextView = itemView.findViewById(R.id.tvCity)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvTemps: TextView = itemView.findViewById(R.id.tvTemps)
        private val tvRainProb: TextView = itemView.findViewById(R.id.tvRainProb)
        private val tvSummary: TextView = itemView.findViewById(R.id.tvSummary)

        fun bind(item: WeatherDayUi) {
            tvCity.text = item.city
            tvDate.text = item.dateLabel
            tvTemps.text = "Min ${item.tempMin.toInt()}° • Max ${item.tempMax.toInt()}°"
            tvRainProb.text = "Rain: ${item.precipitationProbability}% (${String.format("%.1f", item.precipitationSum)} mm)"
            tvSummary.text = rainSummary(item)

            val ctx = itemView.context
            val bgColor = if (item.isRainy) {
                ContextCompat.getColor(ctx, R.color.yellow_50)
            } else {
                Color.WHITE
            }
            container.setCardBackgroundColor(bgColor)
        }

        private fun rainSummary(item: WeatherDayUi): String {
            return if (item.isRainy) "Likely rain" else "Low rain risk"
        }
    }
}
