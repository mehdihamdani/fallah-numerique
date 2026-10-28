package com.example.util

import com.example.data.model.NormalizedPoint
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Bijective WGS84 geographic projection utility converting between parcel-relative
 * [NormalizedPoint] ([0.02..0.98] inside a 420m × 320m agronomic frame) and real-world
 * WGS84 [GeoLatLng] coordinates, plus spherical geodesy surface calculation.
 * Operates completely without any proprietary API key dependency.
 */
object GeoCoordinateMapper {

  // Default Tolga Oasis center (34°43'12.4"N 5°22'48.9"E)
  const val DEFAULT_TOLGA_LAT = 34.720111
  const val DEFAULT_TOLGA_LNG = 5.380250

  // Viewport span matching NormalizedPoint.computeAreaHectares calibration (~420m E-W × 320m N-S)
  private const val FRAME_WIDTH_METERS = 420.0
  private const val FRAME_HEIGHT_METERS = 320.0
  private const val METERS_PER_DEGREE_LAT = 111_320.0
  private const val EARTH_RADIUS_METERS = 6_378_137.0

  data class GeoLatLng(val latitude: Double, val longitude: Double)

  data class GeoCenter(val latitude: Double, val longitude: Double)

  /**
   * Parses DMS (`34°43'12.4"N 5°22'48.9"E`) or decimal (`34.7201°N 5.3803°E` / `34.7201, 5.3803`)
   * coordinate labels into WGS84 decimal degrees, falling back to Tolga Oasis if unparseable.
   */
  fun parseCenterCoordinates(label: String): GeoCenter {
    val clean = label.trim()
    if (clean.isEmpty()) return GeoCenter(DEFAULT_TOLGA_LAT, DEFAULT_TOLGA_LNG)

    // 1. Try DMS pattern: 34°43'12.4"N 5°22'48.9"E
    val dmsRegex =
      Regex("""(\d+)\s*°\s*(\d+)\s*['′]\s*([\d.]+)\s*["″]?\s*([NSns])\s+(\d+)\s*°\s*(\d+)\s*['′]\s*([\d.]+)\s*["″]?\s*([EWew])""")
    dmsRegex.find(clean)?.let { match ->
      val (latD, latM, latS, latH, lonD, lonM, lonS, lonH) = match.destructured
      val lat = dmsToDecimal(latD.toDouble(), latM.toDouble(), latS.toDouble(), latH)
      val lon = dmsToDecimal(lonD.toDouble(), lonM.toDouble(), lonS.toDouble(), lonH)
      if (lat in -90.0..90.0 && lon in -180.0..180.0) {
        return GeoCenter(lat, lon)
      }
    }

    // 2. Try Decimal with hemisphere: 34.7201°N 5.3803°E
    val decHemiRegex = Regex("""([\d.]+)\s*°?\s*([NSns])[\s,]+([\d.]+)\s*°?\s*([EWew])""")
    decHemiRegex.find(clean)?.let { match ->
      val (latRaw, latH, lonRaw, lonH) = match.destructured
      val latSign = if (latH.equals("S", ignoreCase = true)) -1.0 else 1.0
      val lonSign = if (lonH.equals("W", ignoreCase = true)) -1.0 else 1.0
      val lat = (latRaw.toDoubleOrNull() ?: DEFAULT_TOLGA_LAT) * latSign
      val lon = (lonRaw.toDoubleOrNull() ?: DEFAULT_TOLGA_LNG) * lonSign
      if (lat in -90.0..90.0 && lon in -180.0..180.0) {
        return GeoCenter(lat, lon)
      }
    }

    // 3. Try plain pair: 34.7201, 5.3803
    val plainRegex = Regex("""(-?\d+\.\d+)[\s,]+(-?\d+\.\d+)""")
    plainRegex.find(clean)?.let { match ->
      val (latRaw, lonRaw) = match.destructured
      val lat = latRaw.toDoubleOrNull()
      val lon = lonRaw.toDoubleOrNull()
      if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
        return GeoCenter(lat, lon)
      }
    }

