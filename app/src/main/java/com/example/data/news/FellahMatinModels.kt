package com.example.data.news

import com.example.data.model.AppLanguage
import com.example.data.model.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * Canonical 10 agricultural news categories for **Fellah Matin (أخبار الصباح)**.
 */
enum class FellahNewsCategory(
  val code: String,
  val titleAr: String,
  val titleFr: String,
  val titleEn: String,
) {
  GOV_ANNOUNCEMENTS(
    code = "gov_announcements",
    titleAr = "الأخبار الفلاحية والبيانات الرسمية",
    titleFr = "Actualités agricoles & annonces officielles",
    titleEn = "Agricultural news & government announcements",
  ),
  CROP_PRODUCTION(
    code = "crop_production",
    titleAr = "الإنتاج النباتي والممارسات الزراعية",
    titleFr = "Production végétale & pratiques culturales",
    titleEn = "Crop production & agricultural practices",
  ),
  MARKET_AVAILABILITY(
    code = "market_availability",
    titleAr = "أسعار السوق وتوفر المنتجات",
    titleFr = "Prix du marché & disponibilité des produits",
    titleEn = "Market prices & product availability",
  ),
  WEATHER_ALERTS(
    code = "weather_alerts",
    titleAr = "الطقس والتنبيهات الفلاحية",
    titleFr = "Météo & alertes agrométéorologiques",
    titleEn = "Weather & agricultural alerts",
  ),
  INPUTS_AND_MACHINERY(
    code = "inputs_and_machinery",
    titleAr = "البذور والأسمدة والري والعتاد الفلاحي",
    titleFr = "Semences, engrais, irrigation & machinisme",
    titleEn = "Seeds, fertilizers, irrigation & machinery",
  ),
  TRANSPORT_LOGISTICS(
    code = "transport_logistics",
    titleAr = "النقل واللوجستيك الفلاحي",
    titleFr = "Transport & logistique agricole",
    titleEn = "Transportation & logistics",
  ),
  COLD_STORAGE_POSTHARVEST(
    code = "cold_storage_postharvest",
    titleAr = "التبريد والتوضيب وما بعد الحصاد",
    titleFr = "Froid, conditionnement & post-récolte",
    titleEn = "Cold storage, packaging & post-harvest",
  ),
  SERVICES_AND_TRAINING(
    code = "services_and_training",
    titleAr = "الخدمات الفلاحية والتكوين والإرشاد",
    titleFr = "Services agricoles, vulgarisation & formation",
    titleEn = "Agricultural services & training",
  ),
  RESEARCH_AND_TECH(
    code = "research_and_tech",
    titleAr = "البحث العلمي والتكنولوجيا والابتكار الفلاحي",
    titleFr = "Recherche, technologie & innovation agricole",
    titleEn = "Agricultural research, technology & innovation",
  ),
  EVENTS_AND_FAIRS(
    code = "events_and_fairs",
    titleAr = "التظاهرات والمعارض المحلية والوطنية",
    titleFr = "Salons & événements locaux et nationaux",
    titleEn = "Local & national agricultural events",
  );

  fun localizedTitle(language: AppLanguage): String =
    when (language) {
      AppLanguage.AR -> titleAr
      AppLanguage.FR -> titleFr
      AppLanguage.EN -> titleEn
    }

  companion object {
    val validCodes: Set<String> = entries.map { it.code }.toSet()

    fun fromCodeOrNull(code: String?): FellahNewsCategory? =
      entries.firstOrNull { it.code == code?.trim()?.lowercase() }

    fun fromCode(code: String?): FellahNewsCategory =
      fromCodeOrNull(code) ?: GOV_ANNOUNCEMENTS

    /**
     * Role-prioritized categories for each of the 6 professional roles.
     * Note: Personalization only prioritizes these categories in the "For Your Role" view
     * and NEVER hides or blocks access to general news in the shared feed.
     */
    fun priorityCategoriesForRole(role: UserRole): List<FellahNewsCategory> =
      when (role) {
        UserRole.FARMER ->
          listOf(
            CROP_PRODUCTION,
            WEATHER_ALERTS,
            INPUTS_AND_MACHINERY,
            GOV_ANNOUNCEMENTS,
            MARKET_AVAILABILITY,
          )
        UserRole.TRADER ->
          listOf(
            MARKET_AVAILABILITY,
            COLD_STORAGE_POSTHARVEST,
            TRANSPORT_LOGISTICS,
            GOV_ANNOUNCEMENTS,
            EVENTS_AND_FAIRS,
          )
        UserRole.TRANSPORTER ->
          listOf(
            TRANSPORT_LOGISTICS,
            WEATHER_ALERTS,
            MARKET_AVAILABILITY,
            COLD_STORAGE_POSTHARVEST,
            GOV_ANNOUNCEMENTS,
          )
        UserRole.SUPPLIER ->
          listOf(
            INPUTS_AND_MACHINERY,
            CROP_PRODUCTION,
            GOV_ANNOUNCEMENTS,
            RESEARCH_AND_TECH,
            EVENTS_AND_FAIRS,
          )
        UserRole.SERVICE_PROVIDER ->
          listOf(
            SERVICES_AND_TRAINING,
            RESEARCH_AND_TECH,
            INPUTS_AND_MACHINERY,
            CROP_PRODUCTION,
            WEATHER_ALERTS,
          )
        UserRole.STORAGE_PROVIDER ->
          listOf(
            COLD_STORAGE_POSTHARVEST,
            MARKET_AVAILABILITY,
            TRANSPORT_LOGISTICS,
            WEATHER_ALERTS,
            GOV_ANNOUNCEMENTS,
          )
      }
  }
}

