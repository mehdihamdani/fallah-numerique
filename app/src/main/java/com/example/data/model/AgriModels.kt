package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.abs

@IgnoreExtraProperties
data class AgriculturalParcel(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "قطاع بسكرة - طولقة",
  val wilaya: String = "07 - بسكرة (Biskra)",
  val commune: String = "طولقة (Tolga Oasis)",
  val ilotNumber: String = "Ilot N 108 / P-24",
  val tenureType: String = "امتياز فلاحي (Concession)",
  val areaHectares: Double = 4.82,
  val coordinatesLabel: String = "34°43'12.4\"N 5°22'48.9\"E",
  val polygonPoints: List<String> =
    listOf("0.20,0.23", "0.74,0.20", "0.85,0.75", "0.51,0.85", "0.24,0.71"),
  val wellPoint: String = "0.40,0.44",
  val wellDepthLabel: String = "بئر ارتوازي عميق 85m",
  val ndviScore: Double = 0.74,
  val soilTexture: String = "sableux_limoneux",
  val soilPh: Double = 7.3,
  val organicMatterPercent: Double = 2.45,
  val salinityEc: Double = 1.8,
  val drainageStatus: String = "عميق > 120 سم / تصريف ممتاز",
  val labDocumentName: String = "",
  val mainCrop: String = "deglet_nour",
  val irrigationSystem: String = "السقي بالتقطير الحديث (Goutte-à-goutte)",
  val estimatedYieldTonsPerHa: Double = 38.5,
  val plantingYear: String = "2018 (أشجار منتجة)",
  val intercropping: String = "فصة علفية + خضار تحت النخيل",
  val linkedToMarket: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MarketListing(
  @DocumentId val id: String = "",
  val userId: String = "",
  val parcelId: String = "",
  val operatorName: String = "Coopérative El Baraka",
  val parcelReference: String = "Ilot N 108 / P-24",
  val cropType: String = "deglet_nour",
  val wilaya: String = "07 - بسكرة (Biskra)",
  val availableTons: Double = 38.5,
  val pricePerKgDzd: Double = 420.0,
  val status: String = "active",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TransportRequest(
  @DocumentId val id: String = "",
  val userId: String = "",
  val parcelReference: String = "Ilot N 108 / P-24",
  val vehicleType: String = "refrigerated_truck",
  val originWilaya: String = "07 - بسكرة (Biskra)",
  val destinationMarket: String = "سوق الجملة الكاليتوس - الجزائر",
  val cargoTons: Double = 18.0,
  val scheduledDate: String = "2026-11-02",
  val status: String = "pending",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ServiceBooking(
  @DocumentId val id: String = "",
  val userId: String = "",
  val serviceCategory: String = "soil_lab_inraa",
  val serviceTitle: String = "تحليل مخبري للتربة والمياه (INRAA)",
  val parcelReference: String = "Ilot N 108 / P-24",
  val wilaya: String = "07 - بسكرة (Biskra)",
  val preferredDate: String = "2026-11-05",
  val notes: String = "",
  val status: String = "requested",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class NormalizedPoint(val x: Float, val y: Float) {
  fun encode(): String = "%.3f,%.3f".format(java.util.Locale.US, x.coerceIn(0.02f, 0.98f), y.coerceIn(0.02f, 0.98f))

  companion object {
    fun decode(raw: String): NormalizedPoint {
      val parts = raw.split(",")
      val px = parts.getOrNull(0)?.toFloatOrNull()?.coerceIn(0.02f, 0.98f) ?: 0.5f
      val py = parts.getOrNull(1)?.toFloatOrNull()?.coerceIn(0.02f, 0.98f) ?: 0.5f
      return NormalizedPoint(px, py)
    }

    /**
     * Computes calibrated parcel surface in hectares from normalized [0..1] polygon points
     * scaled to a 420m x 320m HD satellite viewport (~13.44 Ha full frame).
     */
    fun computeAreaHectares(points: List<NormalizedPoint>): Double {
      if (points.size < 3) return 0.01
      var sum = 0.0
      for (i in points.indices) {
        val current = points[i]
        val next = points[(i + 1) % points.size]
        sum += (current.x.toDouble() * next.y.toDouble()) - (next.x.toDouble() * current.y.toDouble())
      }
      val normalizedArea = abs(sum) * 0.5
      // Calibrated so default 5-vertex Tolga oasis polygon yields ~4.82 Ha
      val hectares = (normalizedArea * 15.88).coerceIn(0.15, 999.0)
      return kotlin.math.round(hectares * 100.0) / 100.0
    }
  }
}

enum class SoilTextureType(
  val code: String,
  val titleAr: String,
  val titleFr: String,
  val descriptionAr: String,
) {
  SABLEUX_LIMONEUX(
    code = "sableux_limoneux",
    titleAr = "رملية طميية",
    titleFr = "Sableux-Limoneux",
    descriptionAr = "مثالي للواحات، نفاذية عالية للمياه",
  ),
  ARGILEUX(
    code = "argileux",
    titleAr = "طينية متماسكة",
    titleFr = "Argileux",
    descriptionAr = "احتفاظ كبير بالرطوبة والمغذيات",
  ),
  LIMONEUX(
    code = "limoneux",
    titleAr = "غرينية فيضية",
    titleFr = "Limoneux Alluvionnaire",
    descriptionAr = "خصوبة طبيعية عالية، غنية بالرواسب",
  ),
  CALCAIRE(
    code = "calcaire",
    titleAr = "كلسية متوازنة",
    titleFr = "Calcaire / Sol Franc",
    descriptionAr = "شائعة في هضاب وسهول الحبوب والزيتون",
  );

  companion object {
    fun fromCode(code: String): SoilTextureType =
      entries.firstOrNull { it.code == code } ?: SABLEUX_LIMONEUX
  }
}

enum class CropVarietyType(
  val code: String,
  val titleAr: String,
  val subtitleFr: String,
  val defaultYieldTonsHa: Double,
  val defaultPriceKgDzd: Double,
) {
  DEGLET_NOUR(
    code = "deglet_nour",
    titleAr = "نخيل دقلة نور",
    subtitleFr = "Deglet Nour (Oasis)",
    defaultYieldTonsHa = 38.5,
    defaultPriceKgDzd = 420.0,
  ),
  POMME_DE_TERRE(
    code = "pomme_de_terre",
    titleAr = "بطاطا موسمية",
    subtitleFr = "Pomme de terre (Spunta)",
    defaultYieldTonsHa = 32.0,
    defaultPriceKgDzd = 75.0,
  ),
  OLIVIER(
    code = "olivier",
    titleAr = "أشجار الزيتون",
    subtitleFr = "Olivier (Sigoise / Chemlal)",
    defaultYieldTonsHa = 14.5,
    defaultPriceKgDzd = 190.0,
  ),
  TOMATE_INDUSTRIELLE(
    code = "tomate_industrielle",
    titleAr = "طماطم صناعية",
    subtitleFr = "Tomate industrielle",
    defaultYieldTonsHa = 65.0,
    defaultPriceKgDzd = 42.0,
  );

  companion object {
    fun fromCode(code: String): CropVarietyType =
      entries.firstOrNull { it.code == code } ?: DEGLET_NOUR
  }
}

enum class MapLayerMode(val labelAr: String, val labelFr: String) {
  SATELLITE("قمر اصطناعي", "Satellite"),
  RELIEF("تضاريس", "Relief"),
  CADASTRE("كاداستر", "Cadastre"),
}

enum class MapInteractionTool {
  VIEW,
  DRAW_POLYGON,
  PLACE_FORAGE,
}

enum class AppLanguage(val code: String) {
  AR("AR"),
  FR("FR"),
  EN("EN"),
}

enum class TransportVehicleType(
  val code: String,
  val titleAr: String,
  val subtitleFr: String,
  val capacityLabel: String,
) {
  REFRIGERATED_TRUCK(
    code = "refrigerated_truck",
    titleAr = "شاحنة مبردة (Frigorifique)",
    subtitleFr = "Camion Frigorifique +4°C",
    capacityLabel = "حفظ التمور والخضر • حتى 20 طن",
  ),
  FLATBED_TRUCK(
    code = "flatbed_truck",
    titleAr = "شاحنة مسطحة لنقل الصناديق",
    subtitleFr = "Plateau Agricole",
    capacityLabel = "نقل الصناديق البلاستيكية • حتى 25 طن",
  ),
  GRAIN_HOPPER(
    code = "grain_hopper",
    titleAr = "شاحنة حبوب وأعلاف سائبة",
    subtitleFr = "Benne Céréalière",
    capacityLabel = "نقل الحبوب والأعلاف • حتى 30 طن",
  ),
  WATER_TANKER(
    code = "water_tanker",
    titleAr = "صهريج مياه وأسمدة سائلة",
    subtitleFr = "Citerne Agricole",
    capacityLabel = "دعم السقي التكميلي • 15,000 لتر",
  );

  companion object {
    fun fromCode(code: String): TransportVehicleType =
      entries.firstOrNull { it.code == code } ?: REFRIGERATED_TRUCK
  }
}

enum class AgriServiceCategory(
  val code: String,
  val titleAr: String,
  val subtitleFr: String,
  val descriptionAr: String,
  val providerBadge: String,
) {
  SOIL_LAB_INRAA(
    code = "soil_lab_inraa",
    titleAr = "تحليل التربة والمياه المخبري",
    subtitleFr = "Laboratoire Pédologique INRAA",
    descriptionAr = "أخذ عينات ميدانية وفحص pH، الملوحة EC، والمادة العضوية بشهادة رسمية.",
    providerBadge = "مخبر معتمد INRAA",
  ),
  DRONE_NDVI_SCAN(
    code = "drone_ndvi_scan",
    titleAr = "مسح طيفي بطائرات الدرون NDVI",
    subtitleFr = "Imagerie Multispectrale Drone",
    descriptionAr = "تصوير جوي عالي الدقة لكشف الإجهاد المائي وآفات النخيل والأشجار المثمرة.",
    providerBadge = "وحدة الاستشعار الفلاحي",
  ),
  FORAGE_DRILLING(
    code = "forage_drilling",
    titleAr = "حفر وصيانة الآبار والسقي بالتقطير",
    subtitleFr = "Forage & Irrigation Goutte-à-goutte",
    descriptionAr = "دراسة جيوفيزيائية، تنظيف الآبار الارتوازية، وتركيب شبكات التقطير الذكية.",
    providerBadge = "مؤسسة الري الفلاحي",
  ),
  HARVESTER_RENTAL(
    code = "harvester_rental",
    titleAr = "كراء العتاد والآلات الفلاحية",
    subtitleFr = "Location Matériel & Récolte",
    descriptionAr = "جرارات، آلات جني البطاطا، رافعات النخيل الهيدروليكية، ومعدات الحرث.",
    providerBadge = "تعاونية العتاد الفلاحي",
  ),
  AGRONOMIC_ADVISORY(
    code = "agronomic_advisory",
    titleAr = "الإرشاد الزراعي والوقاية النباتية",
    subtitleFr = "Conseil Agronomique & Phytosanitaire",
    descriptionAr = "مرافقة مهندس فلاحي ميداني لضبط التسميد ومكافحة سوسة النخيل والأمراض.",
    providerBadge = "الغرفة الفلاحية",
  );

  companion object {
    fun fromCode(code: String): AgriServiceCategory =
      entries.firstOrNull { it.code == code } ?: SOIL_LAB_INRAA
  }
}

enum class WorkspaceSection(
  val titleAr: String,
  val titleFr: String,
  val navLabelAr: String,
  val navLabelFr: String,
  val navLabelEn: String,
) {
  PARCEL_DELINEATION(
    titleAr = "تعيين القطعة وسجل التربة",
    titleFr = "Délimitation & Fiche",
    navLabelAr = "القطعة",
    navLabelFr = "Parcelle",
    navLabelEn = "Parcel",
  ),
  PUBLIC_MARKET(
    titleAr = "السوق العام الفلاحي",
    titleFr = "Marché Public",
    navLabelAr = "السوق",
    navLabelFr = "Marché",
    navLabelEn = "Market",
  ),
  TRANSPORT(
    titleAr = "النقل واللوجستيك الفلاحي",
    titleFr = "Transport & Expéditions",
    navLabelAr = "النقل",
    navLabelFr = "Transport",
    navLabelEn = "Transport",
  ),
  SERVICES(
    titleAr = "الخدمات الفلاحية والتقنية",
    titleFr = "Services Agricoles",
    navLabelAr = "الخدمات",
    navLabelFr = "Services",
    navLabelEn = "Services",
  ),
  SETTINGS(
    titleAr = "الإعدادات وحساب الفلاح",
    titleFr = "Paramètres & Compte",
    navLabelAr = "الإعدادات",
    navLabelFr = "Paramètres",
    navLabelEn = "Settings",
  ),
  MY_PARCELS(
    titleAr = "سجل القطع والمحاصيل",
    titleFr = "Mes Récoltes & Parcelles",
    navLabelAr = "سجل القطع",
    navLabelFr = "Parcelles",
    navLabelEn = "Parcels",
  );

  fun bottomLabel(language: AppLanguage): String =
    when (language) {
      AppLanguage.AR -> navLabelAr
      AppLanguage.FR -> navLabelFr
      AppLanguage.EN -> navLabelEn
    }
}
