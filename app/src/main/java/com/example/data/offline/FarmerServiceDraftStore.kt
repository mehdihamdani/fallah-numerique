package com.example.data.offline

import android.content.SharedPreferences
import androidx.annotation.StringRes
import com.example.R
import com.example.data.model.AgriculturalParcel
import com.example.data.model.PartnerCatalogItem
import com.example.util.GeoCoordinateMapper
import com.example.util.GeoCoordinateMapper.GeoLatLng
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Five top-level tiles on the low-literacy Farmer Service Grid screen.
 */
enum class FarmerServiceTile(
  val tileCode: String,
  @StringRes val titleRes: Int,
  @StringRes val subtitleRes: Int,
) {
  WORKERS(
    tileCode = "workers",
    titleRes = R.string.farmer_tile_workers,
    subtitleRes = R.string.farmer_tile_workers_sub,
  ),
  MACHINERY(
    tileCode = "machinery",
    titleRes = R.string.farmer_tile_machinery,
    subtitleRes = R.string.farmer_tile_machinery_sub,
  ),
  TRANSPORT(
    tileCode = "transport",
    titleRes = R.string.farmer_tile_transport,
    subtitleRes = R.string.farmer_tile_transport_sub,
  ),
  ANALYSIS_SCAN(
    tileCode = "analysis_scan",
    titleRes = R.string.farmer_tile_analysis_scan,
    subtitleRes = R.string.farmer_tile_analysis_scan_sub,
  ),
  OTHER(
    tileCode = "other",
    titleRes = R.string.farmer_tile_other,
    subtitleRes = R.string.farmer_tile_other_sub,
  );

  companion object {
    fun fromCodeOrNull(code: String?): FarmerServiceTile? =
      entries.firstOrNull { it.tileCode == code?.trim()?.lowercase(Locale.ROOT) }
  }
}

/**
 * Maps canonical `ServiceBooking.serviceCategory` / `PartnerCatalogItem.category` codes
 * to translated string resources so technical enum codes are NEVER shown to the farmer.
 */
enum class FarmerServiceChoice(
  val categoryCode: String,
  val parentTile: FarmerServiceTile?,
  @StringRes val labelRes: Int,
  @StringRes val unitLabelRes: Int,
  val usesParcelAreaAsQuantity: Boolean = false,
) {
  // Tile 1: Workers
  FARM_LABOR(
    categoryCode = "farm_labor_supply",
    parentTile = FarmerServiceTile.WORKERS,
    labelRes = R.string.srv_cat_farm_labor_supply,
    unitLabelRes = R.string.srv_unit_day,
  ),
  HARVESTING_CREW(
    categoryCode = "harvesting_picking",
    parentTile = FarmerServiceTile.WORKERS,
    labelRes = R.string.srv_cat_harvesting_picking,
    unitLabelRes = R.string.srv_unit_day,
  ),

  // Tile 2: Machinery
  PLOWING_LEVELING(
    categoryCode = "plowing_leveling",
    parentTile = FarmerServiceTile.MACHINERY,
    labelRes = R.string.srv_cat_plowing_leveling,
    unitLabelRes = R.string.srv_unit_hectare,
    usesParcelAreaAsQuantity = true,
  ),
  TRACTOR_RENTAL(
    categoryCode = "tractor_machinery_rental",
    parentTile = FarmerServiceTile.MACHINERY,
    labelRes = R.string.srv_cat_tractor_machinery_rental,
    unitLabelRes = R.string.srv_unit_day,
  ),
  HARVESTER_RENTAL(
    categoryCode = "harvester_rental",
    parentTile = FarmerServiceTile.MACHINERY,
    labelRes = R.string.srv_cat_harvester_rental,
    unitLabelRes = R.string.srv_unit_hectare,
    usesParcelAreaAsQuantity = true,
  ),

  // Tile 3: Transport
  CROP_TRANSPORT(
    categoryCode = "agricultural_transport_service",
    parentTile = FarmerServiceTile.TRANSPORT,
    labelRes = R.string.srv_cat_agricultural_transport_service,
    unitLabelRes = R.string.srv_unit_trip,
  ),

  // Tile 4: Analysis and scanning
  SOIL_LAB_ANALYSIS(
    categoryCode = "soil_lab_inraa",
    parentTile = FarmerServiceTile.ANALYSIS_SCAN,
    labelRes = R.string.srv_cat_soil_lab_inraa,
    unitLabelRes = R.string.srv_unit_service,
  ),
  DRONE_SCAN(
    categoryCode = "drone_ndvi_scan",
    parentTile = FarmerServiceTile.ANALYSIS_SCAN,
    labelRes = R.string.srv_cat_drone_ndvi_scan,
    unitLabelRes = R.string.srv_unit_hectare,
    usesParcelAreaAsQuantity = true,
  ),

  // Tile 5: Other
  IRRIGATION_SERVICE(
    categoryCode = "irrigation_install_maint",
    parentTile = FarmerServiceTile.OTHER,
    labelRes = R.string.srv_cat_irrigation_install_maint,
    unitLabelRes = R.string.srv_unit_service,
  ),
  COLD_STORAGE(
    categoryCode = "cold_storage_warehouses",
    parentTile = FarmerServiceTile.OTHER,
    labelRes = R.string.srv_cat_cold_storage_warehouses,
    unitLabelRes = R.string.srv_unit_day,
  ),
  WELL_DRILLING(
    categoryCode = "forage_drilling",
    parentTile = FarmerServiceTile.OTHER,
    labelRes = R.string.srv_cat_forage_drilling,
    unitLabelRes = R.string.srv_unit_service,
  ),
  AGRONOMIC_ADVISORY(
    categoryCode = "agronomic_advisory",
    parentTile = FarmerServiceTile.OTHER,
    labelRes = R.string.srv_cat_agronomic_advisory,
    unitLabelRes = R.string.srv_unit_service,
  ),

  // Secondary action inside the service screen ("Call my engineer")
  CALL_MY_ENGINEER(
    categoryCode = "agronomist_consultation",
    parentTile = null,
    labelRes = R.string.srv_cat_agronomist_consultation,
    unitLabelRes = R.string.srv_unit_service,
  );

  companion object {
    fun choicesForTile(tile: FarmerServiceTile): List<FarmerServiceChoice> =
      entries.filter { it.parentTile == tile }

    fun fromCategoryCodeOrNull(code: String?): FarmerServiceChoice? =
      entries.firstOrNull { it.categoryCode == code?.trim()?.lowercase(Locale.ROOT) }
  }
}

