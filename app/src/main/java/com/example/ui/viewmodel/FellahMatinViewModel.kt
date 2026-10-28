package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.AiAssistantProvider
import com.example.data.ai.FirebaseGeminiAssistantProvider
import com.example.data.model.AppLanguage
import com.example.data.model.UserRole
import com.example.data.news.FellahMatinArticle
import com.example.data.news.FellahNewsCategory
import com.example.data.news.FellahNewsRssIngestionService
import com.example.data.news.MorningBriefingSummary
import com.example.data.news.NewsArticleRecord
import com.example.data.news.NewsFeedMode
import com.example.data.news.NewsFeedPageRequest
import com.example.data.news.NewsFeedPageResult
import com.example.data.news.NewsModerationStatus
import com.example.data.news.NewsSourceRecord
import com.example.data.news.NewsSourceType
import com.example.data.news.NewsVerificationStatus
import com.example.data.news.UserNewsPreferences
import com.example.data.offline.FellahOfflineDatabase
import com.example.data.repository.FellahNewsRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FellahMatinUiState(
  val isLoading: Boolean = false,
  val isRefreshingFeeds: Boolean = false,
  val isLoadingBriefing: Boolean = false,
  val feedMode: NewsFeedMode = NewsFeedMode.SHARED_ALL,
  val selectedCategoryCode: String? = null,
  val languageFilter: String = "ALL", // "ALL", "AR", "FR", "EN"
  val wilayaFilter: String = "",
  val currentPage: Int = 1,
  val pageSize: Int = 6,
  val allowDemonstrationGuideWhenEmpty: Boolean = true,
  val isTickerPaused: Boolean = false,
  val preferAiBriefing: Boolean = false,
  val morningBriefing: MorningBriefingSummary? = null,
  val selectedArticleForDetail: FellahMatinArticle? = null,
  val showPreferencesDialog: Boolean = false,
  val showAdminConsoleDialog: Boolean = false,
  val isEditorialAdmin: Boolean = false,
  val lastFeedError: String? = null,
  val adminFeedbackMessage: String? = null,
)

/**
 * ViewModel powering **Fellah Matin (أخبار الصباح)** across both:
 * 1. Authenticated Multi-Role Dashboards (`authenticatedUserId != null`): merges Firestore approved
 *    `/news_articles` + Room RSS cache + `/user_news_preferences` + Editorial Admin workflow.
 * 2. Public Visitor Mode (`authenticatedUserId == null`): uses Room RSS cache + zero Firestore calls.
 */
