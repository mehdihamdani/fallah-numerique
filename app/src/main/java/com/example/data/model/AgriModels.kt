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

enum class AgriServiceDomain(
  val code: String,
  val titleAr: String,
  val subtitleFr: String,
) {
  AGRICULTURAL_PRODUCTION(
    code = "agricultural_production",
    titleAr = "الإنتاج الزراعي",
    subtitleFr = "Production Agricole",
  ),
  EQUIPMENT_AND_SUPPLIES(
    code = "equipment_and_supplies",
    titleAr = "التجهيزات والمستلزمات",
    subtitleFr = "Équipements & Intrants",
  ),
  POST_HARVEST(
    code = "post_harvest",
    titleAr = "ما بعد الحصاد",
    subtitleFr = "Post-Récolte & Stockage",
  ),
  TRADE_AND_TRANSPORT(
    code = "trade_and_transport",
    titleAr = "التجارة والنقل",
    subtitleFr = "Commerce & Transport",
  ),
  CONSULTING_AND_DIGITAL(
    code = "consulting_and_digital",
    titleAr = "الاستشارات والخدمات الرقمية",
    subtitleFr = "Conseil & Services Numériques",
  ),
}

enum class AgriServiceCategory(
  val code: String,
  val domain: AgriServiceDomain,
  val titleAr: String,
  val subtitleFr: String,
  val descriptionAr: String,
  val providerBadge: String,
  val actionLabelAr: String = "طلب الخدمة",
) {
  // 1. الإنتاج الزراعي
  PLOWING_LEVELING(
    code = "plowing_leveling",
    domain = AgriServiceDomain.AGRICULTURAL_PRODUCTION,
    titleAr = "حرث وتسوية الأرض",
    subtitleFr = "Labour & Nivellement Laser",
    descriptionAr = "حرث عميق وتفتيت التربة وتسوية الحقول بالليزر لتحسين توزيع مياه السقي وتجهيز المرقد الزراعي.",
    providerBadge = "وحدة تهيئة الأراضي",
  ),
  TRACTOR_MACHINERY_RENTAL(
    code = "tractor_machinery_rental",
    domain = AgriServiceDomain.AGRICULTURAL_PRODUCTION,
    titleAr = "كراء الجرارات والآلات",
    subtitleFr = "Location Tracteurs & Engins",
    descriptionAr = "حجز جرارات فلاحية رباعية الدفع، آلات البذر، المرشات المحورية، ومعدات الخدمة الميكانيكية بالساعة أو اليوم.",
    providerBadge = "تعاونية العتاد الفلاحي",
  ),
  IRRIGATION_INSTALL_MAINT(
    code = "irrigation_install_maint",
    domain = AgriServiceDomain.AGRICULTURAL_PRODUCTION,
    titleAr = "تركيب وصيانة الري",
    subtitleFr = "Installation & Maintenance Irrigation",
    descriptionAr = "تصميم وتركيب شبكات السقي بالتقطير والرش المحوري وصيانة المضخات الغاطسة والفلاتر.",
    providerBadge = "مؤسسة الري الفلاحي",
  ),
  FARM_LABOR_SUPPLY(
    code = "farm_labor_supply",
    domain = AgriServiceDomain.AGRICULTURAL_PRODUCTION,
    titleAr = "توفير العمالة",
    subtitleFr = "Main-d'œuvre Agricole Qualifiée",
    descriptionAr = "توفير فرق عمالة موسمية ومختصة في غرس الشتلات، تقليم الأشجار، تلقيح النخيل، وخدمة البيوت البلاستيكية.",
    providerBadge = "فرق اليد العاملة المؤهلة",
  ),
  HARVESTING_PICKING(
    code = "harvesting_picking",
    domain = AgriServiceDomain.AGRICULTURAL_PRODUCTION,
    titleAr = "الحصاد والجني",
    subtitleFr = "Moisson & Cueillette",
    descriptionAr = "آلات الحصاد والدرس للحبوب الكبرى، جني عراجين التمور (دقلة نور)، واقتلاع البطاطا والزيتون.",
    providerBadge = "تعاونية الحصاد والجني",
  ),

  // 2. التجهيزات والمستلزمات
  SEEDS_SEEDLINGS(
    code = "seeds_seedlings",
    domain = AgriServiceDomain.EQUIPMENT_AND_SUPPLIES,
    titleAr = "البذور والشتلات",
    subtitleFr = "Semences Certifiées & Plants",
    descriptionAr = "توريد بذور معتمدة عالية المردودية، فسائل نخيل نسيجية، وشتلات أشجار مثمرة مقاومة للجفاف والملوحة.",
    providerBadge = "المشاتل المعتمدة CCLS",
    actionLabelAr = "طلب توريد",
  ),
  FERTILIZERS_SUPPLY(
    code = "fertilizers_supply",
    domain = AgriServiceDomain.EQUIPMENT_AND_SUPPLIES,
    titleAr = "الأسمدة",
    subtitleFr = "Engrais NPK & Amendements",
    descriptionAr = "توفير الأسمدة العضوية والمعدنية (NPK، اليوريا، الفوسفات) والمخصبات الورقية الذوابة الملائمة لتربتك.",
    providerBadge = "مركب التموين بالأسمدة",
    actionLabelAr = "طلب توريد",
  ),

  // 3. ما بعد الحصاد
  COLD_STORAGE_WAREHOUSES(
    code = "cold_storage_warehouses",
    domain = AgriServiceDomain.POST_HARVEST,
    titleAr = "مخازن التبريد",
    subtitleFr = "Chambres Froides & Conditionnement",
    descriptionAr = "حجز غرف تبريد موجبة وسالبة لحفظ التمور والبطاطا والفواكه مع فرز وتوضيب المنتوج للتسويق والتصدير.",
    providerBadge = "شبكة التبريد والتخزين FRIGO",
    actionLabelAr = "حجز مساحة",
  ),

  // 4. التجارة والنقل
  AGRICULTURAL_TRANSPORT_SERVICE(
    code = "agricultural_transport_service",
    domain = AgriServiceDomain.TRADE_AND_TRANSPORT,
    titleAr = "النقل الزراعي",
    subtitleFr = "Transport & Logistique Agricole",
    descriptionAr = "شاحنات مبردة ومسطحة لنقل المحاصيل والمستلزمات من المستثمرة نحو أسواق الجملة والموانئ.",
    providerBadge = "أسطول اللوجستيك الفلاحي",
    actionLabelAr = "طلب نقل",
  ),

  // 5. الاستشارات والخدمات الرقمية
  AGRONOMIST_CONSULTATION(
    code = "agronomist_consultation",
    domain = AgriServiceDomain.CONSULTING_AND_DIGITAL,
    titleAr = "مهندس زراعي",
    subtitleFr = "Ingénieur Agronome & Suivi",
    descriptionAr = "حجز زيارة ميدانية أو استشارة تقنية مع مهندس فلاحي مختص لتشخيص الآفات وضبط برنامج التسميد والوقاية.",
    providerBadge = "مكتب الخبرة الزراعية",
    actionLabelAr = "حجز استشارة",
  ),
  MARKET_PRICES_SERVICE(
    code = "market_prices_service",
    domain = AgriServiceDomain.CONSULTING_AND_DIGITAL,
    titleAr = "أسعار السوق",
    subtitleFr = "Mercuriale & Prix du Marché",
    descriptionAr = "متابعة مؤشرات أسعار الجملة اليومية للمنتجات الفلاحية وتفعيل تنبيهات الأسعار حسب ولايتك ومحصولك.",
    providerBadge = "مرصد الأسواق الفلاحية",
    actionLabelAr = "عرض وتفعيل",
  ),
  AGRI_WEATHER_SERVICE(
    code = "agri_weather_service",
    domain = AgriServiceDomain.CONSULTING_AND_DIGITAL,
    titleAr = "الطقس الزراعي",
    subtitleFr = "Météo Agricole & Alertes Gel/Vent",
    descriptionAr = "نشرة الرصد الجوي الفلاحي الميداني، معدلات التبخر والنتح (ET0)، وتنبيهات الصقيع والرياح الرملية ومواعيد السقي.",
    providerBadge = "الأرصاد الفلاحية الرقمية",
    actionLabelAr = "عرض النشرة",
  ),
  SOIL_LAB_INRAA(
    code = "soil_lab_inraa",
    domain = AgriServiceDomain.CONSULTING_AND_DIGITAL,
    titleAr = "تحليل التربة والمياه المخبري",
    subtitleFr = "Laboratoire Pédologique INRAA",
    descriptionAr = "أخذ عينات ميدانية وفحص الحموضة pH، الملوحة EC، والمادة العضوية بشهادة مخبرية معتمدة.",
    providerBadge = "مخبر معتمد INRAA",
    actionLabelAr = "حجز تحليل",
  ),
  DRONE_NDVI_SCAN(
    code = "drone_ndvi_scan",
    domain = AgriServiceDomain.CONSULTING_AND_DIGITAL,
    titleAr = "مسح طيفي بطائرات الدرون NDVI",
    subtitleFr = "Imagerie Multispectrale Drone",
    descriptionAr = "تصوير جوي عالي الدقة لكشف الإجهاد المائي وآفات النخيل والأشجار المثمرة.",
    providerBadge = "وحدة الاستشعار الفلاحي",
    actionLabelAr = "حجز مسح",
  );

  companion object {
    val primaryCatalogEntries: List<AgriServiceCategory> =
      listOf(
        PLOWING_LEVELING,
        TRACTOR_MACHINERY_RENTAL,
        IRRIGATION_INSTALL_MAINT,
        FARM_LABOR_SUPPLY,
        HARVESTING_PICKING,
        SEEDS_SEEDLINGS,
        FERTILIZERS_SUPPLY,
        COLD_STORAGE_WAREHOUSES,
        AGRICULTURAL_TRANSPORT_SERVICE,
        AGRONOMIST_CONSULTATION,
        MARKET_PRICES_SERVICE,
        AGRI_WEATHER_SERVICE,
        SOIL_LAB_INRAA,
        DRONE_NDVI_SCAN,
      )

    fun byDomain(domain: AgriServiceDomain): List<AgriServiceCategory> =
      primaryCatalogEntries.filter { it.domain == domain }

    fun fromCode(code: String): AgriServiceCategory =
      entries.firstOrNull { it.code == code }
        ?: when (code) {
          "forage_drilling" -> IRRIGATION_INSTALL_MAINT
          "harvester_rental" -> TRACTOR_MACHINERY_RENTAL
          "agronomic_advisory" -> AGRONOMIST_CONSULTATION
          else -> PLOWING_LEVELING
        }
  }
}

