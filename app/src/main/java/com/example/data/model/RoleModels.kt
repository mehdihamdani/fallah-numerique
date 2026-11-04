package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * Six canonical professional activities supported on Fellah Numérique.
 */
enum class UserRole(
  val code: String,
  val titleAr: String,
  val titleFr: String,
  val titleEn: String,
  val shortDescAr: String,
  val shortDescFr: String,
  val shortDescEn: String,
  val businessFieldLabelAr: String,
  val businessFieldLabelFr: String,
  val businessFieldLabelEn: String,
  val activityFieldLabelAr: String,
  val activityFieldLabelFr: String,
  val activityFieldLabelEn: String,
  val capacityFieldLabelAr: String,
  val capacityFieldLabelFr: String,
  val capacityFieldLabelEn: String,
) {
  FARMER(
    code = "farmer",
    titleAr = "الفلاح",
    titleFr = "Agriculteur",
    titleEn = "Farmer",
    shortDescAr = "إدارة المستثمرة والقطع الفلاحية، تربية النحل وإنتاج العسل، تربية المائيات والأسماك، تربية المواشي والأبقار، نشر السلع والطلب.",
    shortDescFr = "Gérer vos exploitations (cultures, apiculture, aquaculture, élevage ovin/bovin), publier vos produits et demander du matériel.",
    shortDescEn = "Manage farms, parcels, apiculture, fish farming & livestock, publish commodities, and request services.",
    businessFieldLabelAr = "اسم المستثمرة أو المنشأة الفلاحية / التعاونية",
    businessFieldLabelFr = "Nom de l'exploitation, élevage ou coopérative",
    businessFieldLabelEn = "Farm, ranch, apiary, or cooperative name",
    activityFieldLabelAr = "الشعبة (نباتي، عسل ونحل، سمك ومائيات، مواشي وأبقار)",
    activityFieldLabelFr = "Filière (Végétale, Apiculture, Aquaculture, Élevage)",
    activityFieldLabelEn = "Sector (Crops, Apiculture, Aquaculture, Livestock)",
    capacityFieldLabelAr = "المساحة الإجمالية التقريبية بالهكتار (اختياري)",
    capacityFieldLabelFr = "Superficie approximative en hectares (optionnel)",
    capacityFieldLabelEn = "Approximate farm size in hectares (optional)",
  ),
  TRADER(
    code = "trader",
    titleAr = "تاجر الجملة والتجزئة",
    titleFr = "Commerçant Gros & Détail",
    titleEn = "Wholesale & Retail Trader",
    shortDescAr = "تصفح ومقارنة المحاصيل الفلاحية، إنشاء طلبات الشراء، والتعاقد المباشر مع الفلاحين والناقلين.",
    shortDescFr = "Parcourir les récoltes, comparer les offres, créer des demandes d'achat et organiser la livraison.",
    shortDescEn = "Browse harvests, compare seller offers, create purchase orders, and coordinate transport.",
    businessFieldLabelAr = "اسم المؤسسة التجارية أو المسمكة/المربع",
    businessFieldLabelFr = "Nom de l'établissement commercial / mandataire",
    businessFieldLabelEn = "Trading business or market stall name",
    activityFieldLabelAr = "نوع النشاط التجاري (جملة، نصف جملة، تجزئة، تصدير)",
    activityFieldLabelFr = "Type d'activité (Gros, Demi-gros, Détail, Export)",
    activityFieldLabelEn = "Trading type (Wholesale, Semi-wholesale, Retail, Export)",
    capacityFieldLabelAr = "سوق الجملة أو نطاق التوزيع الرئيسي",
    capacityFieldLabelFr = "Marché de gros ou zone de distribution",
    capacityFieldLabelEn = "Primary wholesale market or distribution area",
  ),
  TRANSPORTER(
    code = "transporter",
    titleAr = "الناقل",
    titleFr = "Transporteur Agricole",
    titleEn = "Transporter",
    shortDescAr = "تسجيل الشاحنات المبردة والمسطحة، نشر خطوط النقل، وتقديم عروض الأسعار وتتبع الشحنات.",
    shortDescFr = "Enregistrer vos camions frigorifiques/plateaux, publier vos trajets et répondre aux demandes.",
    shortDescEn = "Register refrigerated/flatbed trucks, publish routes, submit freight quotes, and track jobs.",
    businessFieldLabelAr = "اسم شركة النقل أو الناقل المستقل",
    businessFieldLabelFr = "Nom de la société de transport ou transporteur",
    businessFieldLabelEn = "Transport company or independent carrier name",
    activityFieldLabelAr = "نوع المركبات (شاحنة مبردة، مسطحة، صهريج، حبوب)",
    activityFieldLabelFr = "Types de véhicules (Frigorifique, Plateau, Benne)",
    activityFieldLabelEn = "Vehicle types (Refrigerated, Flatbed, Grain hopper)",
    capacityFieldLabelAr = "حمولة الأسطول والولايات المغطاة (مثال: 20 طن - وطني)",
    capacityFieldLabelFr = "Capacité en tonnes & wilayas desservies",
    capacityFieldLabelEn = "Fleet capacity (tons) & operating wilayas",
  ),
  SUPPLIER(
    code = "supplier",
    titleAr = "مورد المستلزمات الزراعية",
    titleFr = "Fournisseur d'Intrants Agricoles",
    titleEn = "Agricultural Supplier",
    shortDescAr = "عرض البذور المعتمدة، الشتلات، الأسمدة، ومعدات الري بالتقطير وإدارة المخزون والطلبيات.",
    shortDescFr = "Publier semences, plants, engrais et équipements d'irrigation, gérer les stocks et commandes.",
    shortDescEn = "Publish seeds, seedlings, fertilizers, and irrigation gear; manage stock and customer orders.",
    businessFieldLabelAr = "اسم مؤسسة التموين أو المشتلة المعتمدة",
    businessFieldLabelFr = "Nom du fournisseur ou pépinière agréée",
    businessFieldLabelEn = "Agricultural supply business or nursery name",
    activityFieldLabelAr = "فئات المنتجات (بذور، شتلات، أسمدة NPK، عتاد السقي)",
    activityFieldLabelFr = "Catégories d'intrants (Semences, Engrais, Irrigation)",
    activityFieldLabelEn = "Product categories (Seeds, Fertilizers, Irrigation)",
    capacityFieldLabelAr = "نطاق التوريد والولايات المخدومة",
    capacityFieldLabelFr = "Zone de livraison et wilayas couvertes",
    capacityFieldLabelEn = "Delivery coverage & served wilayas",
  ),
  SERVICE_PROVIDER(
    code = "service_provider",
    titleAr = "مقدم الخدمات الزراعية",
    titleFr = "Prestataire de Services Agricoles",
    titleEn = "Agricultural Service Provider",
    shortDescAr = "تقديم خدمات الحرث، الحصاد، تركيب الري، تحاليل التربة، والاستشارات الهندسية الزراعية.",
    shortDescFr = "Proposer labour, moisson, irrigation, analyses de sol et conseil agronomique sur le terrain.",
    shortDescEn = "Offer plowing, harvesting, irrigation installation, soil analysis, and agronomic consulting.",
    businessFieldLabelAr = "اسم المكتب الهندسي أو تعاونية الخدمات",
    businessFieldLabelFr = "Nom du bureau agronomique ou coopérative de services",
    businessFieldLabelEn = "Service cooperative or agronomy practice name",
    activityFieldLabelAr = "الخدمات المقدمة (حرث، كراء آلات، ري، مهندس زراعي...)",
    activityFieldLabelFr = "Services proposés (Labour, Moisson, Irrigation, Conseil)",
    activityFieldLabelEn = "Offered services (Plowing, Machinery, Irrigation, Consulting)",
    capacityFieldLabelAr = "نطاق التدخل الميداني والتسعير المرجعي",
    capacityFieldLabelFr = "Zone d'intervention & tarification indicative",
    capacityFieldLabelEn = "Service coverage area & indicative availability",
  ),
  STORAGE_PROVIDER(
    code = "storage_provider",
    titleAr = "التخزين والتبريد",
    titleFr = "Stockage & Chambres Froides",
    titleEn = "Storage & Post-Harvest Services",
    shortDescAr = "تسجيل غرف التبريد والمستودعات، نشر السعة المتاحة بالطن، وإدارة حجوزات الحفظ والفرز والتوضيب.",
    shortDescFr = "Enregistrer entrepôts et chambres froides, publier la capacité disponible et gérer les réservations.",
    shortDescEn = "Register warehouses & cold-storage rooms, publish available tonnage, and manage reservations.",
    businessFieldLabelAr = "اسم مركب التبريد أو المستودع الفلاحي",
    businessFieldLabelFr = "Nom de l'entrepôt ou complexe frigorifique",
    businessFieldLabelEn = "Cold-storage complex or warehouse name",
    activityFieldLabelAr = "نوع التخزين (تبريد موجب +4°C، سالب، جاف، فرز وتوضيب)",
    activityFieldLabelFr = "Type de stockage (Froid positif/négatif, Sec, Conditionnement)",
    activityFieldLabelEn = "Facility type (Positive/negative cold room, Dry, Packaging)",
    capacityFieldLabelAr = "السعة التخزينية المتاحة بالطن (مثال: 350 طن)",
    capacityFieldLabelFr = "Capacité de stockage disponible en tonnes",
    capacityFieldLabelEn = "Available storage capacity in metric tons",
  );

  fun localizedTitle(language: AppLanguage): String =
    when (language) {
      AppLanguage.AR -> titleAr
      AppLanguage.FR -> titleFr
      AppLanguage.EN -> titleEn
    }

  fun localizedDescription(language: AppLanguage): String =
    when (language) {
      AppLanguage.AR -> shortDescAr
      AppLanguage.FR -> shortDescFr
      AppLanguage.EN -> shortDescEn
    }

  fun businessFieldLabel(language: AppLanguage): String =
    when (language) {
      AppLanguage.AR -> businessFieldLabelAr
      AppLanguage.FR -> businessFieldLabelFr
      AppLanguage.EN -> businessFieldLabelEn
    }

  fun activityFieldLabel(language: AppLanguage): String =
    when (language) {
      AppLanguage.AR -> activityFieldLabelAr
      AppLanguage.FR -> activityFieldLabelFr
      AppLanguage.EN -> activityFieldLabelEn
    }

  fun capacityFieldLabel(language: AppLanguage): String =
    when (language) {
      AppLanguage.AR -> capacityFieldLabelAr
      AppLanguage.FR -> capacityFieldLabelFr
      AppLanguage.EN -> capacityFieldLabelEn
    }

  companion object {
    val validCodes: Set<String> = entries.map { it.code }.toSet()

    fun fromCodeOrNull(code: String?): UserRole? =
      entries.firstOrNull { it.code == code?.trim()?.lowercase() }

    fun fromCode(code: String?): UserRole = fromCodeOrNull(code) ?: FARMER

    /**
     * Deduplicates and validates role codes while preserving user selection order.
     */
    fun normalizeRoleCodes(codes: Iterable<String>): List<String> {
      val seen = LinkedHashSet<String>()
      for (raw in codes) {
        val role = fromCodeOrNull(raw) ?: continue
        seen.add(role.code)
      }
      return seen.toList()
    }
  }
}