class FellahMatinViewModel(
  private val newsRepository: FellahNewsRepository,
  private val aiProvider: AiAssistantProvider?,
  private val authenticatedUserId: String?,
  autoRefreshOnInit: Boolean = true,
) : ViewModel() {

  private val _uiState = MutableStateFlow(FellahMatinUiState())
  val uiState: StateFlow<FellahMatinUiState> = _uiState.asStateFlow()

  private val _activeRoleFlow = MutableStateFlow<UserRole?>(null)
  private val _uiLanguageFlow = MutableStateFlow(AppLanguage.AR)

  private val cachedPublicArticlesFlow =
    newsRepository.observeCachedPublicArticles().catch { emit(emptyList()) }

  private val approvedFirestoreArticlesFlow =
    if (!authenticatedUserId.isNullOrBlank()) {
      newsRepository.observeApprovedFirestoreArticles().catch { emit(emptyList()) }
    } else {
      flowOf(emptyList())
    }

  val userPreferencesState: StateFlow<UserNewsPreferences?> =
    if (!authenticatedUserId.isNullOrBlank()) {
      newsRepository
        .observeUserNewsPreferences(authenticatedUserId)
        .catch { emit(null) }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5_000),
          initialValue = null,
        )
    } else {
      MutableStateFlow(null)
    }

  val editorialQueueState: StateFlow<List<NewsArticleRecord>> =
    if (!authenticatedUserId.isNullOrBlank()) {
      combine(_uiState, newsRepository.observeEditorialQueue().catch { emit(emptyList()) }) {
          ui,
          queue ->
          if (ui.isEditorialAdmin) queue else emptyList()
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5_000),
          initialValue = emptyList(),
        )
    } else {
      MutableStateFlow(emptyList())
    }

  val adminSourcesState: StateFlow<List<NewsSourceRecord>> =
    if (!authenticatedUserId.isNullOrBlank()) {
      combine(
          _uiState,
          newsRepository.observeAllNewsSourcesForAdmin().catch { emit(emptyList()) },
        ) { ui, sources ->
          if (ui.isEditorialAdmin) sources else emptyList()
        }
        .stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5_000),
          initialValue = emptyList(),
        )
    } else {
      MutableStateFlow(emptyList())
    }

  /**
   * Reactive paginated feed result recomputed immediately whenever articles, filters, role,
   * user preferences, or page number change.
   */
  val feedPageState: StateFlow<NewsFeedPageResult> =
    combine(
        approvedFirestoreArticlesFlow,
        cachedPublicArticlesFlow,
        _uiState,
        _activeRoleFlow,
        userPreferencesState,
      ) { editorial, cached, ui, activeRole, prefs ->
        val effectiveWilaya =
          ui.wilayaFilter.ifBlank {
            if (ui.feedMode == NewsFeedMode.ROLE_PRIORITY) prefs?.preferredRegion.orEmpty() else ""
          }
        val effectiveLang =
          if (ui.languageFilter != "ALL") {
            ui.languageFilter
          } else if (
            ui.feedMode == NewsFeedMode.ROLE_PRIORITY &&
              !prefs?.preferredLanguage.isNullOrBlank() &&
              prefs?.preferredLanguage != "ALL"
          ) {
            prefs?.preferredLanguage ?: "ALL"
          } else {
            "ALL"
          }

        newsRepository.mergeAndPaginateArticles(
          editorialArticles = editorial,
          rssCachedArticles = cached,
          request =
            NewsFeedPageRequest(
              page = ui.currentPage,
              pageSize = ui.pageSize,
              feedMode = ui.feedMode,
              categoryCode = ui.selectedCategoryCode,
              languageFilter = effectiveLang,
              wilayaFilter = effectiveWilaya,
              activeRole = activeRole,
              userPreferredCategories = prefs?.preferredCategories ?: emptyList(),
            ),
          allowDemonstrationFallback = ui.allowDemonstrationGuideWhenEmpty,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue =
          NewsFeedPageResult(
            items = emptyList(),
            page = 1,
            pageSize = 6,
            totalItems = 0,
            totalPages = 1,
            hasNextPage = false,
            isUsingDemonstrationFallback = false,
            availableWilayas = emptyList(),
          ),
      )

  init {
    if (!authenticatedUserId.isNullOrBlank()) {
      viewModelScope.launch {
        val isAdmin = newsRepository.checkIsEditorialAdmin()
        _uiState.update { it.copy(isEditorialAdmin = isAdmin) }
      }
    }
    if (autoRefreshOnInit) {
      refreshLiveNewsFeeds(force = false)
    }
    // Keep Morning Briefing synchronized with verified feed articles and UI language
    viewModelScope.launch {
      combine(feedPageState, _uiLanguageFlow, _uiState) { page, lang, ui ->
          Triple(page.items, lang, ui.preferAiBriefing)
        }
        .collect { (items, lang, preferAi) ->
          val briefing =
            newsRepository.getOrGenerateMorningBriefing(
              verifiedArticles = items,
              language = lang,
              aiProvider = aiProvider,
              preferAiSummary = preferAi,
            )
          _uiState.update { it.copy(morningBriefing = briefing, isLoadingBriefing = false) }
        }
    }
  }

  fun syncContext(activeRole: UserRole?, language: AppLanguage) {
    _activeRoleFlow.value = activeRole
    _uiLanguageFlow.value = language
  }

  fun setFeedMode(mode: NewsFeedMode) {
    _uiState.update { it.copy(feedMode = mode, currentPage = 1) }
  }

  fun selectCategory(categoryCode: String?) {
    _uiState.update { current ->
      val next = if (current.selectedCategoryCode == categoryCode) null else categoryCode
      current.copy(selectedCategoryCode = next, currentPage = 1)
    }
  }

  fun setLanguageFilter(langFilter: String) {
    _uiState.update { it.copy(languageFilter = langFilter, currentPage = 1) }
  }

  fun setWilayaFilter(wilaya: String) {
    _uiState.update { it.copy(wilayaFilter = wilaya, currentPage = 1) }
  }

  fun goToPage(page: Int) {
    _uiState.update { it.copy(currentPage = page.coerceAtLeast(1)) }
  }

  fun toggleTickerPaused() {
    _uiState.update { it.copy(isTickerPaused = !it.isTickerPaused) }
  }

  fun setAllowDemonstrationFallback(allow: Boolean) {
    _uiState.update { it.copy(allowDemonstrationGuideWhenEmpty = allow, currentPage = 1) }
  }

  fun togglePreferAiBriefing(preferAi: Boolean) {
    _uiState.update { it.copy(preferAiBriefing = preferAi, isLoadingBriefing = true) }
  }

  fun openArticleDetail(article: FellahMatinArticle?) {
    _uiState.update { it.copy(selectedArticleForDetail = article) }
  }

  fun setShowPreferencesDialog(show: Boolean) {
    _uiState.update { it.copy(showPreferencesDialog = show) }
  }

  fun setShowAdminConsoleDialog(show: Boolean) {
    if (show && !_uiState.value.isEditorialAdmin) return
    _uiState.update { it.copy(showAdminConsoleDialog = show) }
  }

  fun clearAdminFeedback() {
    _uiState.update { it.copy(adminFeedbackMessage = null) }
  }

  fun refreshLiveNewsFeeds(force: Boolean = true) {
    viewModelScope.launch {
      _uiState.update { it.copy(isRefreshingFeeds = true, lastFeedError = null) }
      try {
        val outcomes = newsRepository.refreshPublicRssFeeds(forceBypassCooldown = force)
        val allFailed =
          outcomes.isNotEmpty() &&
            outcomes.all { it.importedArticles.isEmpty() && it.errorMessage != null }
        val errorNote =
          if (allFailed) outcomes.firstOrNull()?.errorMessage else null
        _uiState.update {
          it.copy(
            isRefreshingFeeds = false,
            lastFeedError = errorNote,
          )
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
            isRefreshingFeeds = false,
            lastFeedError = e.localizedMessage ?: "تعذر تحديث مصادر الأخبار الخارجية حالياً.",
          )
        }
      }
    }
  }

  fun saveUserPreferences(
    preferredCategories: List<String>,
    preferredRegion: String,
    preferredLanguage: String,
  ) {
    viewModelScope.launch {
      val res =
        newsRepository.saveUserNewsPreferences(
          preferredCategories = preferredCategories,
          preferredRegion = preferredRegion,
          preferredLanguage = preferredLanguage,
        )
      if (res.isSuccess) {
        _uiState.update { it.copy(showPreferencesDialog = false) }
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Editorial Admin Actions (Protected by Firestore Security Rules + UI Gate)
  // ---------------------------------------------------------------------------

  fun adminSubmitEditorialArticle(
    title: String,
    summary: String,
    sourceName: String,
    originalUrl: String,
    publishedAtIso: String,
    category: FellahNewsCategory,
    language: String,
    wilaya: String,
    publishImmediately: Boolean,
  ) {
    if (!_uiState.value.isEditorialAdmin) return
    viewModelScope.launch {
      val res =
        newsRepository.submitOrImportArticle(
          title = title,
          summary = summary,
          sourceName = sourceName,
          originalUrl = originalUrl,
          publishedAtIso = publishedAtIso,
          category = category,
          language = language,
          wilaya = wilaya,
          moderationStatus =
            if (publishImmediately) NewsModerationStatus.APPROVED
            else NewsModerationStatus.PENDING_REVIEW,
          verificationStatus = NewsVerificationStatus.EDITORIAL_VERIFIED,
        )
      _uiState.update {
        it.copy(
          adminFeedbackMessage =
            res.fold(
              onSuccess = { "تم حفظ الخبر التحريري بنجاح." },
              onFailure = { err -> err.localizedMessage ?: "تعذر حفظ الخبر." },
            )
        )
      }
    }
  }

  fun adminModerateArticle(
    articleId: String,
    status: NewsModerationStatus,
    updatedSummary: String? = null,
    updatedCategory: FellahNewsCategory? = null,
  ) {
    if (!_uiState.value.isEditorialAdmin) return
    viewModelScope.launch {
      val res =
        newsRepository.moderateAndEditArticle(
          articleId = articleId,
          moderationStatus = status,
          updatedSummary = updatedSummary,
          updatedCategory = updatedCategory,
        )
      _uiState.update {
        it.copy(
          adminFeedbackMessage =
            res.fold(
              onSuccess = { "تم تحديث حالة المراجعة التحريرية." },
              onFailure = { err -> err.localizedMessage ?: "تعذر تحديث حالة الخبر." },
            )
        )
      }
    }
  }

  fun adminToggleNewsSource(sourceId: String, enabled: Boolean) {
    if (!_uiState.value.isEditorialAdmin) return
    viewModelScope.launch {
      newsRepository.setNewsSourceEnabled(sourceId = sourceId, isEnabled = enabled)
    }
  }

  fun adminAddRssSource(
    name: String,
    baseUrl: String,
    feedUrl: String,
    sourceType: NewsSourceType,
    language: String,
  ) {
    if (!_uiState.value.isEditorialAdmin) return
    viewModelScope.launch {
      val res =
        newsRepository.upsertNewsSource(
          name = name,
          baseUrl = baseUrl,
          feedUrl = feedUrl,
          sourceType = sourceType,
          language = language,
          isEnabled = true,
        )
      _uiState.update {
        it.copy(
          adminFeedbackMessage =
            res.fold(
              onSuccess = { "تمت إضافة المصدر الإخباري الموثق." },
              onFailure = { err -> err.localizedMessage ?: "عنوان المصدر غير صالح (يجب أن يبدأ بـ https://)." },
            )
        )
      }
    }
  }

  fun adminImportRssIntoEditorialQueue() {
    if (!_uiState.value.isEditorialAdmin) return
    viewModelScope.launch {
      val res = newsRepository.runAdminIngestionToEditorialQueue(publishImmediately = false)
      _uiState.update {
        it.copy(
          adminFeedbackMessage =
            res.fold(
              onSuccess = { count -> "تم استيراد $count مقالاً إلى طابور المراجعة التحريرية." },
              onFailure = { err -> err.localizedMessage ?: "تعذر استيراد الخلاصات." },
            )
        )
      }
    }
  }

  companion object {
    fun provideFactory(authenticatedUserId: String?): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val appContext = checkNotNull(this[APPLICATION_KEY]).applicationContext
        val repository =
          if (authenticatedUserId.isNullOrBlank()) {
            // Visitor mode: strictly local Room cache + permitted RSS ingestion (ZERO Firestore)
            FellahNewsRepository(
              db = null,
              auth = null,
              cachedNewsDao = FellahOfflineDatabase.getInstance(appContext).cachedNewsArticleDao(),
              rssIngestionService = FellahNewsRssIngestionService(),
            )
          } else {
            FellahNewsRepository(appContext)
          }
        val aiProvider =
          if (authenticatedUserId.isNullOrBlank()) null
          else FirebaseGeminiAssistantProvider(appContext)
        FellahMatinViewModel(
          newsRepository = repository,
          aiProvider = aiProvider,
          authenticatedUserId = authenticatedUserId,
        )
      }
    }
  }
}