enum class NewsSourceType(
  val code: String,
  val labelAr: String,
  val labelFr: String,
  val labelEn: String,
) {
  OFFICIAL_INSTITUTION(
    code = "official_institution",
    labelAr = "مؤسسة رسمية / عمومية",
    labelFr = "Institution officielle",
    labelEn = "Official institution",
  ),
  METEOROLOGICAL(
    code = "meteorological",
    labelAr = "أرصاد جوية فلاحية",
    labelFr = "Service agrométéorologique",
    labelEn = "Meteorological service",
  ),
  RESEARCH_UNIVERSITY(
    code = "research_university",
    labelAr = "معهد بحث وجامعة",
    labelFr = "Institut de recherche & université",
    labelEn = "Research institute & university",
  ),
  AGRI_PUBLICATION(
    code = "agri_publication",
    labelAr = "نشرية فلاحية متخصصة",
    labelFr = "Publication agricole autorisée",
    labelEn = "Permitted agricultural publication",
  ),
  MARKET_DATA(
    code = "market_data",
    labelAr = "بيانات سوق موثقة",
    labelFr = "Données de marché vérifiées",
    labelEn = "Verified market data",
  ),
  LOGISTICS_REGULATORY(
    code = "logistics_regulatory",
    labelAr = "نقل وتنظيم لوجستي",
    labelFr = "Transport & réglementation",
    labelEn = "Transport & regulatory source",
  );

  fun localizedLabel(language: AppLanguage): String =
    when (language) {
      AppLanguage.AR -> labelAr
      AppLanguage.FR -> labelFr
      AppLanguage.EN -> labelEn
    }

  companion object {
    fun fromCode(code: String?): NewsSourceType =
      entries.firstOrNull { it.code == code?.trim()?.lowercase() } ?: OFFICIAL_INSTITUTION
  }
}

enum class NewsVerificationStatus(val code: String) {
  VERIFIED_SOURCE("verified_source"),
  EDITORIAL_VERIFIED("editorial_verified"),
  UNVERIFIED("unverified");

  companion object {
    fun fromCode(code: String?): NewsVerificationStatus =
      entries.firstOrNull { it.code == code?.trim()?.lowercase() } ?: UNVERIFIED
  }
}

enum class NewsModerationStatus(val code: String) {
  PENDING_REVIEW("pending_review"),
  APPROVED("approved"),
  REJECTED("rejected");

  companion object {
    fun fromCode(code: String?): NewsModerationStatus =
      entries.firstOrNull { it.code == code?.trim()?.lowercase() } ?: PENDING_REVIEW
  }
}

enum class NewsFeedMode {
  SHARED_ALL,
  ROLE_PRIORITY,
}

/**
 * Firestore document model for `/editorial_admins/{adminId}`.
 */
@IgnoreExtraProperties
data class EditorialAdminRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val email: String = "",
  val roleLabel: String = "editor",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

/**
 * Firestore document model for `/news_articles/{articleId}`.
 */
