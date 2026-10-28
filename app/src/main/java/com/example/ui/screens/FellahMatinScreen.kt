package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppLanguage
import com.example.data.model.CropVarietyType
import com.example.data.model.MarketListing
import com.example.data.model.UserRole
import com.example.data.network.AgriWeatherReport
import com.example.data.news.FellahMatinArticle
import com.example.data.news.FellahMatinStrings
import com.example.data.news.FellahNewsCategory
import com.example.data.news.FellahNewsRssIngestionService
import com.example.data.news.MorningBriefingSummary
import com.example.data.news.NewsArticleRecord
import com.example.data.news.NewsFeedMode
import com.example.data.news.NewsModerationStatus
import com.example.data.news.NewsSourceRecord
import com.example.data.news.NewsSourceType
import com.example.data.news.UserNewsPreferences
import com.example.ui.viewmodel.FellahMatinViewModel
import com.example.util.RoleStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Prominent **Fellah Matin (أخبار الصباح)** card placed near the top of both
 * [RoleDashboardScreen] (for all 6 professional roles) and [VisitorExploreScreen] (for Visitors).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FellahMatinDashboardSection(
  matinViewModel: FellahMatinViewModel,
  language: AppLanguage,
  activeRole: UserRole?,
  onViewAllNews: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val uiState by matinViewModel.uiState.collectAsStateWithLifecycle()
  val feedPage by matinViewModel.feedPageState.collectAsStateWithLifecycle()

  LaunchedEffect(activeRole, language) {
    matinViewModel.syncContext(activeRole = activeRole, language = language)
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
      ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
          shape = RoundedCornerShape(20.dp),
        )
        .testTag("fellah_matin_dashboard_section"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // 1. Morning-News Heading & Recognizable Sun/Newspaper Icon + View All Action
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f, fill = false),
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(44.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.WbSunny,
                contentDescription = FellahMatinStrings.sectionHeading(language),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
              )
            }
          }
          Column {
            Text(
              text = FellahMatinStrings.sectionHeading(language),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.testTag("fellah_matin_heading"),
            )
            Text(
              text = FellahMatinStrings.brandSubtitle(language),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          IconButton(
            onClick = { matinViewModel.refreshLiveNewsFeeds(force = true) },
            modifier = Modifier.size(48.dp).testTag("fellah_matin_refresh_button"),
          ) {
            if (uiState.isRefreshingFeeds) {
              CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
              Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription =
                  RoleStrings.tr(language, "تحديث الأخبار", "Actualiser", "Refresh news"),
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }

          OutlinedButton(
            onClick = onViewAllNews,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(48.dp).testTag("fellah_matin_view_all_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Newspaper,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = FellahMatinStrings.viewAllAction(language),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      // 2. Concise Daily Summary
      uiState.morningBriefing?.let { briefing ->
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
          modifier = Modifier.fillMaxWidth().testTag("fellah_matin_daily_summary_box"),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector =
                  if (briefing.isAiGenerated) Icons.Filled.AutoAwesome else Icons.Filled.Verified,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text =
                  if (briefing.isAiGenerated) {
                    RoleStrings.tr(
                      language,
                      "ملخص الصباح الذكي (من مصادر موثقة فقط)",
                      "Synthèse matinale IA (sources vérifiées uniquement)",
                      "AI Morning Summary (from verified sources only)",
                    )
                  } else {
                    RoleStrings.tr(
                      language,
                      "موجز الصباح التحريري",
                      "Synthèse matinale vérifiée",
                      "Morning Editorial Digest",
                    )
                  },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Text(
              text = briefing.summaryText,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              maxLines = 4,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      // 3. Optional Pauseable & Manually-Scrollable Headline Ticker (screen-reader friendly)
      if (feedPage.items.isNotEmpty()) {
        PauseableHeadlineTicker(
          articles = feedPage.items,
          language = language,
          isPaused = uiState.isTickerPaused,
          onTogglePause = matinViewModel::toggleTickerPaused,
          onSelectHeadline = { article -> matinViewModel.openArticleDetail(article) },
        )
      }

      // 4. Shared General Feed vs Role Priority + Quick Category Filter Pills
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        FilterChip(
          selected = uiState.feedMode == NewsFeedMode.SHARED_ALL && uiState.selectedCategoryCode == null,
          onClick = {
            matinViewModel.setFeedMode(NewsFeedMode.SHARED_ALL)
            if (uiState.selectedCategoryCode != null) matinViewModel.selectCategory(null)
          },
          label = {
            Text(
              RoleStrings.tr(language, "كل الأخبار العامة", "Toutes les actualités", "All General News")
            )
          },
          modifier = Modifier.testTag("fellah_matin_mode_all"),
        )
        if (activeRole != null) {
          FilterChip(
            selected = uiState.feedMode == NewsFeedMode.ROLE_PRIORITY,
            onClick = { matinViewModel.setFeedMode(NewsFeedMode.ROLE_PRIORITY) },
            label = {
              Text(
                RoleStrings.tr(
                  language,
                  "مقترحات لنشاطي (${activeRole.localizedTitle(language)})",
                  "Priorité ${activeRole.localizedTitle(language)}",
                  "Recommended for ${activeRole.localizedTitle(language)}",
                )
              )
            },
            modifier = Modifier.testTag("fellah_matin_mode_role"),
          )
        }
        val displayedCategories =
          if (activeRole != null) {
            (FellahNewsCategory.priorityCategoriesForRole(activeRole) + FellahNewsCategory.entries)
              .distinct()
          } else {
            FellahNewsCategory.entries
          }
        displayedCategories.take(6).forEach { cat ->
          FilterChip(
            selected = uiState.selectedCategoryCode == cat.code,
            onClick = { matinViewModel.selectCategory(cat.code) },
            label = { Text(cat.localizedTitle(language)) },
            modifier = Modifier.testTag("fellah_matin_chip_${cat.code}"),
          )
        }
      }

      // 5. Explicit Demonstration / Institutional Reference Notice (Honesty Rule)
      if (feedPage.isUsingDemonstrationFallback) {
        DemonstrationReferenceBanner(
          language = language,
          lastFeedError = uiState.lastFeedError,
          onHideDemoGuide = { matinViewModel.setAllowDemonstrationFallback(false) },
        )
      } else if (uiState.lastFeedError != null && feedPage.items.isEmpty()) {
        NewsErrorBanner(
          language = language,
          errorText = uiState.lastFeedError.orEmpty(),
          onRetry = { matinViewModel.refreshLiveNewsFeeds(force = true) },
        )
      }

      // 6. Featured News Cards (up to 3 on Home/Dashboard) or Empty State
      if (uiState.isLoading && feedPage.items.isEmpty()) {
        Box(
          modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator(modifier = Modifier.testTag("fellah_matin_loading"))
        }
      } else if (feedPage.items.isEmpty()) {
        EmptyNewsStateBox(
          language = language,
          onResetFilters = {
            matinViewModel.selectCategory(null)
            matinViewModel.setWilayaFilter("")
            matinViewModel.setLanguageFilter("ALL")
          },
          onShowInstitutionalDirectory = {
            matinViewModel.setAllowDemonstrationFallback(true)
          },
        )
      } else {
        feedPage.items.take(3).forEach { article ->
          FellahMatinArticleCard(
            article = article,
            language = language,
            onOpenDetail = { matinViewModel.openArticleDetail(article) },
            onOpenExternalUrl = { openVerifiedHttpsArticle(context, article.originalUrl) },
          )
        }
      }
    }
  }

  uiState.selectedArticleForDetail?.let { selected ->
    FellahMatinArticleDetailDialog(
      article = selected,
      language = language,
      onOpenSourceLink = { openVerifiedHttpsArticle(context, selected.originalUrl) },
      onDismiss = { matinViewModel.openArticleDetail(null) },
    )
  }
}

/**
 * Full-screen **Fellah Matin** newsroom (`WorkspaceSection.FELLAH_MATIN` and Visitor News Tab)
 * with all 10 categories, wilaya/language filters, pagination, cached AI/non-AI morning briefing,
 * user preferences, **separated verified weather & market snapshots**, and protected Editorial Admin Console.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FellahMatinFullScreen(
  matinViewModel: FellahMatinViewModel,
  language: AppLanguage,
  activeRole: UserRole?,
  isAuthenticated: Boolean,
  verifiedMarketListings: List<MarketListing>,
  verifiedWeatherReport: AgriWeatherReport?,
  isLoadingWeather: Boolean,
  weatherError: String?,
  onRefreshWeather: (() -> Unit)?,
  onBack: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  if (onBack != null) {
    BackHandler(onBack = onBack)
  }

  val context = LocalContext.current
  val uiState by matinViewModel.uiState.collectAsStateWithLifecycle()
  val feedPage by matinViewModel.feedPageState.collectAsStateWithLifecycle()
  val userPrefs by matinViewModel.userPreferencesState.collectAsStateWithLifecycle()
  val editorialQueue by matinViewModel.editorialQueueState.collectAsStateWithLifecycle()
  val adminSources by matinViewModel.adminSourcesState.collectAsStateWithLifecycle()

  LaunchedEffect(activeRole, language) {
    matinViewModel.syncContext(activeRole = activeRole, language = language)
  }

  Column(
    modifier = modifier.fillMaxWidth().testTag("fellah_matin_full_screen"),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Header Bar
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (onBack != null) {
              IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp).testTag("fellah_matin_back_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = RoleStrings.tr(language, "رجوع", "Retour", "Back"),
                )
              }
            }
            Icon(
              imageVector = Icons.Filled.WbSunny,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp),
            )
            Column {
              Text(
                text = FellahMatinStrings.sectionHeading(language),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = FellahMatinStrings.brandSubtitle(language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = { matinViewModel.refreshLiveNewsFeeds(force = true) },
              modifier = Modifier.height(48.dp).testTag("fellah_matin_full_refresh_button"),
            ) {
              Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(RoleStrings.tr(language, "تحديث المصادر", "Actualiser", "Refresh"))
            }

            if (isAuthenticated) {
              OutlinedButton(
                onClick = { matinViewModel.setShowPreferencesDialog(true) },
                modifier = Modifier.height(48.dp).testTag("fellah_matin_preferences_button"),
              ) {
                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(RoleStrings.tr(language, "تفضيلاتي", "Préférences", "Preferences"))
              }
            }

            if (uiState.isEditorialAdmin) {
              Button(
                onClick = { matinViewModel.setShowAdminConsoleDialog(true) },
                modifier = Modifier.height(48.dp).testTag("fellah_matin_admin_console_button"),
              ) {
                Icon(
                  Icons.Filled.AdminPanelSettings,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(RoleStrings.tr(language, "الإدارة التحريرية", "Admin Éditorial", "Editorial Admin"))
              }
            }
          }
        }

        // Pauseable Headline Ticker
        if (feedPage.items.isNotEmpty()) {
          PauseableHeadlineTicker(
            articles = feedPage.items,
            language = language,
            isPaused = uiState.isTickerPaused,
            onTogglePause = matinViewModel::toggleTickerPaused,
            onSelectHeadline = { matinViewModel.openArticleDetail(it) },
          )
        }
      }
    }

    // Morning Briefing Card (Cached AI Optional + Deterministic Non-AI Digest)
    MorningBriefingCard(
      briefing = uiState.morningBriefing,
      language = language,
      preferAi = uiState.preferAiBriefing,
      canUseAi = isAuthenticated,
      isLoading = uiState.isLoadingBriefing,
      onTogglePreferAi = matinViewModel::togglePreferAiBriefing,
      onOpenSourceUrl = { url -> openVerifiedHttpsArticle(context, url) },
    )

    // Feed Mode + Language + Wilaya + All 10 Category Filters
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = uiState.feedMode == NewsFeedMode.SHARED_ALL,
            onClick = { matinViewModel.setFeedMode(NewsFeedMode.SHARED_ALL) },
            label = {
              Text(
                RoleStrings.tr(
                  language,
                  "النشرة العامة المشتركة",
                  "Flux général partagé",
                  "Shared General Feed",
                )
              )
            },
            modifier = Modifier.testTag("full_mode_shared_all"),
          )
          if (activeRole != null) {
            FilterChip(
              selected = uiState.feedMode == NewsFeedMode.ROLE_PRIORITY,
              onClick = { matinViewModel.setFeedMode(NewsFeedMode.ROLE_PRIORITY) },
              label = {
                Text(
                  RoleStrings.tr(
                    language,
                    "ترتيب حسب نشاطي (${activeRole.localizedTitle(language)})",
                    "Recommandé (${activeRole.localizedTitle(language)})",
                    "Prioritized for ${activeRole.localizedTitle(language)}",
                  )
                )
              },
              modifier = Modifier.testTag("full_mode_role_priority"),
            )
          }

          listOf("ALL", "AR", "FR", "EN").forEach { langCode ->
            FilterChip(
              selected = uiState.languageFilter == langCode,
              onClick = { matinViewModel.setLanguageFilter(langCode) },
              label = {
                Text(
                  if (langCode == "ALL") {
                    RoleStrings.tr(language, "كل اللغات", "Toutes langues", "All Languages")
                  } else {
                    langCode
                  }
                )
              },
              modifier = Modifier.testTag("full_lang_filter_$langCode"),
            )
          }
        }

        // All 10 Categories Horizontal Scroll
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = uiState.selectedCategoryCode == null,
            onClick = { matinViewModel.selectCategory(null) },
            label = { Text(RoleStrings.tr(language, "جميع الفئات (10)", "Toutes catégories (10)", "All Categories (10)")) },
            modifier = Modifier.testTag("full_cat_all"),
          )
          FellahNewsCategory.entries.forEach { cat ->
            FilterChip(
              selected = uiState.selectedCategoryCode == cat.code,
              onClick = { matinViewModel.selectCategory(cat.code) },
              label = { Text(cat.localizedTitle(language)) },
              modifier = Modifier.testTag("full_cat_${cat.code}"),
            )
          }
        }

        // Verified Wilaya Chips (shown ONLY when verified articles carry wilaya tags)
        if (feedPage.availableWilayas.isNotEmpty() || uiState.wilayaFilter.isNotBlank()) {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = RoleStrings.tr(language, "النطاق الجغرافي الموثق:", "Wilaya vérifiée :", "Verified Wilaya:"),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilterChip(
              selected = uiState.wilayaFilter.isBlank(),
              onClick = { matinViewModel.setWilayaFilter("") },
              label = { Text(RoleStrings.tr(language, "وطني / الكل", "National / Toutes", "All Regions")) },
              modifier = Modifier.testTag("full_wilaya_all"),
            )
            feedPage.availableWilayas.forEach { wilaya ->
              FilterChip(
                selected = uiState.wilayaFilter == wilaya,
                onClick = { matinViewModel.setWilayaFilter(wilaya) },
                label = { Text(wilaya) },
                modifier = Modifier.testTag("full_wilaya_$wilaya"),
              )
            }
          }
        }
      }
    }

    // Demonstration / Error Notices
    if (feedPage.isUsingDemonstrationFallback) {
      DemonstrationReferenceBanner(
        language = language,
        lastFeedError = uiState.lastFeedError,
        onHideDemoGuide = { matinViewModel.setAllowDemonstrationFallback(false) },
      )
    } else if (uiState.lastFeedError != null && feedPage.items.isEmpty()) {
      NewsErrorBanner(
        language = language,
        errorText = uiState.lastFeedError.orEmpty(),
        onRetry = { matinViewModel.refreshLiveNewsFeeds(force = true) },
      )
    }

    // Paginated News Articles List
    if (feedPage.items.isEmpty()) {
      EmptyNewsStateBox(
        language = language,
        onResetFilters = {
          matinViewModel.selectCategory(null)
          matinViewModel.setWilayaFilter("")
          matinViewModel.setLanguageFilter("ALL")
        },
        onShowInstitutionalDirectory = {
          matinViewModel.setAllowDemonstrationFallback(true)
        },
      )
    } else {
      feedPage.items.forEach { article ->
        FellahMatinArticleCard(
          article = article,
          language = language,
          onOpenDetail = { matinViewModel.openArticleDetail(article) },
          onOpenExternalUrl = { openVerifiedHttpsArticle(context, article.originalUrl) },
        )
      }

      // Pagination Controls
      Row(
        modifier = Modifier.fillMaxWidth().testTag("fellah_matin_pagination_bar"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = { matinViewModel.goToPage(feedPage.page - 1) },
          enabled = feedPage.page > 1,
          modifier = Modifier.height(48.dp).testTag("fellah_matin_prev_page"),
        ) {
          Text(RoleStrings.tr(language, "السابق", "Précédent", "Previous"))
        }

        Text(
          text =
            RoleStrings.tr(
              language,
              "صفحة ${feedPage.page} من ${feedPage.totalPages} (${feedPage.totalItems} مادة)",
              "Page ${feedPage.page} / ${feedPage.totalPages} (${feedPage.totalItems} articles)",
              "Page ${feedPage.page} of ${feedPage.totalPages} (${feedPage.totalItems} items)",
            ),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(
          onClick = { matinViewModel.goToPage(feedPage.page + 1) },
          enabled = feedPage.hasNextPage,
          modifier = Modifier.height(48.dp).testTag("fellah_matin_next_page"),
        ) {
          Text(RoleStrings.tr(language, "التالي", "Suivant", "Next"))
        }
      }
    }

    // Clearly Separated Verified Weather & Market-Price Snapshot Section (Section 9)
    SeparatedVerifiedWeatherAndMarketSnapshots(
      language = language,
      verifiedWeatherReport = verifiedWeatherReport,
      isLoadingWeather = isLoadingWeather,
      weatherError = weatherError,
      onRefreshWeather = onRefreshWeather,
      verifiedMarketListings = verifiedMarketListings,
    )
  }

  // Article Detail Modal
  uiState.selectedArticleForDetail?.let { selected ->
    FellahMatinArticleDetailDialog(
      article = selected,
      language = language,
      onOpenSourceLink = { openVerifiedHttpsArticle(context, selected.originalUrl) },
      onDismiss = { matinViewModel.openArticleDetail(null) },
    )
  }

  // User News Preferences Dialog
  if (uiState.showPreferencesDialog && isAuthenticated) {
    UserNewsPreferencesDialog(
      language = language,
      currentPrefs = userPrefs,
      onSave = matinViewModel::saveUserPreferences,
      onDismiss = { matinViewModel.setShowPreferencesDialog(false) },
    )
  }

  // Protected Editorial Admin Console Dialog
  if (uiState.showAdminConsoleDialog && uiState.isEditorialAdmin) {
    EditorialAdminConsoleDialog(
      language = language,
      editorialQueue = editorialQueue,
      sources = adminSources,
      feedbackMessage = uiState.adminFeedbackMessage,
      onClearFeedback = matinViewModel::clearAdminFeedback,
      onSubmitArticle = matinViewModel::adminSubmitEditorialArticle,
      onModerateArticle = matinViewModel::adminModerateArticle,
      onToggleSource = matinViewModel::adminToggleNewsSource,
      onAddSource = matinViewModel::adminAddRssSource,
      onRunRssIngestion = matinViewModel::adminImportRssIntoEditorialQueue,
      onDismiss = { matinViewModel.setShowAdminConsoleDialog(false) },
    )
  }
}

@Composable
private fun PauseableHeadlineTicker(
  articles: List<FellahMatinArticle>,
  language: AppLanguage,
  isPaused: Boolean,
  onTogglePause: () -> Unit,
  onSelectHeadline: (FellahMatinArticle) -> Unit,
) {
  var activeIndex by remember(articles.size) { mutableIntStateOf(0) }

  // Calm 6.5-second rotation ONLY when not paused; never uses rapid marquee animation
  LaunchedEffect(isPaused, articles.size) {
    if (!isPaused && articles.size > 1) {
      while (true) {
        delay(6_500L)
        activeIndex = (activeIndex + 1) % articles.size
      }
    }
  }

  val pauseDesc =
    if (isPaused) {
      RoleStrings.tr(language, "تشغيل شريط العناوين", "Reprendre le bandeau", "Resume headline ticker")
    } else {
      RoleStrings.tr(language, "إيقاف شريط العناوين مؤقتاً", "Mettre en pause le bandeau", "Pause headline ticker")
    }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier =
      Modifier.fillMaxWidth()
        .semantics {
          contentDescription = FellahMatinStrings.sectionHeading(language)
        }
        .testTag("fellah_matin_ticker"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      IconButton(
        onClick = onTogglePause,
        modifier = Modifier.size(48.dp).testTag("fellah_matin_ticker_pause_button"),
      ) {
        Icon(
          imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
          contentDescription = pauseDesc,
          tint = MaterialTheme.colorScheme.primary,
        )
      }

      Row(
        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val ordered =
          if (articles.isEmpty()) emptyList()
          else articles.drop(activeIndex) + articles.take(activeIndex)
        ordered.take(5).forEach { item ->
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier =
              Modifier.clip(RoundedCornerShape(8.dp))
                .clickable { onSelectHeadline(item) },
          ) {
            Text(
              text = "${item.sourceName}: ${item.title}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FellahMatinArticleCard(
  article: FellahMatinArticle,
  language: AppLanguage,
  onOpenDetail: () -> Unit,
  onOpenExternalUrl: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (article.isDemonstrationReference) {
            MaterialTheme.colorScheme.surfaceContainer
          } else {
            MaterialTheme.colorScheme.surfaceContainerLow
          }
      ),
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onOpenDetail)
        .testTag("fellah_matin_article_${article.id}"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Category + Language + Verified Wilaya + Demonstration Badge
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = article.category.localizedTitle(language),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }

        if (article.isDemonstrationReference) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
          ) {
            Text(
              text =
                RoleStrings.tr(
                  language,
                  "دليل مؤسساتي مرجعي (تجريبي)",
                  "Portail de référence (Démo)",
                  "Reference Directory (Demo)",
                ),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }

        if (article.wilaya.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Text(
              text = article.wilaya,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
      }

      Text(
        text = article.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Text(
        text = article.summary,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      // Distinct Source Attribution + Original Publication Date vs Platform Retrieval Date + Link
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text =
              "${RoleStrings.tr(language, "المصدر:", "Source :", "Source:")} ${article.sourceName}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text =
              if (article.isDemonstrationReference) {
                RoleStrings.tr(
                  language,
                  "رابط بوابة رسمية ثابتة • التحقق على المنصة: ${formatReadableTimestamp(article.retrievedAtEpochMs)}",
                  "Portail permanent • Vérifié sur plateforme : ${formatReadableTimestamp(article.retrievedAtEpochMs)}",
                  "Permanent institutional portal • Platform verified: ${formatReadableTimestamp(article.retrievedAtEpochMs)}",
                )
              } else {
                RoleStrings.tr(
                  language,
                  "تاريخ النشر الأصلي: ${article.publishedAtIso} • تاريخ الجلب: ${formatReadableTimestamp(article.retrievedAtEpochMs)}",
                  "Publié le : ${article.publishedAtIso} • Récupéré le : ${formatReadableTimestamp(article.retrievedAtEpochMs)}",
                  "Published: ${article.publishedAtIso} • Retrieved: ${formatReadableTimestamp(article.retrievedAtEpochMs)}",
                )
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        TextButton(
          onClick = onOpenExternalUrl,
          modifier = Modifier.height(48.dp).testTag("fellah_matin_source_link_${article.id}"),
        ) {
          Text(
            text =
              RoleStrings.tr(
                language,
                "المقال الأصلي",
                "Article original",
                "Original article",
              ),
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = article.sourceName,
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun MorningBriefingCard(
  briefing: MorningBriefingSummary?,
  language: AppLanguage,
  preferAi: Boolean,
  canUseAi: Boolean,
  isLoading: Boolean,
  onTogglePreferAi: (Boolean) -> Unit,
  onOpenSourceUrl: (String) -> Unit,
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
      ),
    modifier = Modifier.fillMaxWidth().testTag("fellah_matin_briefing_card"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector =
              if (briefing?.isAiGenerated == true) Icons.Filled.AutoAwesome
              else Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column {
            Text(
              text =
                if (briefing?.isAiGenerated == true) {
                  RoleStrings.tr(
                    language,
                    "ملخص الصباح المولّد بالذكاء الاصطناعي (مسند للمصادر الأصلية)",
                    "Briefing matinal généré par IA (sources attribuées)",
                    "AI-Generated Morning Briefing (source-attributed)",
                  )
                } else {
                  RoleStrings.tr(
                    language,
                    "موجز الصباح التحريري من المقالات الموثقة",
                    "Briefing matinal des articles vérifiés",
                    "Verified Morning Briefing Digest",
                  )
                },
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            if (briefing?.isFromCache == true) {
              Text(
                text =
                  RoleStrings.tr(
                    language,
                    "نسخة مخزنة مؤقتاً لترشيد الاستهلاك",
                    "Synthèse mise en cache pour aujourd'hui",
                    "Cached summary for today",
                  ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        if (canUseAi) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = RoleStrings.tr(language, "تلخيص ذكي", "Résumé IA", "AI Summary"),
              style = MaterialTheme.typography.labelSmall,
            )
            Switch(
              checked = preferAi,
              onCheckedChange = onTogglePreferAi,
              modifier = Modifier.testTag("fellah_matin_ai_briefing_switch"),
            )
          }
        }
      }

      if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
      } else if (briefing != null) {
        Text(
          text = briefing.summaryText,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        if (briefing.fallbackReason.isNotBlank()) {
          Text(
            text = briefing.fallbackReason,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (briefing.sourceAttributions.isNotEmpty()) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
          Text(
            text =
              RoleStrings.tr(
                language,
                "المصادر الأصلية المعتمدة في هذا الموجز:",
                "Sources originales citées :",
                "Original sources cited in this briefing:",
              ),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
          briefing.sourceAttributions.forEach { src ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { onOpenSourceUrl(src.originalUrl) }
                  .padding(vertical = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "• ${src.headline} (${src.sourceName})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = src.sourceName,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Section 9 Requirement: Keep weather forecasts and live market prices visually separated from
 * editorial news cards, always displaying observation/update timestamps and sources or an explicit
 * unavailable state when not verified.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeparatedVerifiedWeatherAndMarketSnapshots(
  language: AppLanguage,
  verifiedWeatherReport: AgriWeatherReport?,
  isLoadingWeather: Boolean,
  weatherError: String?,
  onRefreshWeather: (() -> Unit)?,
  verifiedMarketListings: List<MarketListing>,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
      ),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
          shape = RoundedCornerShape(18.dp),
        )
        .testTag("fellah_matin_separated_telemetry_section"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text =
          RoleStrings.tr(
            language,
            "مؤشرات الرصد الميداني وأسعار السوق الموثقة (منفصلة عن المقالات الإخبارية)",
            "Données Météo & Prix du Marché Vérifiés (Section séparée des articles)",
            "Verified Weather & Market-Price Telemetry (Separated from News Articles)",
          ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
      )

      // 1. Weather Telemetry Sub-box
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().testTag("fellah_matin_weather_widget"),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(Icons.Filled.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Text(
                text =
                  RoleStrings.tr(
                    language,
                    "الرصد الجوي الفلاحي (المصدر: Open-Meteo FAO ET0)",
                    "Météo Agricole (Source : Open-Meteo FAO ET0)",
                    "Agri-Weather Telemetry (Source: Open-Meteo FAO ET0)",
                  ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
              )
            }
            if (onRefreshWeather != null) {
              TextButton(onClick = onRefreshWeather, modifier = Modifier.height(48.dp)) {
                Text(RoleStrings.tr(language, "تحديث الرصد", "Actualiser", "Refresh"))
              }
            }
          }

          if (isLoadingWeather) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
          } else if (verifiedWeatherReport != null) {
            Text(
              text =
                RoleStrings.tr(
                  language,
                  "الإحداثيات: ${verifiedWeatherReport.coordinatesLabel} • الحرارة: ${verifiedWeatherReport.currentTempC}°C • الرطوبة: ${verifiedWeatherReport.relativeHumidityPercent}% • الرياح: ${verifiedWeatherReport.windSpeedKmh} كم/سا • التبخر ET0: ${verifiedWeatherReport.dailyEt0Mm} ملم",
                  "Coordonnées : ${verifiedWeatherReport.coordinatesLabel} • Temp : ${verifiedWeatherReport.currentTempC}°C • Humidité : ${verifiedWeatherReport.relativeHumidityPercent}% • Vent : ${verifiedWeatherReport.windSpeedKmh} km/h • ET0 : ${verifiedWeatherReport.dailyEt0Mm} mm",
                  "Coords: ${verifiedWeatherReport.coordinatesLabel} • Temp: ${verifiedWeatherReport.currentTempC}°C • Humidity: ${verifiedWeatherReport.relativeHumidityPercent}% • Wind: ${verifiedWeatherReport.windSpeedKmh} km/h • ET0: ${verifiedWeatherReport.dailyEt0Mm} mm",
                ),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
            val obsDate = verifiedWeatherReport.forecastDays.firstOrNull()?.date.orEmpty()
            if (obsDate.isNotEmpty()) {
              Text(
                text =
                  RoleStrings.tr(
                    language,
                    "تاريخ النشرة الجوية المرصودة: $obsDate",
                    "Date d'observation : $obsDate",
                    "Observation date: $obsDate",
                  ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            Text(
              text =
                weatherError
                  ?: RoleStrings.tr(
                    language,
                    "بيانات الطقس الميدانية غير متوفرة حالياً (لا يتم عرض أي توقعات غير موثقة).",
                    "Données météo vérifiées actuellement indisponibles.",
                    "Verified live weather data is currently unavailable.",
                  ),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // 2. Verified Platform Market Prices Sub-box
      val activeVerifiedListings = verifiedMarketListings.filter { it.status == "active" }
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().testTag("fellah_matin_market_widget"),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(Icons.Filled.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
              text =
                RoleStrings.tr(
                  language,
                  "عروض السوق الفلاحي الموثقة بتوقيت النشر",
                  "Prix vérifiés publiés sur le Marché Fellah Numérique",
                  "Timestamped Verified Marketplace Listings",
                ),
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
            )
          }

          if (activeVerifiedListings.isEmpty()) {
            Text(
              text =
                RoleStrings.tr(
                  language,
                  "لا توجد إعلانات أسعار نشطة وموثقة حالياً (يُشترط تسجيل الدخول أو وجود عروض فلاحين منشورة؛ لا يتم اختلاق أسعار تقديرية).",
                  "Aucune annonce de prix vérifiée disponible actuellement.",
                  "No timestamped verified marketplace price listings are currently loaded.",
                ),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            activeVerifiedListings.take(4).forEach { listing ->
              val crop = CropVarietyType.fromCode(listing.cropType)
              val tsMs = (listing.updatedAt ?: listing.createdAt)?.toDate()?.time ?: 0L
              Text(
                text =
                  "• ${crop.titleAr} (${listing.wilaya}) — ${listing.pricePerKgDzd} DZD/kg • ${listing.operatorName}" +
                    if (tsMs > 0L) " • [${formatReadableTimestamp(tsMs)}]" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DemonstrationReferenceBanner(
  language: AppLanguage,
  lastFeedError: String?,
  onHideDemoGuide: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
    modifier = Modifier.fillMaxWidth().testTag("fellah_matin_demo_banner"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onTertiaryContainer,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text =
            RoleStrings.tr(
              language,
              "وضع الدليل المؤسساتي المرجعي (لا توجد أخبار حية مستلمة حالياً)",
              "Mode Annuaire Institutionnel de Référence (Aucun flux RSS actif reçu)",
              "Institutional Reference Directory Mode (No live articles currently loaded)",
            ),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
      }
      Text(
        text =
          RoleStrings.tr(
            language,
            "حفاظاً على المصداقية، لا يعرض Fellah Matin أخباراً أو أسعاراً مختلقة عند تعذر الوصول إلى خلاصات RSS الخارجية. البطاقات أدناه هي بوابات مؤسساتية رسمية ثابتة للمراجعة.",
            "Par intégrité éditoriale, aucune fausse actualité n'est générée hors ligne. Les cartes ci-dessous renvoient aux portails agricoles officiels.",
            "For data integrity, Fellah Matin never fabricates breaking news when external RSS feeds are unreachable. The cards below are verified permanent institutional portals.",
          ) + (if (!lastFeedError.isNullOrBlank()) " ($lastFeedError)" else ""),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
      TextButton(
        onClick = onHideDemoGuide,
        modifier = Modifier.height(48.dp).testTag("fellah_matin_hide_demo_button"),
      ) {
        Text(
          RoleStrings.tr(
            language,
            "إخفاء الدليل المرجعي وعرض الحالة الفارغة فقط",
            "Masquer l'annuaire de référence",
            "Hide reference guide",
          )
        )
      }
    }
  }
}

@Composable
private fun NewsErrorBanner(
  language: AppLanguage,
  errorText: String,
  onRetry: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.errorContainer,
    modifier = Modifier.fillMaxWidth().testTag("fellah_matin_error_banner"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = errorText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.weight(1f),
      )
      TextButton(onClick = onRetry, modifier = Modifier.height(48.dp)) {
        Text(RoleStrings.tr(language, "إعادة المحاولة", "Réessayer", "Retry"))
      }
    }
  }
}

@Composable
private fun EmptyNewsStateBox(
  language: AppLanguage,
  onResetFilters: () -> Unit,
  onShowInstitutionalDirectory: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = Modifier.fillMaxWidth().testTag("fellah_matin_empty_state"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text =
          RoleStrings.tr(
            language,
            "لا توجد مقالات منشورة ومطابقة لمعايير التصفية الحالية.",
            "Aucun article vérifié ne correspond aux filtres sélectionnés.",
            "No verified published articles match the selected filters.",
          ),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = onResetFilters,
          modifier = Modifier.height(48.dp).testTag("fellah_matin_reset_filters_button"),
        ) {
          Text(RoleStrings.tr(language, "إعادة ضبط الفلاتر", "Réinitialiser", "Reset filters"))
        }
        TextButton(
          onClick = onShowInstitutionalDirectory,
          modifier = Modifier.height(48.dp),
        ) {
          Text(RoleStrings.tr(language, "عرض البوابات المرجعية", "Portails officiels", "Reference portals"))
        }
      }
    }
  }
}

@Composable
private fun FellahMatinArticleDetailDialog(
  article: FellahMatinArticle,
  language: AppLanguage,
  onOpenSourceLink: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = article.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = article.category.localizedTitle(language),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = article.summary,
          style = MaterialTheme.typography.bodyMedium,
        )
        HorizontalDivider()
        Text(
          text = "${RoleStrings.tr(language, "المصدر:", "Source :", "Source:")} ${article.sourceName}",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text =
            "${RoleStrings.tr(language, "تاريخ النشر الأصلي:", "Date de publication :", "Original publication date:")} ${article.publishedAtIso}",
          style = MaterialTheme.typography.bodySmall,
        )
        Text(
          text =
            "${RoleStrings.tr(language, "تاريخ الجلب والتحقق:", "Date de récupération :", "Platform retrieval date:")} ${formatReadableTimestamp(article.retrievedAtEpochMs)}",
          style = MaterialTheme.typography.bodySmall,
        )
        if (article.wilaya.isNotBlank()) {
          Text(
            text = "${RoleStrings.tr(language, "الولاية:", "Wilaya :", "Wilaya:")} ${article.wilaya}",
            style = MaterialTheme.typography.bodySmall,
          )
        }
        Text(
          text = article.originalUrl,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onOpenSourceLink,
        modifier = Modifier.height(48.dp).testTag("fellah_matin_detail_open_url"),
      ) {
        Text(
          RoleStrings.tr(
            language,
            "قراءة المقال الكامل من المصدر",
            "Lire sur la source originale",
            "Read full article at source",
          )
        )
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
        Text(RoleStrings.tr(language, "إغلاق", "Fermer", "Close"))
      }
    },
  )
}

@Composable
private fun UserNewsPreferencesDialog(
  language: AppLanguage,
  currentPrefs: UserNewsPreferences?,
  onSave: (List<String>, String, String) -> Unit,
  onDismiss: () -> Unit,
) {
  val selectedCats = remember(currentPrefs) {
    mutableStateListOf<String>().apply {
      addAll(currentPrefs?.preferredCategories ?: emptyList())
    }
  }
  var preferredRegion by rememberSaveable(currentPrefs) {
    mutableStateOf(currentPrefs?.preferredRegion.orEmpty())
  }
  var preferredLang by rememberSaveable(currentPrefs) {
    mutableStateOf(currentPrefs?.preferredLanguage ?: "ALL")
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        RoleStrings.tr(
          language,
          "تفضيلات أخبار الصباح",
          "Préférences Fellah Matin",
          "Fellah Matin Preferences",
        )
      )
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text =
            RoleStrings.tr(
              language,
              "تُستخدم هذه التفضيلات لإبراز المواضيع المهمة لك دون إخفاء الأخبار العامة.",
              "Ces préférences mettent en avant vos sujets sans jamais masquer l'actualité générale.",
              "Preferences highlight relevant topics without hiding general news.",
            ),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
          value = preferredRegion,
          onValueChange = { preferredRegion = it },
          singleLine = true,
          label = { Text(RoleStrings.tr(language, "الولاية المفضلة (اختياري)", "Wilaya préférée", "Preferred Wilaya")) },
          modifier = Modifier.fillMaxWidth().testTag("news_prefs_region_input"),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("ALL", "AR", "FR", "EN").forEach { code ->
            FilterChip(
              selected = preferredLang == code,
              onClick = { preferredLang = code },
              label = { Text(code) },
            )
          }
        }

        FellahNewsCategory.entries.forEach { cat ->
          val checked = selectedCats.contains(cat.code)
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable {
                  if (checked) selectedCats.remove(cat.code) else selectedCats.add(cat.code)
                }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Checkbox(
              checked = checked,
              onCheckedChange = { isChecked ->
                if (isChecked) {
                  if (!selectedCats.contains(cat.code)) selectedCats.add(cat.code)
                } else {
                  selectedCats.remove(cat.code)
                }
              },
            )
            Text(text = cat.localizedTitle(language), style = MaterialTheme.typography.bodySmall)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(selectedCats.toList(), preferredRegion, preferredLang) },
        modifier = Modifier.height(48.dp).testTag("news_prefs_save_button"),
      ) {
        Text(RoleStrings.tr(language, "حفظ التفضيلات", "Enregistrer", "Save"))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
        Text(RoleStrings.tr(language, "إلغاء", "Annuler", "Cancel"))
      }
    },
  )
}

@Composable
private fun EditorialAdminConsoleDialog(
  language: AppLanguage,
  editorialQueue: List<NewsArticleRecord>,
  sources: List<NewsSourceRecord>,
  feedbackMessage: String?,
  onClearFeedback: () -> Unit,
  onSubmitArticle: (String, String, String, String, String, FellahNewsCategory, String, String, Boolean) -> Unit,
  onModerateArticle: (String, NewsModerationStatus, String?, FellahNewsCategory?) -> Unit,
  onToggleSource: (String, Boolean) -> Unit,
  onAddSource: (String, String, String, NewsSourceType, String) -> Unit,
  onRunRssIngestion: () -> Unit,
  onDismiss: () -> Unit,
) {
  var activeTab by rememberSaveable { mutableIntStateOf(0) }
  var title by rememberSaveable { mutableStateOf("") }
  var summary by rememberSaveable { mutableStateOf("") }
  var sourceName by rememberSaveable { mutableStateOf("") }
  var originalUrl by rememberSaveable { mutableStateOf("https://") }
  var publishedIso by rememberSaveable {
    mutableStateOf(FellahNewsRssIngestionService.formatIsoUtc(System.currentTimeMillis()))
  }
  var selectedCat by rememberSaveable { mutableStateOf(FellahNewsCategory.GOV_ANNOUNCEMENTS) }
  var articleLang by rememberSaveable { mutableStateOf("AR") }
  var wilaya by rememberSaveable { mutableStateOf("") }
  var publishNow by rememberSaveable { mutableStateOf(true) }

  var newSourceName by rememberSaveable { mutableStateOf("") }
  var newSourceBaseUrl by rememberSaveable { mutableStateOf("https://") }
  var newSourceFeedUrl by rememberSaveable { mutableStateOf("https://") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text =
          RoleStrings.tr(
            language,
            "وحدة الإدارة التحريرية والمصادر — Fellah Matin",
            "Console d'Administration Éditoriale",
            "Editorial Publishing & Ingestion Console",
          ),
        fontWeight = FontWeight.ExtraBold,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (!feedbackMessage.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth().clickable { onClearFeedback() },
          ) {
            Text(
              text = feedbackMessage,
              style = MaterialTheme.typography.labelMedium,
              modifier = Modifier.padding(10.dp),
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          FilterChip(
            selected = activeTab == 0,
            onClick = { activeTab = 0 },
            label = { Text(RoleStrings.tr(language, "المراجعة (${editorialQueue.size})", "File (${editorialQueue.size})", "Queue (${editorialQueue.size})")) },
            modifier = Modifier.testTag("admin_tab_queue"),
          )
          FilterChip(
            selected = activeTab == 1,
            onClick = { activeTab = 1 },
            label = { Text(RoleStrings.tr(language, "نشر خبر", "Publier", "Publish")) },
            modifier = Modifier.testTag("admin_tab_publish"),
          )
          FilterChip(
            selected = activeTab == 2,
            onClick = { activeTab = 2 },
            label = { Text(RoleStrings.tr(language, "المصادر (${sources.size})", "Sources (${sources.size})", "Sources (${sources.size})")) },
            modifier = Modifier.testTag("admin_tab_sources"),
          )
        }

        when (activeTab) {
          0 -> {
            OutlinedButton(
              onClick = onRunRssIngestion,
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_import_rss_button"),
            ) {
              Text(
                RoleStrings.tr(
                  language,
                  "جلب الخلاصات المفعلة إلى طابور المراجعة",
                  "Importer les flux RSS vers la file",
                  "Import enabled RSS feeds to review queue",
                )
              )
            }

            if (editorialQueue.isEmpty()) {
              Text(
                text = RoleStrings.tr(language, "طابور المراجعة فارغ حالياً.", "Aucun article en file.", "Review queue is empty."),
                style = MaterialTheme.typography.bodySmall,
              )
            } else {
              editorialQueue.forEach { rec ->
                Card(
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth().testTag("admin_queue_item_${rec.id}"),
                ) {
                  Column(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                  ) {
                    Text(rec.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                      "${rec.sourceName} • [${rec.moderationStatus}] • ${rec.publishedAtIso}",
                      style = MaterialTheme.typography.labelSmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Button(
                        onClick = { onModerateArticle(rec.id, NewsModerationStatus.APPROVED, null, null) },
                        modifier = Modifier.height(48.dp).testTag("admin_approve_${rec.id}"),
                      ) {
                        Text(RoleStrings.tr(language, "اعتماد ونشر", "Approuver", "Approve"))
                      }
                      OutlinedButton(
                        onClick = { onModerateArticle(rec.id, NewsModerationStatus.REJECTED, null, null) },
                        modifier = Modifier.height(48.dp).testTag("admin_reject_${rec.id}"),
                      ) {
                        Text(RoleStrings.tr(language, "رفض", "Rejeter", "Reject"))
                      }
                    }
                  }
                }
              }
            }
          }

          1 -> {
            OutlinedTextField(
              value = title,
              onValueChange = { title = it },
              label = { Text(RoleStrings.tr(language, "العنوان الأصلي", "Titre", "Headline")) },
              modifier = Modifier.fillMaxWidth().testTag("admin_input_title"),
            )
            OutlinedTextField(
              value = summary,
              onValueChange = { summary = it },
              label = { Text(RoleStrings.tr(language, "الملخص الموثق", "Résumé court", "Verified summary")) },
              modifier = Modifier.fillMaxWidth().testTag("admin_input_summary"),
            )
            OutlinedTextField(
              value = sourceName,
              onValueChange = { sourceName = it },
              label = { Text(RoleStrings.tr(language, "اسم المصدر الرسمي", "Nom de la source", "Source name")) },
              modifier = Modifier.fillMaxWidth().testTag("admin_input_source"),
            )
            OutlinedTextField(
              value = originalUrl,
              onValueChange = { originalUrl = it },
              singleLine = true,
              label = { Text("https:// URL") },
              modifier = Modifier.fillMaxWidth().testTag("admin_input_url"),
            )
            OutlinedTextField(
              value = wilaya,
              onValueChange = { wilaya = it },
              singleLine = true,
              label = { Text(RoleStrings.tr(language, "الولاية (فقط إذا ذُكرت بالمصدر)", "Wilaya vérifiée", "Verified Wilaya (optional)")) },
              modifier = Modifier.fillMaxWidth(),
            )
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              FellahNewsCategory.entries.forEach { cat ->
                FilterChip(
                  selected = selectedCat == cat,
                  onClick = { selectedCat = cat },
                  label = { Text(cat.localizedTitle(language)) },
                )
              }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(checked = publishNow, onCheckedChange = { publishNow = it })
              Text(
                RoleStrings.tr(language, "اعتماد ونشر فوري", "Publier immédiatement", "Approve & publish immediately"),
                style = MaterialTheme.typography.bodySmall,
              )
            }
            Button(
              onClick = {
                onSubmitArticle(
                  title,
                  summary,
                  sourceName,
                  originalUrl,
                  publishedIso,
                  selectedCat,
                  articleLang,
                  wilaya,
                  publishNow,
                )
              },
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_submit_article_button"),
            ) {
              Text(RoleStrings.tr(language, "حفظ المقال التحريري", "Enregistrer l'article", "Save Article"))
            }
          }

          2 -> {
            sources.forEach { src ->
              Card(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(src.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(src.feedUrl, style = MaterialTheme.typography.labelSmall)
                    if (src.lastError.isNotBlank()) {
                      Text(
                        "Error: ${src.lastError}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                      )
                    }
                  }
                  Switch(
                    checked = src.isEnabled,
                    onCheckedChange = { enabled -> onToggleSource(src.id, enabled) },
                  )
                }
              }
            }

            HorizontalDivider()
            OutlinedTextField(
              value = newSourceName,
              onValueChange = { newSourceName = it },
              singleLine = true,
              label = { Text(RoleStrings.tr(language, "اسم المصدر الجديد", "Nom source", "New source name")) },
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = newSourceBaseUrl,
              onValueChange = { newSourceBaseUrl = it },
              singleLine = true,
              label = { Text("Base https:// URL") },
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = newSourceFeedUrl,
              onValueChange = { newSourceFeedUrl = it },
              singleLine = true,
              label = { Text("RSS Feed https:// URL") },
              modifier = Modifier.fillMaxWidth(),
            )
            Button(
              onClick = {
                onAddSource(
                  newSourceName,
                  newSourceBaseUrl,
                  newSourceFeedUrl,
                  NewsSourceType.OFFICIAL_INSTITUTION,
                  "AR",
                )
              },
              modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_add_source_button"),
            ) {
              Text(RoleStrings.tr(language, "إضافة مصدر RSS موثق", "Ajouter source RSS", "Add RSS Source"))
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
        Text(RoleStrings.tr(language, "إغلاق", "Fermer", "Close"))
      }
    },
  )
}

private fun formatReadableTimestamp(epochMs: Long): String {
  if (epochMs <= 0L) return "—"
  val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
  return sdf.format(Date(epochMs))
}

private fun openVerifiedHttpsArticle(context: Context, rawUrl: String) {
  if (!FellahNewsRssIngestionService.isValidHttpsNewsUrl(rawUrl)) return
  runCatching {
    val intent =
      Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl.trim())).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(intent)
  }
}