/**
 * Plain status words shown to the farmer after submission (never exposing raw backend status enums).
 */
enum class FarmerPlainOrderStatus(
  @StringRes val labelRes: Int,
  @StringRes val descriptionRes: Int,
) {
  WAITING_FOR_PROVIDER(
    labelRes = R.string.farmer_status_waiting_provider,
    descriptionRes = R.string.farmer_status_waiting_provider_desc,
  ),
  PROVIDER_ACCEPTED(
    labelRes = R.string.farmer_status_provider_accepted,
    descriptionRes = R.string.farmer_status_provider_accepted_desc,
  ),
  DONE(
    labelRes = R.string.farmer_status_done,
    descriptionRes = R.string.farmer_status_done_desc,
  ),
  CANCELLED(
    labelRes = R.string.farmer_status_cancelled,
    descriptionRes = R.string.farmer_status_cancelled,
  );

  companion object {
    fun fromBackendStatus(status: String?): FarmerPlainOrderStatus =
      when (status?.trim()?.lowercase(Locale.ROOT)) {
        "accepted" -> PROVIDER_ACCEPTED
        "completed" -> DONE
        "cancelled" -> CANCELLED
        else -> WAITING_FOR_PROVIDER // "pending" and "quoted" both mean Waiting for a provider
      }
  }
}

/**
 * One of the 3 suggested date slots generated from the next 7 days (+1d, +3d, +6d).
 */
data class SuggestedDateSlot(
  val slotIndex: Int,
  val dayOffset: Int,
  val isoDate: String,
  val readableDayMonth: String,
  @StringRes val templateRes: Int,
)

/**
 * Indicative reference price matched from `PartnerCatalogItem`.
 */
data class IndicativePriceMatch(
  val catalogItemId: String,
  val priceDzd: Double,
  val unitLabel: String,
  val matchedSameWilaya: Boolean,
)

/**
 * Pre-filled parcel, wilaya, commune, and centroid derived from the farmer's first registered parcel.
 */
data class ParcelLocationPrefill(
  val parcelId: String,
  val parcelName: String,
  val wilaya: String,
  val commune: String,
  val areaHectares: Double,
  val centroidLat: Double,
  val centroidLng: Double,
)

/**
 * Locally persisted 3-step farmer service draft so slow or interrupted rural connections
 * never lose user input and can retry submission in a single tap.
 */