@IgnoreExtraProperties
data class NewsArticleRecord(
  @DocumentId val id: String = "",
  val authorId: String = "",
  val title: String = "",
  val summary: String = "",
  val sourceName: String = "",
  val originalUrl: String = "",
  val publishedAtIso: String = "",
  val retrievedAt: Timestamp? = null,
  val category: String = FellahNewsCategory.GOV_ANNOUNCEMENTS.code,
  val language: String = "AR",
  val wilaya: String = "",
  val imageUrl: String = "",
  val verificationStatus: String = NewsVerificationStatus.VERIFIED_SOURCE.code,
  val moderationStatus: String = NewsModerationStatus.APPROVED.code,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

/**
 * Firestore document model for `/news_sources/{sourceId}`.
 */
@IgnoreExtraProperties
data class NewsSourceRecord(
  @DocumentId val id: String = "",
  val name: String = "",
  val baseUrl: String = "",
  val feedUrl: String = "",
  val sourceType: String = NewsSourceType.OFFICIAL_INSTITUTION.code,
  val language: String = "AR",
  @get:PropertyName("isEnabled")
  @set:PropertyName("isEnabled")
  var isEnabled: Boolean = true,
  val lastError: String = "",
  val lastCheckedAt: Timestamp? = null,
  val updatedBy: String = "",
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

/**
 * Firestore document model for `/user_news_preferences/{userId}`.
 */
@IgnoreExtraProperties
data class UserNewsPreferences(
  @DocumentId val id: String = "",
  val userId: String = "",
  val preferredCategories: List<String> = emptyList(),
  val preferredRegion: String = "",
  val preferredLanguage: String = "ALL",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

/**
 * Unified, UI-ready article representation used across Visitor Mode (Room + RSS) and
 * Authenticated Dashboards (Firestore + Room + RSS).
 *
 * Explicitly separates:
 * - [publishedAtIso] / [publishedAtEpochMs]: Original publication timestamp from source.
 * - [retrievedAtEpochMs]: Timestamp when Fellah Numérique retrieved or verified the item.
 * - [isDemonstrationReference]: True ONLY for clearly badged institutional reference cards
 *   when live RSS/editorial sources are unreachable (never presented as breaking news).
 */
data class FellahMatinArticle(
  val id: String,
  val title: String,
  val summary: String,
  val sourceName: String,
  val originalUrl: String,
  val publishedAtIso: String,
  val publishedAtEpochMs: Long,
  val retrievedAtEpochMs: Long,
  val category: FellahNewsCategory,
  val language: String,
  val wilaya: String,
  val imageUrl: String = "",
  val verificationStatus: NewsVerificationStatus = NewsVerificationStatus.VERIFIED_SOURCE,
  val moderationStatus: NewsModerationStatus = NewsModerationStatus.APPROVED,
  val isDemonstrationReference: Boolean = false,
)

/**
 * Query parameters for `/api/v1/news` equivalent pagination & filtering.
 */
data class NewsFeedPageRequest(
  val page: Int = 1,
  val pageSize: Int = 6,
  val feedMode: NewsFeedMode = NewsFeedMode.SHARED_ALL,
  val categoryCode: String? = null,
  val languageFilter: String = "ALL", // "ALL", "AR", "FR", "EN"
  val wilayaFilter: String = "",
  val activeRole: UserRole? = null,
  val userPreferredCategories: List<String> = emptyList(),
)

/**
 * Paginated feed response for `/api/v1/news` equivalent.
 */
data class NewsFeedPageResult(
  val items: List<FellahMatinArticle>,
  val page: Int,
  val pageSize: Int,
  val totalItems: Int,
  val totalPages: Int,
  val hasNextPage: Boolean,
  val isUsingDemonstrationFallback: Boolean,
  val availableWilayas: List<String>,
)

data class BriefingHighlightItem(
  val articleId: String,
  val headline: String,
  val sourceName: String,
  val originalUrl: String,
  val publishedAtIso: String,
)

/**
 * Daily Morning Briefing response (`/api/v1/news/briefing/today` equivalent).
 * Clearly distinguishes cached AI-generated summaries from deterministic verified-article digests.
 */
data class MorningBriefingSummary(
  val dayKey: String,
  val language: AppLanguage,
  val summaryText: String,
  val sourceAttributions: List<BriefingHighlightItem>,
  val isAiGenerated: Boolean,
  val isFromCache: Boolean,
  val generatedAtEpochMs: Long,
  val fallbackReason: String = "",
)

/**
 * Localized headings and labels for **Fellah Matin**.
 */
object FellahMatinStrings {
  fun sectionHeading(language: AppLanguage): String =
    when (language) {
      AppLanguage.AR -> "أخبار الصباح"
      AppLanguage.FR -> "Actualités agricoles du matin"
      AppLanguage.EN -> "Morning Agricultural News"
    }

  fun brandSubtitle(language: AppLanguage): String =
    when (language) {
      AppLanguage.AR -> "Fellah Matin • النشرة الفلاحية اليومية الموثقة"
      AppLanguage.FR -> "Fellah Matin • Flux quotidien d'information agricole vérifiée"
      AppLanguage.EN -> "Fellah Matin • Verified Daily Agricultural News Feed"
    }

  fun viewAllAction(language: AppLanguage): String =
    when (language) {
      AppLanguage.AR -> "عرض كل الأخبار"
      AppLanguage.FR -> "Voir toutes les actualités"
      AppLanguage.EN -> "View all news"
    }
}
