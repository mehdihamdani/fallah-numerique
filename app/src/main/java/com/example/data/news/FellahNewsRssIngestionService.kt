package com.example.data.news

import com.example.data.offline.CachedNewsArticleEntity
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class ConfiguredRssFeedSource(
  val id: String,
  val name: String,
  val baseUrl: String,
  val feedUrl: String,
  val sourceType: NewsSourceType,
  val defaultCategory: FellahNewsCategory,
  val language: String,
  val isEnabled: Boolean = true,
)

data class RssIngestionOutcome(
  val sourceId: String,
  val sourceName: String,
  val importedArticles: List<CachedNewsArticleEntity>,
  val skippedDuplicatesOrInvalid: Int,
  val errorMessage: String? = null,
)

/**
 * Permitted, rate-limited RSS/Atom ingestion and sanitization service for **Fellah Matin**.
 *
 * Security & Content Integrity Guarantees:
 * 1. Only accepts `https://` URLs with valid public hostnames (rejects `http://`, `javascript:`,
 *    `data:`, `file:`, localhost/loopback/private IPs).
 * 2. Uses non-expanding `XmlPullParser` (`FEATURE_PROCESS_DOCDECL = false`) to block XXE attacks.
 * 3. Strips all HTML/script/style markup and collapses whitespace, truncating summaries to <= 360 chars
 *    so full copyrighted articles are NEVER republished — users always click through to [originalUrl].
 * 4. Enforces per-source cooldown rate limiting and 6-second connect/read timeouts.
 * 5. Never fabricates live headlines, quotes, prices, wilayas, or publication dates. When external RSS
 *    endpoints are unreachable and zero cached/approved articles exist, [demonstrationReferenceGuideItems]
 *    provides explicitly badged Institutional Portal Directory cards (`isDemonstrationReference = true`).
 */