data class FarmerServiceDraft(
  val wizardStage: FarmerServiceWizardStage = FarmerServiceWizardStage.GRID,
  val selectedTileCode: String = "",
  val selectedCategoryCode: String = "",
  val selectedDateIso: String = "",
  val isCustomDate: Boolean = false,
  val hasFailedSendToRetry: Boolean = false,
  val lastSubmittedOrderId: String = "",
)

enum class FarmerServiceWizardStage {
  GRID,
  STEP_1_SERVICE,
  STEP_2_DATE,
  STEP_3_CONFIRM,
  STATUS_SCREEN,
}

interface FarmerServiceDraftStorage {
  fun loadDraft(): FarmerServiceDraft

  fun saveDraft(draft: FarmerServiceDraft)

  fun clearDraft()
}

class InMemoryFarmerServiceDraftStorage(
  initial: FarmerServiceDraft = FarmerServiceDraft()
) : FarmerServiceDraftStorage {
  private var state: FarmerServiceDraft = initial

  override fun loadDraft(): FarmerServiceDraft = state

  override fun saveDraft(draft: FarmerServiceDraft) {
    state = draft
  }

  override fun clearDraft() {
    state = FarmerServiceDraft()
  }
}

class SharedPrefsFarmerServiceDraftStorage(
  private val prefs: SharedPreferences
) : FarmerServiceDraftStorage {

  override fun loadDraft(): FarmerServiceDraft {
    val rawStage = prefs.getString(KEY_STAGE, FarmerServiceWizardStage.GRID.name).orEmpty()
    val parsedStage =
      FarmerServiceWizardStage.entries.firstOrNull { it.name == rawStage }
        ?: FarmerServiceWizardStage.GRID
    return FarmerServiceDraft(
      wizardStage = parsedStage,
      selectedTileCode = prefs.getString(KEY_TILE, "").orEmpty(),
      selectedCategoryCode = prefs.getString(KEY_CATEGORY, "").orEmpty(),
      selectedDateIso = prefs.getString(KEY_DATE_ISO, "").orEmpty(),
      isCustomDate = prefs.getBoolean(KEY_IS_CUSTOM_DATE, false),
      hasFailedSendToRetry = prefs.getBoolean(KEY_HAS_RETRY_ERROR, false),
      lastSubmittedOrderId = prefs.getString(KEY_LAST_ORDER_ID, "").orEmpty(),
    )
  }

  override fun saveDraft(draft: FarmerServiceDraft) {
    prefs
      .edit()
      .putString(KEY_STAGE, draft.wizardStage.name)
      .putString(KEY_TILE, draft.selectedTileCode)
      .putString(KEY_CATEGORY, draft.selectedCategoryCode)
      .putString(KEY_DATE_ISO, draft.selectedDateIso)
      .putBoolean(KEY_IS_CUSTOM_DATE, draft.isCustomDate)
      .putBoolean(KEY_HAS_RETRY_ERROR, draft.hasFailedSendToRetry)
      .putString(KEY_LAST_ORDER_ID, draft.lastSubmittedOrderId)
      .apply()
  }

  override fun clearDraft() {
    prefs.edit().clear().apply()
  }

  companion object {
    private const val KEY_STAGE = "farmer_srv_stage"
    private const val KEY_TILE = "farmer_srv_tile"
    private const val KEY_CATEGORY = "farmer_srv_category"
    private const val KEY_DATE_ISO = "farmer_srv_date_iso"
    private const val KEY_IS_CUSTOM_DATE = "farmer_srv_is_custom_date"
    private const val KEY_HAS_RETRY_ERROR = "farmer_srv_retry_error"
    private const val KEY_LAST_ORDER_ID = "farmer_srv_last_order_id"
  }
}

/**
 * Pure domain helpers for the 3-step Farmer Service Request flow.
 */
object FarmerServiceFlowHelper {

  private val SUGGESTED_DAY_OFFSETS =
    listOf(
      1 to R.string.farmer_slot_tomorrow,
      3 to R.string.farmer_slot_in_three_days,
      6 to R.string.farmer_slot_end_of_week,
    )

  /**
   * Generates exactly 3 suggested date slots spread across the next 7 days (+1d, +3d, +6d).
   */
  fun generateSuggestedSlotsNext7Days(
    nowMillis: Long = System.currentTimeMillis(),
    displayLocale: Locale = Locale.forLanguageTag("ar"),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): List<SuggestedDateSlot> {
    val isoFormatter =
      SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { this.timeZone = timeZone }
    val readableFormatter =
      SimpleDateFormat("EEEE d MMM", displayLocale).apply { this.timeZone = timeZone }

    return SUGGESTED_DAY_OFFSETS.mapIndexed { index, (offsetDays, resId) ->
      val cal = Calendar.getInstance(timeZone, Locale.US)
      cal.timeInMillis = nowMillis
      cal.add(Calendar.DAY_OF_YEAR, offsetDays)
      val date = Date(cal.timeInMillis)
      SuggestedDateSlot(
        slotIndex = index,
        dayOffset = offsetDays,
        isoDate = isoFormatter.format(date),
        readableDayMonth = readableFormatter.format(date),
        templateRes = resId,
      )
    }
  }