/**
 * Centralized role-to-navigation configuration for both mobile bottom navigation
 * and expanded/drawer sidebar navigation.
 */
object RoleNavigationConfig {

  fun bottomBarSectionsForRole(role: UserRole): List<WorkspaceSection> =
    when (role) {
      UserRole.FARMER ->
        listOf(
          WorkspaceSection.ROLE_DASHBOARD,
          WorkspaceSection.PARCEL_DELINEATION,
          WorkspaceSection.PUBLIC_MARKET,
          WorkspaceSection.SERVICES,
          WorkspaceSection.SETTINGS,
        )
      UserRole.TRADER ->
        listOf(
          WorkspaceSection.ROLE_DASHBOARD,
          WorkspaceSection.PUBLIC_MARKET,
          WorkspaceSection.ORDERS_AND_REQUESTS,
          WorkspaceSection.TRANSPORT,
          WorkspaceSection.SETTINGS,
        )
      UserRole.TRANSPORTER ->
        listOf(
          WorkspaceSection.ROLE_DASHBOARD,
          WorkspaceSection.PARTNER_CATALOG,
          WorkspaceSection.ORDERS_AND_REQUESTS,
          WorkspaceSection.MESSAGES_INBOX,
          WorkspaceSection.SETTINGS,
        )
      UserRole.SUPPLIER ->
        listOf(
          WorkspaceSection.ROLE_DASHBOARD,
          WorkspaceSection.PARTNER_CATALOG,
          WorkspaceSection.ORDERS_AND_REQUESTS,
          WorkspaceSection.PUBLIC_MARKET,
          WorkspaceSection.SETTINGS,
        )
      UserRole.SERVICE_PROVIDER ->
        listOf(
          WorkspaceSection.ROLE_DASHBOARD,
          WorkspaceSection.PARTNER_CATALOG,
          WorkspaceSection.ORDERS_AND_REQUESTS,
          WorkspaceSection.SERVICES,
          WorkspaceSection.SETTINGS,
        )
      UserRole.STORAGE_PROVIDER ->
        listOf(
          WorkspaceSection.ROLE_DASHBOARD,
          WorkspaceSection.PARTNER_CATALOG,
          WorkspaceSection.ORDERS_AND_REQUESTS,
          WorkspaceSection.PUBLIC_MARKET,
          WorkspaceSection.SETTINGS,
        )
    }