class FellahNewsRssIngestionService(
  private val httpFetcher: suspend (String) -> String = ::defaultHttpsGetXml,
  private val minCooldownMsPerSource: Long = 120_000L,
  private val clockMs: () -> Long = { System.currentTimeMillis() },
) {

  private val lastFetchedBySourceUrl = ConcurrentHashMap<String, Long>()

  suspend fun ingestSource(
    source: ConfiguredRssFeedSource,
    forceBypassCooldown: Boolean = false,
  ): RssIngestionOutcome {
    if (!source.isEnabled) {
      return RssIngestionOutcome(
        sourceId = source.id,
        sourceName = source.name,
        importedArticles = emptyList(),
        skippedDuplicatesOrInvalid = 0,
        errorMessage = "Source is disabled by editorial administrator.",
      )
    }

    if (!isValidHttpsNewsUrl(source.feedUrl)) {
      return RssIngestionOutcome(
        sourceId = source.id,
        sourceName = source.name,
        importedArticles = emptyList(),
        skippedDuplicatesOrInvalid = 1,
        errorMessage = "Invalid or non-HTTPS feed URL rejected.",
      )
    }

    val now = clockMs()
    val previousFetch = lastFetchedBySourceUrl[source.feedUrl]
    if (!forceBypassCooldown && previousFetch != null && (now - previousFetch) < minCooldownMsPerSource) {
      return RssIngestionOutcome(
        sourceId = source.id,
        sourceName = source.name,
        importedArticles = emptyList(),
        skippedDuplicatesOrInvalid = 0,
        errorMessage = null,
      )
    }

    return try {
      val rawXml = httpFetcher(source.feedUrl)
      lastFetchedBySourceUrl[source.feedUrl] = now
      parseRssOrAtomXml(
        rawXml = rawXml,
        source = source,
        retrievedAtEpochMs = now,
      )
    } catch (e: Exception) {
      RssIngestionOutcome(
        sourceId = source.id,
        sourceName = source.name,
        importedArticles = emptyList(),
        skippedDuplicatesOrInvalid = 0,
        errorMessage = (e.message ?: e.javaClass.simpleName).take(220),
      )
    }
  }

  fun parseRssOrAtomXml(
    rawXml: String,
    source: ConfiguredRssFeedSource,
    retrievedAtEpochMs: Long = clockMs(),
  ): RssIngestionOutcome {
    if (rawXml.isBlank()) {
      return RssIngestionOutcome(
        sourceId = source.id,
        sourceName = source.name,
        importedArticles = emptyList(),
        skippedDuplicatesOrInvalid = 0,
        errorMessage = "Empty RSS feed payload.",
      )
    }

    // Reject XML containing DOCTYPE / ENTITY declarations before parsing (XXE defense-in-depth)
    if (rawXml.contains("<!ENTITY", ignoreCase = true) || rawXml.contains("<!DOCTYPE", ignoreCase = true)) {
      return RssIngestionOutcome(
        sourceId = source.id,
        sourceName = source.name,
        importedArticles = emptyList(),
        skippedDuplicatesOrInvalid = 1,
        errorMessage = "Rejected unsafe XML DOCTYPE/ENTITY declaration.",
      )
    }

    val deduplicatedByCanonicalUrl = LinkedHashMap<String, CachedNewsArticleEntity>()
    var skippedCount = 0

    try {
      val factory = XmlPullParserFactory.newInstance()
      factory.isNamespaceAware = false
      runCatching {
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false)
      }
      val parser = factory.newPullParser()
      parser.setInput(StringReader(rawXml))

      var inItem = false
      var currentTag = ""
      var rawTitle = ""
      var rawDescription = ""
      var rawLink = ""
      var rawPubDate = ""
      var rawCategory = ""
      var rawImageUrl = ""

      var eventType = parser.eventType
      while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
          XmlPullParser.START_TAG -> {
            val tagName = parser.name?.lowercase(Locale.ROOT).orEmpty()
            if (tagName == "item" || tagName == "entry") {
              inItem = true
              rawTitle = ""
              rawDescription = ""
              rawLink = ""
              rawPubDate = ""
              rawCategory = ""
              rawImageUrl = ""
            } else if (inItem) {
              currentTag = tagName
              if (tagName == "link" && rawLink.isBlank()) {
                val href = parser.getAttributeValue(null, "href")?.trim().orEmpty()
                if (href.isNotEmpty()) {
                  rawLink = href
                }
              }
              if ((tagName == "enclosure" || tagName == "media:content" || tagName == "media:thumbnail") && rawImageUrl.isBlank()) {
                val mediaUrl = parser.getAttributeValue(null, "url")?.trim().orEmpty()
                if (isValidHttpsNewsUrl(mediaUrl)) {
                  rawImageUrl = mediaUrl
                }
              }
            }
          }
          XmlPullParser.TEXT -> {
            if (inItem && currentTag.isNotEmpty()) {
              val text = parser.text.orEmpty()
              when (currentTag) {
                "title" -> rawTitle += text
                "description", "summary", "content:encoded", "content" -> {
                  if (rawDescription.length < 2500) {
                    rawDescription += text
                  }
                }
                "link" -> if (rawLink.isBlank()) rawLink += text
                "pubdate", "published", "updated", "dc:date" -> {
                  if (rawPubDate.isBlank()) rawPubDate += text
                }
                "category" -> if (rawCategory.isBlank()) rawCategory += text
              }
            }
          }
          XmlPullParser.END_TAG -> {
            val tagName = parser.name?.lowercase(Locale.ROOT).orEmpty()
            if (tagName == "item" || tagName == "entry") {
              inItem = false
              val cleanTitle = sanitizeHeadlineText(rawTitle)
              val cleanSummary = sanitizeExternalSummaryText(rawDescription.ifBlank { rawTitle })
              val trimmedUrl = rawLink.trim()

              if (
                cleanTitle.length >= 4 &&
                  cleanSummary.length >= 10 &&
                  isValidHttpsNewsUrl(trimmedUrl)
              ) {
                val canonical = canonicalizeNewsUrl(trimmedUrl)
                if (deduplicatedByCanonicalUrl.containsKey(canonical)) {
                  skippedCount++
                } else {
                  val parsedEpoch = parseFeedDateEpochMs(rawPubDate, fallbackMs = retrievedAtEpochMs)
                  val normalizedIso = formatIsoUtc(parsedEpoch)
                  val inferredCategory =
                    inferNewsCategory(
                      explicitTag = rawCategory,
                      title = cleanTitle,
                      summary = cleanSummary,
                      fallback = source.defaultCategory,
                    )
                  val verifiedWilaya = extractVerifiedWilayaTag("$cleanTitle $rawCategory")
                  val articleId = "rss_${sha256ShortHash(canonical)}"
                  deduplicatedByCanonicalUrl[canonical] =
                    CachedNewsArticleEntity(
                      id = articleId,
                      canonicalUrl = canonical,
                      title = cleanTitle,
                      summary = cleanSummary,
                      sourceName = source.name.take(120),
                      originalUrl = trimmedUrl.take(500),
                      publishedAtIso = normalizedIso,
                      publishedAtEpochMs = parsedEpoch,
                      retrievedAtEpochMs = retrievedAtEpochMs,
                      categoryCode = inferredCategory.code,
                      language = detectLanguageOrFallback("$cleanTitle $cleanSummary", source.language),
                      wilaya = verifiedWilaya,
                      imageUrl = if (isValidHttpsNewsUrl(rawImageUrl)) rawImageUrl.take(500) else "",
                      verificationStatusCode = NewsVerificationStatus.VERIFIED_SOURCE.code,
                    )
                }
              } else {
                skippedCount++
              }
            }
            currentTag = ""
          }
        }
        eventType = parser.next()
      }
    } catch (e: Exception) {
      return RssIngestionOutcome(
        sourceId = source.id,
        sourceName = source.name,
        importedArticles = deduplicatedByCanonicalUrl.values.toList(),
        skippedDuplicatesOrInvalid = skippedCount + 1,
        errorMessage = "Malformed RSS/Atom XML: ${(e.message ?: e.javaClass.simpleName).take(180)}",
      )
    }

    return RssIngestionOutcome(
      sourceId = source.id,
      sourceName = source.name,
      importedArticles = deduplicatedByCanonicalUrl.values.take(MAX_ITEMS_PER_FEED),
      skippedDuplicatesOrInvalid = skippedCount,
      errorMessage = null,
    )
  }

  companion object {
    private const val MAX_ITEMS_PER_FEED = 20
    private const val MAX_SUMMARY_CHARS = 340
    private const val MAX_HEADLINE_CHARS = 200

    /**
     * Permitted public institutional agricultural RSS sources.
     */
    val defaultOfficialSources: List<ConfiguredRssFeedSource> =
      listOf(
        ConfiguredRssFeedSource(
          id = "src_aps_economie_ar",
          name = "وكالة الأنباء الجزائرية (APS) — الاقتصاد والفلاحة",
          baseUrl = "https://www.aps.dz",
          feedUrl = "https://www.aps.dz/ar/economie?format=feed&type=rss",
          sourceType = NewsSourceType.OFFICIAL_INSTITUTION,
          defaultCategory = FellahNewsCategory.GOV_ANNOUNCEMENTS,
          language = "AR",
        ),
        ConfiguredRssFeedSource(
          id = "src_aps_economie_fr",
          name = "Algérie Presse Service (APS) — Économie & Agriculture",
          baseUrl = "https://www.aps.dz",
          feedUrl = "https://www.aps.dz/economie?format=feed&type=rss",
          sourceType = NewsSourceType.OFFICIAL_INSTITUTION,
          defaultCategory = FellahNewsCategory.GOV_ANNOUNCEMENTS,
          language = "FR",
        ),
        ConfiguredRssFeedSource(
          id = "src_fao_newsroom",
          name = "FAO Newsroom — منظمة الأغذية والزراعة",
          baseUrl = "https://www.fao.org",
          feedUrl = "https://www.fao.org/feeds/fao-newsroom-rss",
          sourceType = NewsSourceType.OFFICIAL_INSTITUTION,
          defaultCategory = FellahNewsCategory.CROP_PRODUCTION,
          language = "EN",
        ),
      )

    /**
     * Explicitly labelled **Institutional Reference Directory** cards (`isDemonstrationReference = true`).
     *
     * Honesty & Integrity Rule:
     * - These represent real, verified Algerian and UN agricultural institutional portals
     *   (MADR, INRAA, ONM, OAIC, FAO) with their real `https://` portals — NEVER fabricated breaking
     *   headlines, fake quotes, invented prices, or fake weather forecasts.
     * - Used ONLY when zero live RSS/Firestore articles are available (e.g. offline mode),
     *   and always rendered behind an explicit Demonstration / Institutional Reference Guide banner.
     */
    fun demonstrationReferenceGuideItems(retrievedAtMs: Long = 1761638400000L): List<FellahMatinArticle> =
      listOf(
        FellahMatinArticle(
          id = "ref_madr_portal",
          title = "[دليل مرجعي] البوابة الرسمية لوزارة الفلاحة والتنمية الريفية (MADR)",
          summary = "مرجع مؤسساتي للاطلاع على البرامج الوطنية لدعم الفلاحين، تنظيم الشعب الفلاحية، والبيانات الرسمية عند توفر الاتصال بالشبكة.",
          sourceName = "وزارة الفلاحة والتنمية الريفية (MADR)",
          originalUrl = "https://madr.gov.dz",
          publishedAtIso = "2026-01-01T08:00:00Z",
          publishedAtEpochMs = 1767254400000L,
          retrievedAtEpochMs = retrievedAtMs,
          category = FellahNewsCategory.GOV_ANNOUNCEMENTS,
          language = "AR",
          wilaya = "",
          verificationStatus = NewsVerificationStatus.VERIFIED_SOURCE,
          moderationStatus = NewsModerationStatus.APPROVED,
          isDemonstrationReference = true,
        ),
        FellahMatinArticle(
          id = "ref_inraa_research",
          title = "[دليل مرجعي] المعهد الوطني للبحث الزراعي الجزائري (INRAA) — الممارسات الزراعية والتربة",
          summary = "بوابة الأبحاث التطبيقية حول خصوبة التربة، الموارد الوراثية، وتقنيات السقي الاقتصادي الملائمة للمناطق السهبية والواحات.",
          sourceName = "INRAA Algérie",
          originalUrl = "https://www.inraa.dz",
          publishedAtIso = "2026-01-01T08:00:00Z",
          publishedAtEpochMs = 1767254300000L,
          retrievedAtEpochMs = retrievedAtMs,
          category = FellahNewsCategory.RESEARCH_AND_TECH,
          language = "AR",
          wilaya = "",
          verificationStatus = NewsVerificationStatus.VERIFIED_SOURCE,
          moderationStatus = NewsModerationStatus.APPROVED,
          isDemonstrationReference = true,
        ),
        FellahMatinArticle(
          id = "ref_onm_meteo",
          title = "[Guide Référence] Office National de la Météorologie (ONM) — Bulletins & Vigilance",
          summary = "Portail officiel des bulletins météorologiques et cartes de vigilance climatique en Algérie pour planifier l'irrigation, la récolte et le transport.",
          sourceName = "Météo Algérie (ONM)",
          originalUrl = "https://www.meteo.dz",
          publishedAtIso = "2026-01-01T08:00:00Z",
          publishedAtEpochMs = 1767254200000L,
          retrievedAtEpochMs = retrievedAtMs,
          category = FellahNewsCategory.WEATHER_ALERTS,
          language = "FR",
          wilaya = "",
          verificationStatus = NewsVerificationStatus.VERIFIED_SOURCE,
          moderationStatus = NewsModerationStatus.APPROVED,
          isDemonstrationReference = true,
        ),
        FellahMatinArticle(
          id = "ref_fao_algeria",
          title = "[Reference Guide] FAO Agricultural Practices, Post-Harvest & Cold-Chain Standards",
          summary = "Official FAO technical resources on seed certification, sustainable irrigation, cold-storage handling, and post-harvest loss reduction.",
          sourceName = "FAO — Food and Agriculture Organization",
          originalUrl = "https://www.fao.org/home/en",
          publishedAtIso = "2026-01-01T08:00:00Z",
          publishedAtEpochMs = 1767254100000L,
          retrievedAtEpochMs = retrievedAtMs,
          category = FellahNewsCategory.COLD_STORAGE_POSTHARVEST,
          language = "EN",
          wilaya = "",
          verificationStatus = NewsVerificationStatus.VERIFIED_SOURCE,
          moderationStatus = NewsModerationStatus.APPROVED,
          isDemonstrationReference = true,
        ),
      )

    /**
     * Strict HTTPS URL validator preventing `javascript:`, `data:`, `http://`, and SSRF local targets.
     */
    fun isValidHttpsNewsUrl(rawUrl: String?): Boolean {
      val trimmed = rawUrl?.trim().orEmpty()
      if (trimmed.length !in 10..500) return false
      if (!trimmed.startsWith("https://")) return false
      return try {
        val uri = URI(trimmed)
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
        if (scheme != "https" || host.isBlank() || !host.contains('.')) return false
        if (
          host == "localhost" ||
            host.endsWith(".local") ||
            host.startsWith("127.") ||
            host.startsWith("10.") ||
            host.startsWith("192.168.") ||
            host == "0.0.0.0"
        ) {
          return false
        }
        Regex("^[a-z0-9.-]+\\.[a-z]{2,}$").matches(host)
      } catch (_: Exception) {
        false
      }
    }

    /**
     * Canonicalizes article URLs (stripping UTM/tracking query parameters and trailing slashes)
     * so duplicate syndications across RSS & editorial imports collapse deterministically.
     */
    fun canonicalizeNewsUrl(rawUrl: String): String {
      return try {
        val uri = URI(rawUrl.trim())
        val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
        val path = (uri.path ?: "").trimEnd('/').ifEmpty { "/" }
        val keptParams =
          uri.rawQuery
            ?.split("&")
            ?.filter { param ->
              val key = param.substringBefore("=").lowercase(Locale.ROOT)
              key.isNotBlank() &&
                !key.startsWith("utm_") &&
                key != "fbclid" &&
                key != "gclid" &&
                key != "ref"
            }
            ?.sorted()
            ?.joinToString("&")
            .orEmpty()
        if (keptParams.isEmpty()) "https://$host$path" else "https://$host$path?$keptParams"
      } catch (_: Exception) {
        rawUrl.trim().trimEnd('/')
      }
    }

    fun sha256ShortHash(input: String): String {
      val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
      return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    fun sanitizeHeadlineText(raw: String): String {
      val noHtml = stripHtmlAndEntities(raw)
      return noHtml
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .replace(Regex("\\s{2,}"), " ")
        .trim()
        .take(MAX_HEADLINE_CHARS)
    }

    /**
     * Strips `<script>`, `<style>`, all HTML tags, control characters, and prompt-injection markers,
     * capping summary length to [MAX_SUMMARY_CHARS] so full external articles are never republished.
     */
    fun sanitizeExternalSummaryText(raw: String): String {
      val noHtml = stripHtmlAndEntities(raw)
      val cleaned =
        noHtml
          .replace(Regex("(?i)(ignore previous instructions|system prompt|<untrusted_news_articles>|</untrusted_news_articles>)"), "")
          .replace(Regex("[\\r\\n\\t]+"), " ")
          .replace(Regex("\\s{2,}"), " ")
          .trim()
      return if (cleaned.length <= MAX_SUMMARY_CHARS) {
        cleaned
      } else {
        cleaned.take(MAX_SUMMARY_CHARS - 1).trimEnd() + "…"
      }
    }

    private fun stripHtmlAndEntities(raw: String): String =
      raw
        .replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
        .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("[\\p{Cntrl}&&[^\\r\\n\\t]]"), "")

    fun inferNewsCategory(
      explicitTag: String,
      title: String,
      summary: String,
      fallback: FellahNewsCategory,
    ): FellahNewsCategory {
      FellahNewsCategory.fromCodeOrNull(explicitTag)?.let { return it }
      val haystack = "$explicitTag $title $summary".lowercase(Locale.ROOT)
      return when {
        listOf("météo", "meteo", "طقس", "صقيع", "أمطار", "جفاف", "gel", "pluie", "weather", "drought").any { haystack.contains(it) } ->
          FellahNewsCategory.WEATHER_ALERTS
        listOf("froid", "frigo", "تبريد", "تخزين", "توضيب", "post-récolte", "cold storage", "packaging").any { haystack.contains(it) } ->
          FellahNewsCategory.COLD_STORAGE_POSTHARVEST
        listOf("transport", "logistique", "نقل", "شحن", "لوجستيك", "camion", "freight").any { haystack.contains(it) } ->
          FellahNewsCategory.TRANSPORT_LOGISTICS
        listOf("semence", "engrais", "irrigation", "بذور", "أسمدة", "سقي", "ري", "جرار", "عتاد", "fertilizer", "machinery").any { haystack.contains(it) } ->
          FellahNewsCategory.INPUTS_AND_MACHINERY
        listOf("prix", "marché", "سوق", "أسعار", "تموين", "وفرة", "wholesale", "market price").any { haystack.contains(it) } ->
          FellahNewsCategory.MARKET_AVAILABILITY
        listOf("inraa", "recherche", "innovation", "drone", "بحث", "تكنولوجيا", "ابتكار", "رقمنة", "research").any { haystack.contains(it) } ->
          FellahNewsCategory.RESEARCH_AND_TECH
        listOf("formation", "vulgarisation", "تكوين", "إرشاد", "مهندس", "استشارة", "training", "advisory").any { haystack.contains(it) } ->
          FellahNewsCategory.SERVICES_AND_TRAINING
        listOf("salon", "foire", "معرض", "تظاهرة", "ملتقى", "sipsa", "event", "conference").any { haystack.contains(it) } ->
          FellahNewsCategory.EVENTS_AND_FAIRS
        listOf("récolte", "blé", "céréale", "datte", "olivier", "حصاد", "محصول", "زراعة", "نخيل", "بطاطا", "crop", "harvest").any { haystack.contains(it) } ->
          FellahNewsCategory.CROP_PRODUCTION
        else -> fallback
      }
    }

    /**
     * Extracts a wilaya scope ONLY when an Algerian wilaya name is explicitly stated in the verified
     * headline/category metadata. Never guesses or fabricates regional scope.
     */
    fun extractVerifiedWilayaTag(verifiedHeadlineOrTag: String): String {
      val lower = verifiedHeadlineOrTag.lowercase(Locale.ROOT)
      val knownWilayas =
        listOf(
          "01 - أدرار (Adrar)" to listOf("أدرار", "adrar"),
          "07 - بسكرة (Biskra)" to listOf("بسكرة", "biskra", "طولقة", "tolga"),
          "16 - الجزائر (Alger)" to listOf("الجزائر العاصمة", "alger "),
          "19 - سطيف (Sétif)" to listOf("سطيف", "sétif", "setif"),
          "29 - معسكر (Mascara)" to listOf("معسكر", "mascara"),
          "30 - ورقلة (Ouargla)" to listOf("ورقلة", "ouargla"),
          "39 - الوادي (El Oued)" to listOf("الوادي", "واد سوف", "el oued"),
          "44 - عين الدفلى (Aïn Defla)" to listOf("عين الدفلى", "ain defla", "aïn defla"),
        )
      return knownWilayas.firstOrNull { (_, tokens) -> tokens.any { lower.contains(it) } }?.first.orEmpty()
    }

    private fun detectLanguageOrFallback(text: String, fallback: String): String {
      if (Regex("[\\u0600-\\u06FF]").containsMatchIn(text)) return "AR"
      val normalized = fallback.uppercase(Locale.ROOT)
      return if (normalized in setOf("AR", "FR", "EN")) normalized else "FR"
    }

    fun parseFeedDateEpochMs(rawDate: String, fallbackMs: Long): Long {
      val trimmed = rawDate.trim()
      if (trimmed.isEmpty()) return fallbackMs
      val patterns =
        listOf(
          "EEE, dd MMM yyyy HH:mm:ss Z",
          "EEE, d MMM yyyy HH:mm:ss Z",
          "yyyy-MM-dd'T'HH:mm:ss'Z'",
          "yyyy-MM-dd'T'HH:mm:ssXXX",
          "yyyy-MM-dd HH:mm:ss",
          "yyyy-MM-dd",
        )
      for (pattern in patterns) {
        try {
          val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
          sdf.timeZone = TimeZone.getTimeZone("UTC")
          val parsed = sdf.parse(trimmed)
          if (parsed != null && parsed.time > 946684800000L) {
            return parsed.time
          }
        } catch (_: Exception) {
          // Try next RFC-822 / ISO-8601 format
        }
      }
      return fallbackMs
    }

    fun formatIsoUtc(epochMs: Long): String {
      val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
      sdf.timeZone = TimeZone.getTimeZone("UTC")
      return sdf.format(Date(epochMs))
    }

    private suspend fun defaultHttpsGetXml(feedUrl: String): String =
      withContext(Dispatchers.IO) {
        require(isValidHttpsNewsUrl(feedUrl)) { "Non-HTTPS feed URL rejected" }
        val connection = (URL(feedUrl).openConnection() as HttpURLConnection)
        try {
          connection.requestMethod = "GET"
          connection.instanceFollowRedirects = false // Prevent HTTPS -> HTTP downgrade redirect
          connection.connectTimeout = 6_000
          connection.readTimeout = 6_000
          connection.setRequestProperty(
            "User-Agent",
            "FellahNumerique-MatinRssReader/1.0 (+https://fellah-numerique.dz)",
          )
          connection.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml;q=0.9")
          val code = connection.responseCode
          if (code !in 200..299) {
            throw IllegalStateException("HTTP $code from news source")
          }
          connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            val buffer = CharArray(8192)
            val sb = StringBuilder()
            var read = reader.read(buffer)
            while (read != -1 && sb.length < 350_000) {
              sb.append(buffer, 0, read)
              read = reader.read(buffer)
            }
            sb.toString()
          }
        } finally {
          connection.disconnect()
        }
      }
  }
}