  /**
   * Generates simple upcoming single-tap "Other date" choices (+2d, +4d, +5d, +7d, +10d, +14d)
   * when the farmer explicitly taps "Other date" — avoiding an overwhelming free-grid calendar.
   */
  fun generateOtherDateOptions(
    nowMillis: Long = System.currentTimeMillis(),
    displayLocale: Locale = Locale.forLanguageTag("ar"),
    timeZone: TimeZone = TimeZone.getDefault(),
  ): List<Pair<String, String>> {
    val offsets = listOf(2, 4, 5, 7, 10, 14)
    val isoFormatter =
      SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { this.timeZone = timeZone }
    val readableFormatter =
      SimpleDateFormat("EEEE d MMMM", displayLocale).apply { this.timeZone = timeZone }

    return offsets.map { offset ->
      val cal = Calendar.getInstance(timeZone, Locale.US)
      cal.timeInMillis = nowMillis
      cal.add(Calendar.DAY_OF_YEAR, offset)
      val date = Date(cal.timeInMillis)
      isoFormatter.format(date) to readableFormatter.format(date)
    }
  }

  /**
   * Pre-fills parcel, wilaya, commune, and centroid from the farmer's FIRST registered parcel,
   * using [GeoCoordinateMapper.computeCentroid] on `polygonPoints`.
   * Returns `null` if the farmer has no registered parcel in Firestore (`id.isBlank()`).
   */
  fun resolveFirstParcelPrefill(parcels: List<AgriculturalParcel>): ParcelLocationPrefill? {
    val firstParcel = parcels.firstOrNull { it.id.isNotBlank() } ?: return null
    val fallbackCenter = GeoCoordinateMapper.parseCenterCoordinates(firstParcel.coordinatesLabel)
    val vertices =
      GeoCoordinateMapper.decodePolygonToLatLng(firstParcel.polygonPoints, fallbackCenter)
    val centroid =
      GeoCoordinateMapper.computeCentroid(
        vertices = vertices,
        fallback = GeoLatLng(fallbackCenter.latitude, fallbackCenter.longitude),
      )
    return ParcelLocationPrefill(
      parcelId = firstParcel.id,
      parcelName = firstParcel.name,
      wilaya = firstParcel.wilaya,
      commune = firstParcel.commune,
      areaHectares = firstParcel.areaHectares.coerceAtLeast(0.1),
      centroidLat = centroid.latitude,
      centroidLng = centroid.longitude,
    )
  }

  /**
   * Finds the best indicative reference price from active `PartnerCatalogItem` records matching
   * [serviceCategoryCode], prioritizing providers in the farmer's wilaya first.
   */
  fun findIndicativeReferencePrice(
    catalogItems: List<PartnerCatalogItem>,
    serviceCategoryCode: String,
    farmerWilaya: String,
  ): IndicativePriceMatch? {
    val cleanCat = serviceCategoryCode.trim().lowercase(Locale.ROOT)
    if (cleanCat.isEmpty()) return null
    val matching =
      catalogItems.filter { item ->
        item.status == "active" &&
          item.category.trim().lowercase(Locale.ROOT) == cleanCat &&
          item.priceDzd > 0.0
      }
    if (matching.isEmpty()) return null

    val farmerWilayaCode = extractWilayaPrefix(farmerWilaya)
    val localMatches =
      matching.filter { item ->
        item.wilaya.equals(farmerWilaya, ignoreCase = true) ||
          (farmerWilayaCode.isNotEmpty() && extractWilayaPrefix(item.wilaya) == farmerWilayaCode)
      }
    val chosen = (localMatches.ifEmpty { matching }).minByOrNull { it.priceDzd } ?: return null
    return IndicativePriceMatch(
      catalogItemId = chosen.id,
      priceDzd = chosen.priceDzd,
      unitLabel = chosen.unitLabel,
      matchedSameWilaya = localMatches.isNotEmpty(),
    )
  }

  private fun extractWilayaPrefix(wilaya: String): String =
    wilaya.trim().substringBefore("-").trim()
}