  fun sidebarSectionsForRole(role: UserRole): List<WorkspaceSection> =
    when (role) {
      UserRole.FARMER ->
        listOf(
          WorkspaceSection.ROLE_DASHBOARD,
          WorkspaceSection.FELLAH_MATIN,
          WorkspaceSection.FELLAH_AI,
          WorkspaceSection.PARCEL_DELINEATION,
          WorkspaceSection.MY_PARCELS,
          WorkspaceSection.PUBLIC_MARKET,
          WorkspaceSection.TRANSPORT,
          WorkspaceSection.SERVICES,
          WorkspaceSection.ORDERS_AND_REQUESTS,
          WorkspaceSection.MESSAGES_INBOX,
          WorkspaceSection.SETTINGS,
        )
      UserRole.TRADER ->
        listOf(
          WorkspaceSection.ROLE_DASHBOARD,
          WorkspaceSection.FELLAH_MATIN,
          WorkspaceSection.FELLAH_AI,
          WorkspaceSection.PUBLIC_MARKET,
          WorkspaceSection.ORDERS_AND_REQUESTS,
          WorkspaceSection.TRANSPORT,
          WorkspaceSection.SERVICES,
          WorkspaceSection.MESSAGES_INBOX,
          WorkspaceSection.SETTINGS,
        )
      UserRole.TRANSPORTER ->
        listOf(
          WorkspaceSection.ROLE_DASHBOARD,
          WorkspaceSection.FELLAH_MATIN,
          WorkspaceSection.FELLAH_AI,
          WorkspaceSection.PARTNER_CATALOG,
          WorkspaceSection.ORDERS_AND_REQUESTS,
          WorkspaceSection.PUBLIC_MARKET,
          WorkspaceSection.MESSAGES_INBOX,
          WorkspaceSection.SETTINGS,
        )
      UserRole.SUPPLIER ->
        listOf(
          WorkspaceSection.ROLE_DASHBOARD,
          WorkspaceSection.FELLAH_MATIN,
          WorkspaceSection.FELLAH_AI,
          WorkspaceSection.PARTNER_CATALOG,
          WorkspaceSection.ORDERS_AND_REQUESTS,
          WorkspaceSection.PUBLIC_MARKET,
          WorkspaceSection.MESSAGES_INBOX,
          WorkspaceSection.SETTINGS,
        )
      UserRole.SERVICE_PROVIDER ->
        listOf(
          WorkspaceSection.ROLE_DASHBOARD,
          WorkspaceSection.FELLAH_MATIN,
          WorkspaceSection.FELLAH_AI,
          WorkspaceSection.PARTNER_CATALOG,
          WorkspaceSection.ORDERS_AND_REQUESTS,
          WorkspaceSection.SERVICES,
          WorkspaceSection.MESSAGES_INBOX,
          WorkspaceSection.SETTINGS,
        )
      UserRole.STORAGE_PROVIDER ->
        listOf(
          WorkspaceSection.ROLE_DASHBOARD,
          WorkspaceSection.FELLAH_MATIN,
          WorkspaceSection.FELLAH_AI,
          WorkspaceSection.PARTNER_CATALOG,
          WorkspaceSection.ORDERS_AND_REQUESTS,
          WorkspaceSection.PUBLIC_MARKET,
          WorkspaceSection.MESSAGES_INBOX,
          WorkspaceSection.SETTINGS,
        )
    }
}