    return GeoCenter(DEFAULT_TOLGA_LAT, DEFAULT_TOLGA_LNG)
  }

  fun formatDecimalLabel(latitude: Double, longitude: Double): String {
    val latDir = if (latitude >= 0) "N" else "S"
    val lonDir = if (longitude >= 0) "E" else "W"
    return String.format(
      Locale.US,
      "%.4f°%s %.4f°%s",
      abs(latitude),
      latDir,
      abs(longitude),
      lonDir,
    )
  }

  fun normalizedToLatLng(point: NormalizedPoint, center: GeoCenter): GeoLatLng {
    val cosLat = cos(Math.toRadians(center.latitude)).coerceAtLeast(0.1)
    val metersPerDegLng = METERS_PER_DEGREE_LAT * cosLat
    val latSpan = FRAME_HEIGHT_METERS / METERS_PER_DEGREE_LAT
    val lngSpan = FRAME_WIDTH_METERS / metersPerDegLng

    // Screen Y grows downward (South), X grows rightward (East)
    val lat = center.latitude + (0.5 - point.y.toDouble()) * latSpan
    val lng = center.longitude + (point.x.toDouble() - 0.5) * lngSpan
    return GeoLatLng(lat, lng)
  }

  fun latLngToNormalized(latLng: GeoLatLng, center: GeoCenter): NormalizedPoint {
    val cosLat = cos(Math.toRadians(center.latitude)).coerceAtLeast(0.1)
    val metersPerDegLng = METERS_PER_DEGREE_LAT * cosLat
    val latSpan = FRAME_HEIGHT_METERS / METERS_PER_DEGREE_LAT
    val lngSpan = FRAME_WIDTH_METERS / metersPerDegLng

    val normX = (0.5 + (latLng.longitude - center.longitude) / lngSpan).toFloat().coerceIn(0.02f, 0.98f)
    val normY = (0.5 - (latLng.latitude - center.latitude) / latSpan).toFloat().coerceIn(0.02f, 0.98f)
    return NormalizedPoint(normX, normY)
  }

  /**
   * Computes spherical geodesic polygon area in Hectares from real WGS84 [GeoLatLng] vertices.
   */
  fun computeGeodesicAreaHectares(vertices: List<GeoLatLng>): Double {
    if (vertices.size < 3) return 0.15
    var totalAngle = 0.0
    val n = vertices.size
    for (i in 0 until n) {
      val p1 = vertices[i]
      val p2 = vertices[(i + 1) % n]
      val lat1 = Math.toRadians(p1.latitude)
      val lng1 = Math.toRadians(p1.longitude)
      val lat2 = Math.toRadians(p2.latitude)
      val lng2 = Math.toRadians(p2.longitude)
      totalAngle += (lng2 - lng1) * (2.0 + sin(lat1) + sin(lat2))
    }
    val areaSqMeters = abs(totalAngle * EARTH_RADIUS_METERS * EARTH_RADIUS_METERS / 2.0)
    val hectares = (areaSqMeters / 10_000.0).coerceIn(0.15, 999.0)
    return kotlin.math.round(hectares * 100.0) / 100.0
  }

  fun computeCentroid(vertices: List<GeoLatLng>, fallback: GeoLatLng): GeoLatLng {
    if (vertices.isEmpty()) return fallback
    val avgLat = vertices.sumOf { it.latitude } / vertices.size
    val avgLng = vertices.sumOf { it.longitude } / vertices.size
    return GeoLatLng(avgLat, avgLng)
  }

  /**
   * Presets for major Algerian agricultural basins for offline region downloading.
   */
  data class OfflineRegionPreset(
    val id: String,
    val regionName: String,
    val wilaya: String,
    val centerLat: Double,
    val centerLng: Double,
    val radiusKm: Double,
    val estimatedSizeMb: Double,
  )

  val ALGERIAN_OFFLINE_PRESETS: List<OfflineRegionPreset> =
    listOf(
      OfflineRegionPreset(
        id = "region_biskra_tolga",
        regionName = "حوض واحات طولقة وفوغالة (النخيل)",
        wilaya = "07 - بسكرة (Biskra)",
        centerLat = 34.7201,
        centerLng = 5.3803,
        radiusKm = 12.5,
        estimatedSizeMb = 18.4,
      ),
      OfflineRegionPreset(
        id = "region_el_oued_souf",
        regionName = "المحيط الفلاحي وادي سوف (البطاطا والرش المحوري)",
        wilaya = "39 - الوادي (El Oued)",
        centerLat = 33.3683,
        centerLng = 6.8674,
        radiusKm = 18.0,
        estimatedSizeMb = 24.6,
      ),
      OfflineRegionPreset(
        id = "region_mitidja_blida",
        regionName = "سهل متيجة وبوفاريك (الحوامض والأشجار المثمرة)",
        wilaya = "09 - البليدة (Blida)",
        centerLat = 36.5739,
        centerLng = 2.9119,
        radiusKm = 15.0,
        estimatedSizeMb = 21.2,
      ),
      OfflineRegionPreset(
        id = "region_setif_high_plateaus",
        regionName = "الهضاب العليا سطيف (الحبوب الكبرى والقمح الصلب)",
        wilaya = "19 - سطيف (Sétif)",
        centerLat = 36.1898,
        centerLng = 5.4108,
        radiusKm = 22.0,
        estimatedSizeMb = 27.5,
      ),
      OfflineRegionPreset(
        id = "region_mascara_ghriss",
        regionName = "سهل غريس معسكر (الزيتون والخضروات)",
        wilaya = "29 - معسكر (Mascara)",
        centerLat = 35.2486,
        centerLng = 0.1597,
        radiusKm = 14.0,
        estimatedSizeMb = 19.0,
      ),
    )

  private fun dmsToDecimal(deg: Double, min: Double, sec: Double, hemi: String): Double {
    val sign = if (hemi.equals("S", ignoreCase = true) || hemi.equals("W", ignoreCase = true)) -1.0 else 1.0
    return sign * (deg + min / 60.0 + sec / 3600.0)
  }
}
