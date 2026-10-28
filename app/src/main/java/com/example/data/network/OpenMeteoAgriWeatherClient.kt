package com.example.data.network

import com.example.util.GeoCoordinateMapper
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AgriWeatherDayForecast(
  val date: String,
  val maxTempC: Double,
  val minTempC: Double,
  val precipitationMm: Double,
  val et0Mm: Double,
)

data class AgriWeatherReport(
  val coordinatesLabel: String,
  val currentTempC: Double,
  val relativeHumidityPercent: Int,
  val windSpeedKmh: Double,
  val soilMoisturePercent: Double,
  val dailyEt0Mm: Double,
  val irrigationAdviceAr: String,
  val forecastDays: List<AgriWeatherDayForecast>,
)

object OpenMeteoAgriWeatherClient {

  suspend fun fetchAgriWeatherForCoordinates(coordinatesLabel: String): Result<AgriWeatherReport> =
    withContext(Dispatchers.IO) {
      runCatching {
        val center = GeoCoordinateMapper.parseCenterCoordinates(coordinatesLabel)
        val urlStr =
          String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
              "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,soil_moisture_0_to_7cm" +
              "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,et0_fao_evapotranspiration" +
              "&timezone=auto&forecast_days=5",
            center.latitude,
            center.longitude,
          )
        val connection =
          (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6000
            readTimeout = 6000
            setRequestProperty("Accept", "application/json")
          }
        try {
          val code = connection.responseCode
          if (code !in 200..299) {
            error("تعذر الاتصال بمحطة الرصد الجوي (HTTP $code)")
          }
          val body = connection.inputStream.bufferedReader().use { it.readText() }
          parseOpenMeteoJson(body, GeoCoordinateMapper.formatDecimalLabel(center.latitude, center.longitude))
        } finally {
          connection.disconnect()
        }
      }
    }

  internal fun parseOpenMeteoJson(rawJson: String, coordLabel: String): AgriWeatherReport {
    val root = JSONObject(rawJson)
    val current = root.optJSONObject("current")
    val tempC = current?.optDouble("temperature_2m", 26.0) ?: 26.0
    val humidity = current?.optInt("relative_humidity_2m", 38) ?: 38
    val windKmh = current?.optDouble("wind_speed_10m", 14.0) ?: 14.0
    val rawSoilVolumetric = current?.optDouble("soil_moisture_0_to_7cm", 0.22) ?: 0.22
    val soilPercent = (rawSoilVolumetric * 100.0).coerceIn(1.0, 99.0)

    val daily = root.optJSONObject("daily")
    val times = daily?.optJSONArray("time")
    val maxTemps = daily?.optJSONArray("temperature_2m_max")
    val minTemps = daily?.optJSONArray("temperature_2m_min")
    val precip = daily?.optJSONArray("precipitation_sum")
    val et0Arr = daily?.optJSONArray("et0_fao_evapotranspiration")

    val days = mutableListOf<AgriWeatherDayForecast>()
    val count = times?.length() ?: 0
    for (i in 0 until count) {
      days +=
        AgriWeatherDayForecast(
          date = times?.optString(i, "") ?: "",
          maxTempC = maxTemps?.optDouble(i, tempC) ?: tempC,
          minTempC = minTemps?.optDouble(i, tempC - 9.0) ?: (tempC - 9.0),
          precipitationMm = precip?.optDouble(i, 0.0) ?: 0.0,
          et0Mm = et0Arr?.optDouble(i, 4.5) ?: 4.5,
        )
    }

    val todayEt0 = days.firstOrNull()?.et0Mm ?: 4.6
    val advice =
      when {
        windKmh >= 32.0 ->
          "تنبيه رياح نشطة (${String.format(Locale.US, "%.0f", windKmh)} كم/سا): يُنصح بتأجيل الرش الورقي وتثبيت شبكات البيوت البلاستيكية."
        todayEt0 >= 6.0 || tempC >= 35.0 ->
          "معدل تبخر مرتفع (ET0 ${String.format(Locale.US, "%.1f", todayEt0)} ملم): يُوصى بالسقي الليلي أو في الصباح الباكر لتقليل الفاقد المائي."
        (days.firstOrNull()?.precipitationMm ?: 0.0) >= 5.0 ->
          "توقعات تساقطات مطرية: يمكن تقليص حصة السقي التكميلي ومراقبة تصريف المياه."
        else ->
          "ظروف جوية ملائمة للأشغال الحقلية (الحرث، التسميد، والجني) مع انتظام حصة السقي بالتقطير."
      }

    return AgriWeatherReport(
      coordinatesLabel = coordLabel,
      currentTempC = tempC,
      relativeHumidityPercent = humidity,
      windSpeedKmh = windKmh,
      soilMoisturePercent = soilPercent,
      dailyEt0Mm = todayEt0,
      irrigationAdviceAr = advice,
      forecastDays = days,
    )
  }
}