@IgnoreExtraProperties
data class UserAccountProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val preferredLanguage: String = "AR",
  val assignedRoles: List<String> = emptyList(),
  val defaultRole: String = "",
  val onboardingCompleted: Boolean = false,
  val onboardingStep: Int = 1,
  val nationalIdScanned: Boolean = false,
  val nationalIdNumber: String = "",
  val nationalIdHolderName: String = "",
  val farmerCardScanned: Boolean = false,
  val farmerCardNumber: String = "",
  val farmerCardWilaya: String = "",
  val phoneNumber: String = "",
  val phoneVerified: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun parsedAssignedRoles(): List<UserRole> =
    assignedRoles.mapNotNull { UserRole.fromCodeOrNull(it) }.distinct()

  fun resolvedActiveRole(): UserRole? =
    UserRole.fromCodeOrNull(defaultRole)?.takeIf { role ->
      assignedRoles.contains(role.code)
    } ?: parsedAssignedRoles().firstOrNull()

  fun toIdentityVerificationData(): IdentityVerificationData =
    IdentityVerificationData(
      nationalIdScanned = nationalIdScanned,
      nationalIdNumber = nationalIdNumber,
      nationalIdHolderName = nationalIdHolderName,
      farmerCardScanned = farmerCardScanned,
      farmerCardNumber = farmerCardNumber,
      farmerCardWilaya = farmerCardWilaya,
    )
}