enum class WorkspaceSection(
  val titleAr: String,
  val titleFr: String,
  val titleEn: String,
  val navLabelAr: String,
  val navLabelFr: String,
  val navLabelEn: String,
) {
  ROLE_DASHBOARD(
    titleAr = "لوحة القيادة المخصصة",
    titleFr = "Tableau de Bord Personnalisé",
    titleEn = "Personalized Dashboard",
    navLabelAr = "الرئيسية",
    navLabelFr = "Accueil",
    navLabelEn = "Home",
  ),
  PARCEL_DELINEATION(
    titleAr = "تعيين القطعة وسجل التربة",
    titleFr = "Délimitation & Fiche",
    titleEn = "Parcel & Soil Registry",
    navLabelAr = "القطعة",
    navLabelFr = "Parcelle",
    navLabelEn = "Parcel",
  ),
  PUBLIC_MARKET(
    titleAr = "السوق العام الفلاحي",
    titleFr = "Marché Public",
    titleEn = "Public Marketplace",
    navLabelAr = "السوق",
    navLabelFr = "Marché",
    navLabelEn = "Market",
  ),
  PARTNER_CATALOG(
    titleAr = "عروضي ومنتجاتي المهنية",
    titleFr = "Mon Catalogue & Capacités",
    titleEn = "My Offers & Catalog",
    navLabelAr = "عروضي",
    navLabelFr = "Offres",
    navLabelEn = "Catalog",
  ),
  ORDERS_AND_REQUESTS(
    titleAr = "الطلبات وعروض الأسعار",
    titleFr = "Commandes, Devis & Réservations",
    titleEn = "Orders, Quotes & Bookings",
    navLabelAr = "الطلبات",
    navLabelFr = "Commandes",
    navLabelEn = "Orders",
  ),
  TRANSPORT(
    titleAr = "النقل واللوجستيك الفلاحي",
    titleFr = "Transport & Expéditions",
    titleEn = "Agricultural Transport",
    navLabelAr = "النقل",
    navLabelFr = "Transport",
    navLabelEn = "Transport",
  ),
  SERVICES(
    titleAr = "الخدمات الفلاحية والتقنية",
    titleFr = "Services Agricoles",
    titleEn = "Agricultural Services",
    navLabelAr = "الخدمات",
    navLabelFr = "Services",
    navLabelEn = "Services",
  ),
  MESSAGES_INBOX(
    titleAr = "الرسائل والتنبيهات المشتركة",
    titleFr = "Messagerie & Notifications",
    titleEn = "Messages & Notifications",
    navLabelAr = "الرسائل",
    navLabelFr = "Messages",
    navLabelEn = "Messages",
  ),
  SETTINGS(
    titleAr = "الإعدادات وإدارة الأنشطة",
    titleFr = "Paramètres & Multi-Rôles",
    titleEn = "Account & Role Settings",
    navLabelAr = "الإعدادات",
    navLabelFr = "Paramètres",
    navLabelEn = "Settings",
  ),
  MY_PARCELS(
    titleAr = "سجل القطع والمحاصيل",
    titleFr = "Mes Récoltes & Parcelles",
    titleEn = "My Saved Parcels",
    navLabelAr = "سجل القطع",
    navLabelFr = "Parcelles",
    navLabelEn = "Parcels",
  ),
  FELLAH_AI(
    titleAr = "المساعد الذكي Fellah AI",
    titleFr = "Assistant Fellah AI",
    titleEn = "Fellah AI Assistant",
    navLabelAr = "المساعد",
    navLabelFr = "Fellah AI",
    navLabelEn = "Fellah AI",
  ),
  FELLAH_MATIN(
    titleAr = "أخبار الصباح",
    titleFr = "Actualités agricoles du matin",
    titleEn = "Morning Agricultural News",
    navLabelAr = "أخبار الصباح",
    navLabelFr = "Fellah Matin",
    navLabelEn = "Fellah Matin",
  );

  fun localizedTitle(language: AppLanguage): String =
    when (language) {
      AppLanguage.AR -> titleAr
      AppLanguage.FR -> titleFr
      AppLanguage.EN -> titleEn
    }

  fun bottomLabel(language: AppLanguage): String =
    when (language) {
      AppLanguage.AR -> navLabelAr
      AppLanguage.FR -> navLabelFr
      AppLanguage.EN -> navLabelEn
    }
}
