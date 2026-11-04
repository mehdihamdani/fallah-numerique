package com.example

import com.example.data.model.AgriculturalParcel
import com.example.data.model.PartnerCatalogItem
import com.example.data.offline.FarmerPlainOrderStatus
import com.example.data.offline.FarmerServiceChoice
import com.example.data.offline.FarmerServiceDraft
import com.example.data.offline.FarmerServiceFlowHelper
import com.example.data.offline.FarmerServiceTile
import com.example.data.offline.FarmerServiceWizardStage
import com.example.data.offline.InMemoryFarmerServiceDraftStorage
import com.example.util.GeoCoordinateMapper
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FarmerServiceFlowUnitTest {

  @Test
  fun fiveServiceTiles_coverExpectedCategories_andSecondaryEngineerMapsCleanly() {
    assertEquals(5, FarmerServiceTile.entries.size)

    val workersCodes =
      FarmerServiceChoice.choicesForTile(FarmerServiceTile.WORKERS).map { it.categoryCode }
    assertEquals(listOf("farm_labor_supply", "harvesting_picking"), workersCodes)

    val machineryCodes =
      FarmerServiceChoice.choicesForTile(FarmerServiceTile.MACHINERY).map { it.categoryCode }
    assertEquals(
      listOf("plowing_leveling", "tractor_machinery_rental", "harvester_rental"),
      machineryCodes,
    )

    val transportCodes =
      FarmerServiceChoice.choicesForTile(FarmerServiceTile.TRANSPORT).map { it.categoryCode }
    assertEquals(listOf("agricultural_transport_service"), transportCodes)

    val analysisCodes =
      FarmerServiceChoice.choicesForTile(FarmerServiceTile.ANALYSIS_SCAN).map { it.categoryCode }
    assertEquals(listOf("soil_lab_inraa", "drone_ndvi_scan"), analysisCodes)

    val otherCodes =
      FarmerServiceChoice.choicesForTile(FarmerServiceTile.OTHER).map { it.categoryCode }
    assertTrue(otherCodes.isNotEmpty())

    assertEquals(
      "agronomist_consultation",
      FarmerServiceChoice.CALL_MY_ENGINEER.categoryCode,
    )
  }

  @Test
  fun suggestedSlots_generateThreeConcreteDatesWithinNextSevenDays() {
    val fixedUtc = TimeZone.getTimeZone("UTC")
    // 2026-11-03T12:00:00Z
    val baseMillis = 1793707200000L
    val slots =
      FarmerServiceFlowHelper.generateSuggestedSlotsNext7Days(
        nowMillis = baseMillis,
        displayLocale = Locale.forLanguageTag("ar"),
        timeZone = fixedUtc,
      )

    assertEquals(3, slots.size)
    assertTrue(slots.all { it.dayOffset in 1..7 })
    assertEquals(listOf(1, 3, 6), slots.map { it.dayOffset })
    assertEquals("2026-11-04", slots[0].isoDate)
    assertEquals("2026-11-06", slots[1].isoDate)
    assertEquals("2026-11-09", slots[2].isoDate)
  }

  @Test
  fun resolveFirstParcelPrefill_computesCentroidFromFirstParcel_andBlocksWhenNoParcel() {
    // 1. Landless farmer (empty list or unsaved blank-ID draft) -> null (triggers field-visit blocker)
    assertNull(FarmerServiceFlowHelper.resolveFirstParcelPrefill(emptyList()))
    assertNull(
      FarmerServiceFlowHelper.resolveFirstParcelPrefill(listOf(AgriculturalParcel(id = "")))
    )

    // 2. Farmer with registered parcels -> pre-fills from FIRST saved parcel using GeoCoordinateMapper.computeCentroid
    val p1 =
      AgriculturalParcel(
        id = "parcel_biskra_01",
        name = "واحة النخيل طولقة",
        wilaya = "07 - بسكرة (Biskra)",
        commune = "طولقة",
        areaHectares = 5.4,
        polygonPoints =
          listOf(
            "geo:34.720000,5.380000",
            "geo:34.721000,5.380000",
            "geo:34.721000,5.382000",
            "geo:34.720000,5.382000",
          ),
      )
    val p2 =
      AgriculturalParcel(
        id = "parcel_blida_02",
        name = "حقل بوفاريك",
        wilaya = "09 - البليدة (Blida)",
        commune = "بوفاريك",
      )

    val prefill = FarmerServiceFlowHelper.resolveFirstParcelPrefill(listOf(p1, p2))
    assertNotNull(prefill)
    assertEquals("parcel_biskra_01", prefill!!.parcelId)
    assertEquals("07 - بسكرة (Biskra)", prefill.wilaya)
    assertEquals("طولقة", prefill.commune)

    val expectedCentroid =
      GeoCoordinateMapper.computeCentroid(
        listOf(
          GeoCoordinateMapper.GeoLatLng(34.720000, 5.380000),
          GeoCoordinateMapper.GeoLatLng(34.721000, 5.380000),
          GeoCoordinateMapper.GeoLatLng(34.721000, 5.382000),
          GeoCoordinateMapper.GeoLatLng(34.720000, 5.382000),
        ),
        GeoCoordinateMapper.GeoLatLng(0.0, 0.0),
      )
    assertEquals(expectedCentroid.latitude, prefill.centroidLat, 1e-6)
    assertEquals(expectedCentroid.longitude, prefill.centroidLng, 1e-6)
  }

  @Test
  fun findIndicativeReferencePrice_prioritizesSameWilayaActiveCatalogItems() {
    val catalog =
      listOf(
        PartnerCatalogItem(
          id = "cat_setif",
          category = "tractor_machinery_rental",
          wilaya = "19 - سطيف (Sétif)",
          priceDzd = 5000.0,
          unitLabel = "يوم",
          status = "active",
        ),
        PartnerCatalogItem(
          id = "cat_biskra_paused",
          category = "tractor_machinery_rental",
          wilaya = "07 - بسكرة (Biskra)",
          priceDzd = 4000.0,
          unitLabel = "يوم",
          status = "paused",
        ),
        PartnerCatalogItem(
          id = "cat_biskra_active",
          category = "tractor_machinery_rental",
          wilaya = "07 - بسكرة (Biskra)",
          priceDzd = 6500.0,
          unitLabel = "يوم",
          status = "active",
        ),
      )

    val biskraMatch =
      FarmerServiceFlowHelper.findIndicativeReferencePrice(
        catalogItems = catalog,
        serviceCategoryCode = "tractor_machinery_rental",
        farmerWilaya = "07 - بسكرة (Biskra)",
      )
    assertNotNull(biskraMatch)
    assertEquals("cat_biskra_active", biskraMatch!!.catalogItemId)
    assertEquals(6500.0, biskraMatch.priceDzd, 0.01)
    assertTrue(biskraMatch.matchedSameWilaya)
  }

  @Test
  fun plainOrderStatusMapping_hidesBackendEnumsAndPreservesDraftOnFailure() {
    assertEquals(
      FarmerPlainOrderStatus.WAITING_FOR_PROVIDER,
      FarmerPlainOrderStatus.fromBackendStatus("pending"),
    )
    assertEquals(
      FarmerPlainOrderStatus.WAITING_FOR_PROVIDER,
      FarmerPlainOrderStatus.fromBackendStatus("quoted"),
    )
    assertEquals(
      FarmerPlainOrderStatus.PROVIDER_ACCEPTED,
      FarmerPlainOrderStatus.fromBackendStatus("accepted"),
    )
    assertEquals(
      FarmerPlainOrderStatus.DONE,
      FarmerPlainOrderStatus.fromBackendStatus("completed"),
    )

    // Draft survives network failure without losing selected service or date
    val draftStorage = InMemoryFarmerServiceDraftStorage()
    draftStorage.saveDraft(
      FarmerServiceDraft(
        wizardStage = FarmerServiceWizardStage.STEP_3_CONFIRM,
        selectedTileCode = "machinery",
        selectedCategoryCode = "plowing_leveling",
        selectedDateIso = "2026-11-06",
        isCustomDate = false,
        hasFailedSendToRetry = true,
      )
    )
    val reloaded = draftStorage.loadDraft()
    assertEquals(FarmerServiceWizardStage.STEP_3_CONFIRM, reloaded.wizardStage)
    assertEquals("plowing_leveling", reloaded.selectedCategoryCode)
    assertEquals("2026-11-06", reloaded.selectedDateIso)
    assertTrue(reloaded.hasFailedSendToRetry)
  }
}