object AccountInputValidator {
  private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

  /**
   * Normalizes Algerian mobile/landline numbers (`05...`, `06...`, `07...`, `213...`, `00213...`)
   * or international `+...` inputs to clean E.164 format (`+213550123456`).
   * Returns `null` if the phone number does not have valid digit count (8..15 digits).
   */
  fun normalizePhoneToE164(rawPhone: String, defaultDialCode: String = "+213"): String? {
    val cleaned = rawPhone.trim().replace(Regex("[\\s\\-().]"), "")
    if (cleaned.isEmpty()) return null

    val e164 =
      when {
        cleaned.startsWith("+") -> "+" + cleaned.drop(1).filter { it.isDigit() }
        cleaned.startsWith("00") -> "+" + cleaned.drop(2).filter { it.isDigit() }
        cleaned.startsWith("213") && cleaned.length >= 11 -> "+" + cleaned.filter { it.isDigit() }
        cleaned.startsWith("0") && cleaned.length in 9..11 ->
          defaultDialCode + cleaned.drop(1).filter { it.isDigit() }
        cleaned.all { it.isDigit() } && cleaned.length == 9 && cleaned.first() in listOf('5', '6', '7') ->
          defaultDialCode + cleaned
        else -> return null
      }

    val digitsOnly = e164.drop(1)
    if (digitsOnly.length !in 8..15 || !digitsOnly.all { it.isDigit() }) {
      return null
    }
    return e164.take(32)
  }

  fun isValidEmail(rawEmail: String): Boolean {
    val trimmed = rawEmail.trim()
    return trimmed.length in 5..160 && EMAIL_REGEX.matches(trimmed)
  }
}

/**
 * Owner-isolated identity & optional Chamber of Agriculture Farmer Card verification metadata.
 */
data class IdentityVerificationData(
  val nationalIdScanned: Boolean = false,
  val nationalIdNumber: String = "",
  val nationalIdHolderName: String = "",
  val farmerCardScanned: Boolean = false,
  val farmerCardNumber: String = "",
  val farmerCardWilaya: String = "",
  val detectedFarmName: String = "",
  val detectedSector: String = "",
) {
  val hasNationalId: Boolean
    get() = nationalIdScanned || nationalIdNumber.isNotBlank()

  val hasFarmerCard: Boolean
    get() = farmerCardScanned || farmerCardNumber.isNotBlank()
}

@IgnoreExtraProperties
data class RoleSpecificProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val roleCode: String = "farmer",
  val businessName: String = "",
  val wilaya: String = "",
  val commune: String = "",
  val activityType: String = "",
  val capacityOrArea: String = "",
  val qualificationsOrDetails: String = "",
  @get:PropertyName("isProfileComplete")
  @set:PropertyName("isProfileComplete")
  var isProfileComplete: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PartnerCatalogItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val providerRole: String = "supplier",
  val providerName: String = "",
  val title: String = "",
  val category: String = "",
  val wilaya: String = "07 - بسكرة (Biskra)",
  val priceDzd: Double = 0.0,
  val unitLabel: String = "د.ج / وحدة",
  val availableQuantityOrCapacity: Double = 1.0,
  val description: String = "",
  val status: String = "active",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MarketplaceOrder(
  @DocumentId val id: String = "",
  val requesterId: String = "",
  val requesterName: String = "",
  val requesterRole: String = "trader",
  val providerId: String = "",
  val targetRole: String = "farmer",
  val targetItemId: String = "",
  val orderType: String = "purchase_request",
  val itemTitle: String = "",
  val wilaya: String = "07 - بسكرة (Biskra)",
  val quantity: Double = 1.0,
  val unitLabel: String = "طن",
  val proposedPriceDzd: Double = 0.0,
  val notes: String = "",
  val status: String = "pending",
  val parcelId: String = "",
  val commune: String = "",
  val serviceCategory: String = "",
  val preferredDate: String = "",
  val centroidLat: Double? = null,
  val centroidLng: Double? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PlatformMessage(
  @DocumentId val id: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val senderRole: String = "farmer",
  val recipientId: String = "",
  val recipientName: String = "",
  val subject: String = "",
  val body: String = "",
  @get:PropertyName("isRead")
  @set:PropertyName("isRead")
  var isRead: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class FavoriteTargetType(
  val code: String,
  val labelAr: String,
  val labelFr: String,
  val labelEn: String,
) {
  OPERATOR("operator", "مستخدم / متعامل", "Opérateur", "Operator"),
  MARKET_LISTING("market_listing", "محصول فلاحي", "Récolte", "Harvest Listing"),
  PARTNER_ITEM("partner_item", "عرض شريك / خدمة", "Offre Partenaire", "Partner Offer");

  fun localizedLabel(language: AppLanguage): String =
    when (language) {
      AppLanguage.AR -> labelAr
      AppLanguage.FR -> labelFr
      AppLanguage.EN -> labelEn
    }

  companion object {
    fun fromCode(code: String?): FavoriteTargetType =
      entries.firstOrNull { it.code == code?.trim()?.lowercase() } ?: OPERATOR
  }
}

@IgnoreExtraProperties
data class UserRatingRecord(
  @DocumentId val id: String = "",
  val reviewerId: String = "",
  val reviewerName: String = "",
  val reviewerRole: String = "farmer",
  val targetUserId: String = "",
  val targetName: String = "",
  val targetRole: String = "farmer",
  val stars: Int = 5,
  val pointsAwarded: Int = 100,
  val comment: String = "",
  val contextItemId: String = "",
  val status: String = "published",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

/**
 * Aggregated star rating (1.0..5.0) and reputation points (0..100) for an operator.
 */
data class UserReputationSummary(
  val targetUserId: String,
  val averageStars: Double = 0.0,
  val totalRatings: Int = 0,
  val trustPointsOutOf100: Int = 0,
  val totalPointsAccumulated: Int = 0,
  val recentReviews: List<UserRatingRecord> = emptyList(),
) {
  val hasRatings: Boolean
    get() = totalRatings > 0

  fun formattedAverageStars(): String =
    if (totalRatings == 0) "—" else String.format(java.util.Locale.US, "%.1f", averageStars)

  fun tierLabel(language: AppLanguage): String =
    when {
      totalRatings == 0 ->
        when (language) {
          AppLanguage.AR -> "متعامل جديد"
          AppLanguage.FR -> "Nouvel opérateur"
          AppLanguage.EN -> "New operator"
        }
      trustPointsOutOf100 >= 90 ->
        when (language) {
          AppLanguage.AR -> "نخبة موثوقة"
          AppLanguage.FR -> "Élite vérifiée"
          AppLanguage.EN -> "Top Rated"
        }
      trustPointsOutOf100 >= 75 ->
        when (language) {
          AppLanguage.AR -> "موثوق جداً"
          AppLanguage.FR -> "Très fiable"
          AppLanguage.EN -> "Trusted"
        }
      else ->
        when (language) {
          AppLanguage.AR -> "متعامل نشط"
          AppLanguage.FR -> "Opérateur actif"
          AppLanguage.EN -> "Active"
        }
    }

  companion object {
    fun fromRatings(targetUserId: String, ratings: List<UserRatingRecord>): UserReputationSummary {
      val matching =
        ratings
          .filter { it.targetUserId == targetUserId && it.status == "published" && it.stars in 1..5 }
          .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      if (matching.isEmpty()) {
        return UserReputationSummary(targetUserId = targetUserId)
      }
      val avgStars = matching.map { it.stars }.average()
      val avgPoints = kotlin.math.round(avgStars * 20.0).toInt().coerceIn(20, 100)
      val accumulated = matching.sumOf { it.pointsAwarded.coerceAtLeast(it.stars * 20) }
      return UserReputationSummary(
        targetUserId = targetUserId,
        averageStars = avgStars,
        totalRatings = matching.size,
        trustPointsOutOf100 = avgPoints,
        totalPointsAccumulated = accumulated,
        recentReviews = matching.take(10),
      )
    }

    fun buildReputationMap(ratings: List<UserRatingRecord>): Map<String, UserReputationSummary> {
      return ratings
        .filter { it.status == "published" && it.targetUserId.isNotBlank() && it.stars in 1..5 }
        .groupBy { it.targetUserId }
        .mapValues { (uid, list) -> fromRatings(uid, list) }
    }
  }
}

@IgnoreExtraProperties
data class UserFavoriteRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val favoriteType: String = FavoriteTargetType.OPERATOR.code,
  val targetId: String = "",
  val targetOwnerId: String = "",
  val targetTitle: String = "",
  val targetSubtitle: String = "",
  val targetRole: String = "farmer",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  companion object {
    fun sanitizeKeySegment(raw: String): String =
      raw.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(56)

    fun compositeDocumentId(userId: String, favoriteType: FavoriteTargetType, targetId: String): String {
      val safeUid = sanitizeKeySegment(userId)
      val safeTarget = sanitizeKeySegment(targetId)
      return "${safeUid}_${favoriteType.code}_$safeTarget"
    }
  }
}

