package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.AgriculturalParcel
import com.example.data.model.AppLanguage
import com.example.data.model.CropVarietyType
import com.example.data.model.MapInteractionTool
import com.example.data.model.MapLayerMode
import com.example.data.model.MarketListing
import com.example.data.model.MarketplaceOrder
import com.example.data.model.NormalizedPoint
import com.example.data.model.PartnerCatalogItem
import com.example.data.model.PlatformMessage
import com.example.data.model.RoleNavigationConfig
import com.example.data.model.RoleSpecificProfile
import com.example.data.model.ServiceBooking
import com.example.data.model.SoilTextureType
import com.example.data.model.TransportRequest
import com.example.data.model.UserAccountProfile
import com.example.data.model.UserRole
import com.example.data.model.WorkspaceSection
import com.example.data.network.AgriWeatherReport
import com.example.data.network.OpenMeteoAgriWeatherClient
import com.example.data.offline.FellahOfflineDatabase
import com.example.data.offline.OfflineMapCacheRepository
import com.example.data.offline.OfflineMapRegionEntity
import com.example.data.repository.FellahRepository
import com.example.data.repository.RoleAccountRepository
import com.example.util.GeoCoordinateMapper
import com.example.util.GeoCoordinateMapper.GeoLatLng
import com.example.util.NetworkConnectivityObserver
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

data class ParcelEditorState(
  val activeParcel: AgriculturalParcel = AgriculturalParcel(),
  val mapLayerMode: MapLayerMode = MapLayerMode.SATELLITE,
  val mapTool: MapInteractionTool = MapInteractionTool.VIEW,
  val language: AppLanguage = AppLanguage.AR,
  val activeSection: WorkspaceSection = WorkspaceSection.PARCEL_DELINEATION,
  val searchQuery: String = "",
  val isSaving: Boolean = false,
  val isSoilSlidersExpanded: Boolean = false,
  val showNdviHistorySheet: Boolean = false,
  val showCadastralEditDialog: Boolean = false,
  val showPublishMarketDialog: Boolean = false,
  val feedbackMessage: String? = null,
  val isErrorFeedback: Boolean = false,
  val hasLoadedInitialCloudParcel: Boolean = false,
  val operatorDisplayName: String = "Coopérative El Baraka",
  val defaultWilaya: String = "07 - بسكرة (Biskra)",
  val isNetworkConnected: Boolean = true,
  val isManualOfflineMode: Boolean = false,
  val isDownloadingOfflineRegion: Boolean = false,
  val isSyncingOfflineQueue: Boolean = false,
  val showOfflineRegionsDialog: Boolean = false,
  val preferGoogleMapsEngine: Boolean = true,
  val agriWeatherReport: AgriWeatherReport? = null,
  val isLoadingAgriWeather: Boolean = false,
  val agriWeatherError: String? = null,
  val activeRole: UserRole = UserRole.FARMER,
  val isOnboardingSaving: Boolean = false,
  val onboardingErrorMessage: String? = null,
) {
  val isEffectiveOffline: Boolean
    get() = !isNetworkConnected || isManualOfflineMode
}

@OptIn(ExperimentalCoroutinesApi::class)
class FellahViewModel(
  private val repository: FellahRepository,
  private val currentUserId: String,
  private val offlineMapCacheRepository: OfflineMapCacheRepository? = null,
  connectivityFlow: Flow<Boolean> = flowOf(true),
  private val roleRepository: RoleAccountRepository? = null,
) : ViewModel() {

  private val _editorState = MutableStateFlow(ParcelEditorState())
  val editorState: StateFlow<ParcelEditorState> = _editorState.asStateFlow()
  private var hasAttemptedLegacyFarmerAutoMigration = false
  private var hasAppliedInitialLanding = false

  val offlineRegionsState: StateFlow<List<OfflineMapRegionEntity>> =
    (offlineMapCacheRepository?.downloadedRegions ?: flowOf(emptyList())).stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  val pendingOfflineWritesCount: StateFlow<Int> =
    repository
      .observePendingOfflineWritesCount(currentUserId)
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = 0,
      )

  init {
    viewModelScope.launch {
      connectivityFlow.collect { connected ->
        _editorState.update { state -> state.copy(isNetworkConnected = connected) }
      }
    }
  }

  val userParcelsState: StateFlow<UiState<List<AgriculturalParcel>>> =
    repository
      .observeUserParcels(currentUserId)
      .map<List<AgriculturalParcel>, UiState<List<AgriculturalParcel>>> { parcels ->
        if (!_editorState.value.hasLoadedInitialCloudParcel && parcels.isNotEmpty()) {
          _editorState.update { state ->
            if (!state.hasLoadedInitialCloudParcel && state.activeParcel.id.isBlank()) {
              state.copy(
                activeParcel = parcels.first(),
                hasLoadedInitialCloudParcel = true,
              )
            } else {
              state.copy(hasLoadedInitialCloudParcel = true)
            }
          }
        } else if (!_editorState.value.hasLoadedInitialCloudParcel) {
          _editorState.update { it.copy(hasLoadedInitialCloudParcel = true) }
        }
        UiState.Success(parcels)
      }
      .catch { error ->
        Log.w(TAG, "Error observing user parcels", error)
        emit(UiState.Error(error.localizedMessage ?: "تعذر تحميل سجل القطع الفلاحية"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val marketListingsState: StateFlow<UiState<List<MarketListing>>> =
    repository
      .observeActiveMarketListings()
      .map<List<MarketListing>, UiState<List<MarketListing>>> { UiState.Success(it) }
      .catch { error ->
        Log.w(TAG, "Error observing market listings", error)
        emit(UiState.Error(error.localizedMessage ?: "تعذر تحميل إعلانات السوق العام"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val transportRequestsState: StateFlow<UiState<List<TransportRequest>>> =
    repository
      .observeUserTransportRequests(currentUserId)
      .map<List<TransportRequest>, UiState<List<TransportRequest>>> { UiState.Success(it) }
      .catch { error ->
        Log.w(TAG, "Error observing transport requests", error)
        emit(UiState.Error(error.localizedMessage ?: "تعذر تحميل طلبات النقل الفلاحي"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val serviceBookingsState: StateFlow<UiState<List<ServiceBooking>>> =
    repository
      .observeUserServiceBookings(currentUserId)
      .map<List<ServiceBooking>, UiState<List<ServiceBooking>>> { UiState.Success(it) }
      .catch { error ->
        Log.w(TAG, "Error observing service bookings", error)
        emit(UiState.Error(error.localizedMessage ?: "تعذر تحميل حجوزات الخدمات الفلاحية"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val userAccountState: StateFlow<UiState<UserAccountProfile?>> =
    (roleRepository?.observeUserAccount(currentUserId) ?: flowOf(null))
      .map<UserAccountProfile?, UiState<UserAccountProfile?>> { profile ->
        if (profile != null) {
          val resolvedRole = profile.resolvedActiveRole() ?: UserRole.FARMER
          val savedLang =
            AppLanguage.entries.firstOrNull { it.code == profile.preferredLanguage }
          _editorState.update { state ->
            val allowedSections = RoleNavigationConfig.sidebarSectionsForRole(resolvedRole)
            val nextSection =
              if (!hasAppliedInitialLanding && profile.onboardingCompleted) {
                hasAppliedInitialLanding = true
                if (resolvedRole == UserRole.FARMER) state.activeSection
                else WorkspaceSection.ROLE_DASHBOARD
              } else if (state.activeSection !in allowedSections) {
                WorkspaceSection.ROLE_DASHBOARD
              } else {
                state.activeSection
              }
            state.copy(
              activeRole = resolvedRole,
              language = savedLang ?: state.language,
              operatorDisplayName =
                profile.displayName.takeIf { it.isNotBlank() } ?: state.operatorDisplayName,
              activeSection = nextSection,
            )
          }
        }
        UiState.Success(profile)
      }
      .catch { error ->
        Log.w(TAG, "Error observing user multi-role account", error)
        emit(UiState.Error(error.localizedMessage ?: "تعذر تحميل ملف الأنشطة المهنية"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val roleProfilesState: StateFlow<Map<String, RoleSpecificProfile>> =
    (roleRepository?.observeRoleProfiles(currentUserId) ?: flowOf(emptyMap()))
      .catch { emit(emptyMap()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyMap(),
      )

  val activePartnerCatalogState: StateFlow<UiState<List<PartnerCatalogItem>>> =
    (roleRepository?.observeActivePartnerCatalog() ?: flowOf(emptyList()))
      .map<List<PartnerCatalogItem>, UiState<List<PartnerCatalogItem>>> { UiState.Success(it) }
      .catch { error ->
        emit(UiState.Error(error.localizedMessage ?: "تعذر تحميل كتالوج الشركاء"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val userPartnerCatalogState: StateFlow<UiState<List<PartnerCatalogItem>>> =
    (roleRepository?.observeUserPartnerCatalog(currentUserId) ?: flowOf(emptyList()))
      .map<List<PartnerCatalogItem>, UiState<List<PartnerCatalogItem>>> { UiState.Success(it) }
      .catch { error ->
        emit(UiState.Error(error.localizedMessage ?: "تعذر تحميل عروضي المهنية"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val userOrdersState: StateFlow<UiState<List<MarketplaceOrder>>> =
    (roleRepository?.observeUserOrders(currentUserId) ?: flowOf(emptyList()))
      .map<List<MarketplaceOrder>, UiState<List<MarketplaceOrder>>> { UiState.Success(it) }
      .catch { error ->
        emit(UiState.Error(error.localizedMessage ?: "تعذر تحميل الطلبات وعروض الأسعار"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val openRoleOrdersState: StateFlow<List<MarketplaceOrder>> =
    combine(
        userAccountState,
        _editorState.map { it.activeRole }.distinctUntilChanged(),
      ) { accState, activeRole ->
        val acc = (accState as? UiState.Success)?.data
        if (acc != null && acc.onboardingCompleted && activeRole.code in acc.assignedRoles) {
          activeRole.code
        } else {
          null
        }
      }
      .distinctUntilChanged()
      .flatMapLatest { validRoleCode ->
        if (validRoleCode != null && roleRepository != null) {
          roleRepository.observeOpenOrdersForRole(validRoleCode).catch { emit(emptyList()) }
        } else {
          flowOf(emptyList())
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  val accountMessagesState: StateFlow<List<PlatformMessage>> =
    (roleRepository?.observeAccountMessages(currentUserId) ?: flowOf(emptyList()))
      .catch { emit(emptyList()) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  init {
    // Backward-compatible migration: if an existing farmer already has saved parcels in /parcels
    // from before multi-role onboarding existed, auto-initialize their /users/{uid} profile as farmer!
    if (roleRepository != null) {
      viewModelScope.launch {
        combine(userAccountState, userParcelsState) { acc, parcels -> acc to parcels }.collect {
          (accState, parcelsState) ->
          val acc = (accState as? UiState.Success)
          val parcels = (parcelsState as? UiState.Success)?.data
          if (
            !hasAttemptedLegacyFarmerAutoMigration &&
              acc != null &&
              acc.data == null &&
              !parcels.isNullOrEmpty()
          ) {
            hasAttemptedLegacyFarmerAutoMigration = true
            roleRepository.saveOnboardingProgress(
              displayName = _editorState.value.operatorDisplayName,
              email = "",
              preferredLanguage = _editorState.value.language,
              selectedRoleCodes = listOf(UserRole.FARMER.code),
              chosenDefaultRole = UserRole.FARMER.code,
              step = 5,
              completed = true,
            )
          }
        }
      }
    }
  }

  fun setLanguage(language: AppLanguage) {
    _editorState.update { it.copy(language = language) }
    val repo = roleRepository ?: return
    viewModelScope.launch { repo.updatePreferredLanguage(language) }
  }

  fun navigateToSection(section: WorkspaceSection) {
    _editorState.update { it.copy(activeSection = section) }
  }

  fun updateSearchQuery(query: String) {
    _editorState.update { it.copy(searchQuery = query) }
  }

  fun setMapLayerMode(mode: MapLayerMode) {
    _editorState.update { it.copy(mapLayerMode = mode) }
  }

  fun toggleMapTool(tool: MapInteractionTool) {
    _editorState.update { state ->
      val next = if (state.mapTool == tool) MapInteractionTool.VIEW else tool
      state.copy(mapTool = next)
    }
  }

  fun onMapTapped(point: NormalizedPoint) {
    _editorState.update { state ->
      when (state.mapTool) {
        MapInteractionTool.PLACE_FORAGE -> {
          state.copy(
            activeParcel = state.activeParcel.copy(wellPoint = point.encode()),
            mapTool = MapInteractionTool.VIEW,
            feedbackMessage = "تم تحديث موقع نقطة السقي (Forage) بنجاح",
            isErrorFeedback = false,
          )
        }
        MapInteractionTool.DRAW_POLYGON -> {
          val currentPts = state.activeParcel.polygonPoints.map { NormalizedPoint.decode(it) }
          val updatedPts =
            if (currentPts.size >= 16) {
              currentPts.drop(1) + point
            } else {
              currentPts + point
            }
          val area = NormalizedPoint.computeAreaHectares(updatedPts)
          state.copy(
            activeParcel =
              state.activeParcel.copy(
                polygonPoints = updatedPts.map { it.encode() },
                areaHectares = area,
              )
          )
        }
        MapInteractionTool.VIEW -> state
      }
    }
  }

  fun updatePolygonVertex(index: Int, newPoint: NormalizedPoint) {
    _editorState.update { state ->
      val pts = state.activeParcel.polygonPoints.map { NormalizedPoint.decode(it) }.toMutableList()
      if (index in pts.indices) {
        pts[index] = newPoint
        val area = NormalizedPoint.computeAreaHectares(pts)
        state.copy(
          activeParcel =
            state.activeParcel.copy(
              polygonPoints = pts.map { it.encode() },
              areaHectares = area,
            )
        )
      } else {
        state
      }
    }
  }

  /**
   * Handles taps directly on live satellite map [GeoLatLng] coordinates for drawing WGS84 parcel
   * boundaries or placing the irrigation borehole (Forage) in keyless demo mode.
   */
  fun onGoogleMapLatLngTapped(latLng: GeoLatLng) {
    _editorState.update { state ->
      val center = GeoCoordinateMapper.parseCenterCoordinates(state.activeParcel.coordinatesLabel)
      val mappedNorm = GeoCoordinateMapper.latLngToNormalized(latLng, center)
      when (state.mapTool) {
        MapInteractionTool.PLACE_FORAGE -> {
          state.copy(
            activeParcel = state.activeParcel.copy(wellPoint = mappedNorm.encode()),
            mapTool = MapInteractionTool.VIEW,
            feedbackMessage =
              "تم تثبيت نقطة السقي على الخريطة الحية (${GeoCoordinateMapper.formatDecimalLabel(latLng.latitude, latLng.longitude)})",
            isErrorFeedback = false,
          )
        }
        MapInteractionTool.DRAW_POLYGON -> {
          val currentPts = state.activeParcel.polygonPoints.map { NormalizedPoint.decode(it) }
          val updatedPts =
            if (currentPts.size >= 16) currentPts.drop(1) + mappedNorm else currentPts + mappedNorm
          val latLngVertices =
            updatedPts.map { GeoCoordinateMapper.normalizedToLatLng(it, center) }
          val area = GeoCoordinateMapper.computeGeodesicAreaHectares(latLngVertices)
          state.copy(
            activeParcel =
              state.activeParcel.copy(
                polygonPoints = updatedPts.map { it.encode() },
                areaHectares = area,
              )
          )
        }
        MapInteractionTool.VIEW -> state
      }
    }
  }

  /**
   * Handles dragging a polygon vertex marker on the live satellite map in real time.
   */
  fun onGoogleMapVertexDragged(index: Int, newLatLng: GeoLatLng) {
    _editorState.update { state ->
      val center = GeoCoordinateMapper.parseCenterCoordinates(state.activeParcel.coordinatesLabel)
      val pts = state.activeParcel.polygonPoints.map { NormalizedPoint.decode(it) }.toMutableList()
      if (index in pts.indices) {
        pts[index] = GeoCoordinateMapper.latLngToNormalized(newLatLng, center)
        val latLngVertices = pts.map { GeoCoordinateMapper.normalizedToLatLng(it, center) }
        val area = GeoCoordinateMapper.computeGeodesicAreaHectares(latLngVertices)
        state.copy(
          activeParcel =
            state.activeParcel.copy(
              polygonPoints = pts.map { it.encode() },
              areaHectares = area,
            )
        )
      } else {
        state
      }
    }
  }

  fun setPreferGoogleMapsEngine(preferGoogleMaps: Boolean) {
    _editorState.update { it.copy(preferGoogleMapsEngine = preferGoogleMaps) }
  }

  fun setShowOfflineRegionsDialog(show: Boolean) {
    _editorState.update { it.copy(showOfflineRegionsDialog = show) }
  }

  fun toggleManualOfflineMode(enabled: Boolean) {
    _editorState.update { it.copy(isManualOfflineMode = enabled) }
    viewModelScope.launch {
      repository.setCloudNetworkEnabled(!enabled)
      val msg =
        if (enabled) {
          "تم تفعيل وضع العمل الميداني دون اتصال (Offline) — تُحفظ كافة التعديلات محلياً وتُزامن لاحقاً"
        } else {
          "تمت استعادة الاتصال السحابي ومزامنة السجل الفلاحي"
        }
      _editorState.update { it.copy(feedbackMessage = msg, isErrorFeedback = false) }
    }
  }

  fun syncPendingOfflineQueueNow() {
    _editorState.update { it.copy(isSyncingOfflineQueue = true, isManualOfflineMode = false) }
    viewModelScope.launch {
      repository
        .syncPendingOfflineWrites()
        .onSuccess {
          _editorState.update {
            it.copy(
              isSyncingOfflineQueue = false,
              feedbackMessage = "تمت مزامنة كافة البيانات الميدانية المحفوظة محلياً مع السحابة بنجاح",
              isErrorFeedback = false,
            )
          }
        }
        .onFailure { err ->
          _editorState.update {
            it.copy(
              isSyncingOfflineQueue = false,
              feedbackMessage = err.localizedMessage ?: "تعذرت المزامنة الفورية، ستستمر المحاولة تلقائياً",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun downloadCurrentParcelOfflineRegion() {
    val cacheRepo = offlineMapCacheRepository ?: return
    val state = _editorState.value
    val parcel = state.activeParcel
    val center = GeoCoordinateMapper.parseCenterCoordinates(parcel.coordinatesLabel)
    val regionId =
      "parcel_${parcel.ilotNumber.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase(Locale.US)}"
    val regionEntity =
      OfflineMapRegionEntity(
        id = regionId,
        regionName = "${parcel.name} (${parcel.commune})",
        wilaya = parcel.wilaya,
        parcelReference = parcel.ilotNumber,
        centerLat = center.latitude,
        centerLng = center.longitude,
        zoomLevel = 16.6f,
        radiusKm = 5.0,
        mapLayerCode = state.mapLayerMode.name,
        polygonSnapshotEncoded = parcel.polygonPoints.joinToString("|"),
        sizeMb = 14.8,
      )
    _editorState.update { it.copy(isDownloadingOfflineRegion = true) }
    viewModelScope.launch {
      delay(350L)
      cacheRepo.saveDownloadedRegion(regionEntity)
      _editorState.update {
        it.copy(
          isDownloadingOfflineRegion = false,
          feedbackMessage =
            "تم تنزيل وحفظ خريطة المنطقة (${parcel.name}) للعمل أوفلاين بدون إنترنت (14.8 MB)",
          isErrorFeedback = false,
        )
      }
    }
  }

  fun downloadPresetOfflineRegion(preset: GeoCoordinateMapper.OfflineRegionPreset) {
    val cacheRepo = offlineMapCacheRepository ?: return
    val entity =
      OfflineMapRegionEntity(
        id = preset.id,
        regionName = preset.regionName,
        wilaya = preset.wilaya,
        parcelReference = "نطاق فلاحي إقليمي (${preset.radiusKm} كم)",
        centerLat = preset.centerLat,
        centerLng = preset.centerLng,
        zoomLevel = 15.2f,
        radiusKm = preset.radiusKm,
        mapLayerCode = _editorState.value.mapLayerMode.name,
        polygonSnapshotEncoded = _editorState.value.activeParcel.polygonPoints.joinToString("|"),
        sizeMb = preset.estimatedSizeMb,
      )
    _editorState.update { it.copy(isDownloadingOfflineRegion = true) }
    viewModelScope.launch {
      delay(350L)
      cacheRepo.saveDownloadedRegion(entity)
      _editorState.update {
        it.copy(
          isDownloadingOfflineRegion = false,
          feedbackMessage = "تم تنزيل خريطة ${preset.regionName} للاستخدام أوفلاين (${preset.estimatedSizeMb} MB)",
          isErrorFeedback = false,
        )
      }
    }
  }

  fun openOfflineCachedRegion(region: OfflineMapRegionEntity) {
    val restoredPolygon =
      region.polygonSnapshotEncoded
        .split("|")
        .filter { it.contains(",") }
        .ifEmpty { _editorState.value.activeParcel.polygonPoints }
    val coordLabel = GeoCoordinateMapper.formatDecimalLabel(region.centerLat, region.centerLng)
    _editorState.update { state ->
      state.copy(
        activeParcel =
          state.activeParcel.copy(
            wilaya = region.wilaya,
            coordinatesLabel = coordLabel,
            polygonPoints = restoredPolygon,
          ),
        showOfflineRegionsDialog = false,
        activeSection = WorkspaceSection.PARCEL_DELINEATION,
        feedbackMessage = "تم فتح خريطة المنطقة المحفوظة أوفلاين: ${region.regionName}",
        isErrorFeedback = false,
      )
    }
  }

  fun deleteOfflineRegion(regionId: String) {
    val cacheRepo = offlineMapCacheRepository ?: return
    viewModelScope.launch {
      cacheRepo.deleteRegion(regionId)
      _editorState.update {
        it.copy(feedbackMessage = "تم حذف المنطقة من ذاكرة الخرائط دون اتصال", isErrorFeedback = false)
      }
    }
  }

  fun clearAllOfflineRegions() {
    val cacheRepo = offlineMapCacheRepository ?: return
    viewModelScope.launch {
      cacheRepo.clearCache()
      _editorState.update {
        it.copy(feedbackMessage = "تم تفريغ ذاكرة الخرائط المحفوظة دون اتصال", isErrorFeedback = false)
      }
    }
  }

  fun resetPolygonToDefault() {
    val defaultPoints =
      listOf("0.20,0.23", "0.74,0.20", "0.85,0.75", "0.51,0.85", "0.24,0.71")
    _editorState.update { state ->
      state.copy(
        activeParcel =
          state.activeParcel.copy(
            polygonPoints = defaultPoints,
            areaHectares = 4.82,
          ),
        feedbackMessage = "تمت استعادة المضلع القياسي للقطعة (4.82 Ha)",
        isErrorFeedback = false,
      )
    }
  }

  fun startFreshTrianglePolygon() {
    val starter = listOf("0.30,0.28", "0.70,0.30", "0.52,0.74")
    val decoded = starter.map { NormalizedPoint.decode(it) }
    _editorState.update { state ->
      state.copy(
        mapTool = MapInteractionTool.DRAW_POLYGON,
        activeParcel =
          state.activeParcel.copy(
            polygonPoints = starter,
            areaHectares = NormalizedPoint.computeAreaHectares(decoded),
          ),
        feedbackMessage = "اضغط على الخريطة لإضافة رؤوس جديدة للمضلع أو اسحب النقاط",
        isErrorFeedback = false,
      )
    }
  }

  fun updateCoordinatesFromGps(latitude: Double, longitude: Double) {
    val latDir = if (latitude >= 0) "N" else "S"
    val lonDir = if (longitude >= 0) "E" else "W"
    val formatted =
      String.format(
        Locale.US,
        "%.4f°%s %.4f°%s",
        abs(latitude),
        latDir,
        abs(longitude),
        lonDir,
      )
    _editorState.update { state ->
      state.copy(
        activeParcel = state.activeParcel.copy(coordinatesLabel = formatted),
        feedbackMessage = "تم التقاط إحداثيات GPS المباشرة: $formatted",
        isErrorFeedback = false,
      )
    }
  }

  fun selectSoilTexture(texture: SoilTextureType) {
    _editorState.update { state ->
      state.copy(activeParcel = state.activeParcel.copy(soilTexture = texture.code))
    }
  }

  fun toggleSoilSliders() {
    _editorState.update { it.copy(isSoilSlidersExpanded = !it.isSoilSlidersExpanded) }
  }

  fun updateSoilPh(ph: Float) {
    val rounded = kotlin.math.round(ph.toDouble() * 10.0) / 10.0
    _editorState.update { state ->
      state.copy(activeParcel = state.activeParcel.copy(soilPh = rounded.coerceIn(4.5, 9.5)))
    }
  }

  fun updateOrganicMatter(percent: Float) {
    val rounded = kotlin.math.round(percent.toDouble() * 100.0) / 100.0
    _editorState.update { state ->
      state.copy(
        activeParcel = state.activeParcel.copy(organicMatterPercent = rounded.coerceIn(0.1, 8.0))
      )
    }
  }

  fun updateSalinityEc(ec: Float) {
    val rounded = kotlin.math.round(ec.toDouble() * 10.0) / 10.0
    _editorState.update { state ->
      state.copy(activeParcel = state.activeParcel.copy(salinityEc = rounded.coerceIn(0.2, 10.0)))
    }
  }

  fun cycleDrainageStatus() {
    val options =
      listOf(
        "عميق > 120 سم / تصريف ممتاز",
        "متوسط 80 - 120 سم / تصريف جيد",
        "متماسك < 80 سم / يحتاج قنوات صرف",
      )
    _editorState.update { state ->
      val idx = options.indexOf(state.activeParcel.drainageStatus)
      val next = options[(idx + 1) % options.size]
      state.copy(activeParcel = state.activeParcel.copy(drainageStatus = next))
    }
  }

  fun attachLabDocument(fileName: String) {
    val cleanName = fileName.take(180)
    _editorState.update { state ->
      state.copy(
        activeParcel = state.activeParcel.copy(labDocumentName = cleanName),
        feedbackMessage = "تم إرفاق شهادة تحليل المخبر: $cleanName",
        isErrorFeedback = false,
      )
    }
  }

  fun selectMainCrop(crop: CropVarietyType) {
    _editorState.update { state ->
      state.copy(
        activeParcel =
          state.activeParcel.copy(
            mainCrop = crop.code,
            estimatedYieldTonsPerHa = crop.defaultYieldTonsHa,
          )
      )
    }
  }

  fun updateIrrigationSystem(irrigation: String) {
    _editorState.update { state ->
      state.copy(activeParcel = state.activeParcel.copy(irrigationSystem = irrigation.take(95)))
    }
  }

  fun updateEstimatedYield(yieldText: String) {
    val parsed = yieldText.replace(",", ".").toDoubleOrNull() ?: return
    _editorState.update { state ->
      state.copy(
        activeParcel =
          state.activeParcel.copy(estimatedYieldTonsPerHa = parsed.coerceIn(0.0, 500.0))
      )
    }
  }

  fun updatePlantingYear(yearText: String) {
    if (yearText.isBlank()) return
    _editorState.update { state ->
      state.copy(activeParcel = state.activeParcel.copy(plantingYear = yearText.take(55)))
    }
  }

  fun updateIntercropping(text: String) {
    _editorState.update { state ->
      state.copy(activeParcel = state.activeParcel.copy(intercropping = text.take(115)))
    }
  }

  fun updateCadastralDetails(
    name: String,
    wilaya: String,
    commune: String,
    ilotNumber: String,
    tenureType: String,
    wellDepthLabel: String,
  ) {
    _editorState.update { state ->
      state.copy(
        activeParcel =
          state.activeParcel.copy(
            name = name.ifBlank { state.activeParcel.name }.take(110),
            wilaya = wilaya.ifBlank { state.activeParcel.wilaya }.take(75),
            commune = commune.ifBlank { state.activeParcel.commune }.take(75),
            ilotNumber = ilotNumber.ifBlank { state.activeParcel.ilotNumber }.take(55),
            tenureType = tenureType.ifBlank { state.activeParcel.tenureType }.take(75),
            wellDepthLabel = wellDepthLabel.ifBlank { state.activeParcel.wellDepthLabel }.take(75),
          ),
        showCadastralEditDialog = false,
        feedbackMessage = "تم تحديث البيانات العقارية للقطعة",
        isErrorFeedback = false,
      )
    }
  }

  fun setShowNdviHistory(show: Boolean) {
    _editorState.update { it.copy(showNdviHistorySheet = show) }
  }

  fun setShowCadastralEditDialog(show: Boolean) {
    _editorState.update { it.copy(showCadastralEditDialog = show) }
  }

  fun setShowPublishMarketDialog(show: Boolean) {
    _editorState.update { it.copy(showPublishMarketDialog = show) }
  }

  fun dismissFeedback() {
    _editorState.update { it.copy(feedbackMessage = null) }
  }

  fun notifyMessage(message: String, isError: Boolean = false) {
    _editorState.update { it.copy(feedbackMessage = message, isErrorFeedback = isError) }
  }

  fun startNewParcelDraft() {
    _editorState.update { state ->
      state.copy(
        activeParcel =
          AgriculturalParcel(
            name = "قطعة فلاحية جديدة",
            ilotNumber = "Ilot N ${(100..299).random()} / P-${(10..99).random()}",
          ),
        activeSection = WorkspaceSection.PARCEL_DELINEATION,
        feedbackMessage = "تم فتح استمارة قطعة فلاحية جديدة جاهزة للترسيم",
        isErrorFeedback = false,
      )
    }
  }

  fun loadParcelIntoEditor(parcel: AgriculturalParcel) {
    _editorState.update { state ->
      state.copy(
        activeParcel = parcel,
        activeSection = WorkspaceSection.PARCEL_DELINEATION,
        feedbackMessage = "تم تحميل القطعة: ${parcel.name} (${parcel.ilotNumber})",
        isErrorFeedback = false,
      )
    }
  }

  fun saveCurrentParcel() {
    val stateSnapshot = _editorState.value
    val current = stateSnapshot.activeParcel
    val isOffline = stateSnapshot.isEffectiveOffline
    _editorState.update { it.copy(isSaving = true, feedbackMessage = null) }
    viewModelScope.launch {
      val result = repository.saveParcel(current, allowOfflineQueue = isOffline)
      result
        .onSuccess { savedId ->
          val refreshed =
            if (!isOffline) repository.getParcelById(savedId).getOrNull() else null
          _editorState.update { state ->
            state.copy(
              activeParcel = refreshed ?: state.activeParcel.copy(id = savedId, userId = currentUserId),
              isSaving = false,
              feedbackMessage =
                if (isOffline) {
                  "تم حفظ القطعة الفلاحية محلياً في وضع أوفلاين (${"%.2f".format(current.areaHectares)} Ha) — ستتم المزامنة تلقائياً عند توفر الشبكة"
                } else {
                  "تم حفظ وتثبيت القطعة الفلاحية (${"%.2f".format(current.areaHectares)} Ha) في السجل السحابي بنجاح"
                },
              isErrorFeedback = false,
            )
          }
        }
        .onFailure { error ->
          _editorState.update { state ->
            state.copy(
              isSaving = false,
              feedbackMessage = error.localizedMessage ?: "تعذر حفظ القطعة الفلاحية",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun deleteParcel(parcelId: String) {
    viewModelScope.launch {
      repository
        .deleteParcel(parcelId)
        .onSuccess {
          _editorState.update { state ->
            val resetActive =
              if (state.activeParcel.id == parcelId) AgriculturalParcel() else state.activeParcel
            state.copy(
              activeParcel = resetActive,
              feedbackMessage = "تم حذف القطعة من السجل العقاري",
              isErrorFeedback = false,
            )
          }
        }
        .onFailure { error ->
          _editorState.update {
            it.copy(
              feedbackMessage = error.localizedMessage ?: "تعذر حذف القطعة",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun publishParcelToMarket(
    operatorName: String,
    availableTons: Double,
    pricePerKgDzd: Double,
  ) {
    _editorState.update { it.copy(isSaving = true) }
    viewModelScope.launch {
      val currentParcel = _editorState.value.activeParcel
      val ensuredParcelId =
        if (currentParcel.id.isBlank()) {
          val saveRes = repository.saveParcel(currentParcel)
          if (saveRes.isFailure) {
            _editorState.update {
              it.copy(
                isSaving = false,
                feedbackMessage = "تعذر حفظ القطعة قبل النشر في السوق العام",
                isErrorFeedback = true,
              )
            }
            return@launch
          }
          saveRes.getOrThrow()
        } else {
          currentParcel.id
        }

      val listing =
        MarketListing(
          parcelId = ensuredParcelId,
          operatorName = operatorName.ifBlank { "Coopérative El Baraka" }.take(95),
          parcelReference = currentParcel.ilotNumber.take(75),
          cropType = currentParcel.mainCrop,
          wilaya = currentParcel.wilaya.take(75),
          availableTons = availableTons.coerceIn(0.1, 99999.0),
          pricePerKgDzd = pricePerKgDzd.coerceIn(1.0, 99999.0),
        )

      repository
        .publishMarketListing(listing)
        .onSuccess {
          val refreshed = repository.getParcelById(ensuredParcelId).getOrNull()
          _editorState.update { state ->
            state.copy(
              activeParcel =
                refreshed
                  ?: state.activeParcel.copy(id = ensuredParcelId, linkedToMarket = true),
              isSaving = false,
              showPublishMarketDialog = false,
              activeSection = WorkspaceSection.PUBLIC_MARKET,
              feedbackMessage = "تم ربط القطعة ونشر إعلان البيع في السوق العام بنجاح",
              isErrorFeedback = false,
            )
          }
        }
        .onFailure { error ->
          _editorState.update {
            it.copy(
              isSaving = false,
              feedbackMessage = error.localizedMessage ?: "تعذر نشر إعلان البيع",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun markMarketListingAsSold(listingId: String) {
    viewModelScope.launch {
      repository
        .markListingAsSold(listingId)
        .onSuccess {
          _editorState.update {
            it.copy(
              feedbackMessage = "تم إغلاق العرض وتسجيله كمباع (Vendu)",
              isErrorFeedback = false,
            )
          }
        }
        .onFailure { error ->
          _editorState.update {
            it.copy(
              feedbackMessage = error.localizedMessage ?: "تعذر تحديث حالة العرض",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun submitTransportRequest(
    vehicleType: String,
    originWilaya: String,
    destinationMarket: String,
    cargoTons: Double,
    scheduledDate: String,
  ) {
    val activeIlot = _editorState.value.activeParcel.ilotNumber.ifBlank { "Ilot N 108 / P-24" }
    val request =
      TransportRequest(
        parcelReference = activeIlot.take(75),
        vehicleType = vehicleType,
        originWilaya = originWilaya.ifBlank { _editorState.value.defaultWilaya }.take(75),
        destinationMarket = destinationMarket.ifBlank { "سوق الجملة الكاليتوس - الجزائر" }.take(115),
        cargoTons = cargoTons.coerceIn(0.1, 5000.0),
        scheduledDate = scheduledDate.ifBlank { "2026-11-02" }.take(38),
      )
    val isOffline = _editorState.value.isEffectiveOffline
    viewModelScope.launch {
      repository
        .createTransportRequest(request, allowOfflineQueue = isOffline)
        .onSuccess {
          _editorState.update {
            it.copy(
              feedbackMessage =
                if (isOffline) {
                  "تم حفظ طلب النقل محلياً (أوفلاين) وسيُرسل تلقائياً فور توفر الشبكة"
                } else {
                  "تم تسجيل طلب النقل الفلاحي بنجاح وجارٍ تنسيق الشاحنة"
                },
              isErrorFeedback = false,
            )
          }
        }
        .onFailure { error ->
          _editorState.update {
            it.copy(
              feedbackMessage = error.localizedMessage ?: "تعذر تسجيل طلب النقل",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun advanceTransportStatus(request: TransportRequest) {
    val nextStatus =
      when (request.status) {
        "pending" -> "in_transit"
        "in_transit" -> "delivered"
        else -> return
      }
    viewModelScope.launch {
      repository
        .updateTransportStatus(request.id, nextStatus)
        .onSuccess {
          val msg =
            if (nextStatus == "in_transit") "تم تحديث حالة الشحنة إلى: في الطريق"
            else "تم تأكيد تسليم الشحنة الفلاحية بنجاح"
          _editorState.update { it.copy(feedbackMessage = msg, isErrorFeedback = false) }
        }
        .onFailure { error ->
          _editorState.update {
            it.copy(
              feedbackMessage = error.localizedMessage ?: "تعذر تحديث حالة الشحنة",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun cancelTransportRequest(requestId: String) {
    viewModelScope.launch {
      repository
        .deleteTransportRequest(requestId)
        .onSuccess {
          _editorState.update {
            it.copy(feedbackMessage = "تم إلغاء طلب النقل الفلاحي", isErrorFeedback = false)
          }
        }
        .onFailure { error ->
          _editorState.update {
            it.copy(
              feedbackMessage = error.localizedMessage ?: "تعذر إلغاء طلب النقل",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun submitServiceBooking(
    serviceCategory: String,
    serviceTitle: String,
    wilaya: String,
    preferredDate: String,
    notes: String,
  ) {
    val activeIlot = _editorState.value.activeParcel.ilotNumber.ifBlank { "Ilot N 108 / P-24" }
    val booking =
      ServiceBooking(
        serviceCategory = serviceCategory,
        serviceTitle = serviceTitle.take(115),
        parcelReference = activeIlot.take(75),
        wilaya = wilaya.ifBlank { _editorState.value.defaultWilaya }.take(75),
        preferredDate = preferredDate.ifBlank { "2026-11-05" }.take(38),
        notes = notes.take(290),
      )
    val isOffline = _editorState.value.isEffectiveOffline
    viewModelScope.launch {
      repository
        .createServiceBooking(booking, allowOfflineQueue = isOffline)
        .onSuccess {
          _editorState.update {
            it.copy(
              feedbackMessage =
                if (isOffline) {
                  "تم حفظ حجز الخدمة محلياً (أوفلاين) وسيُرسل تلقائياً فور توفر الشبكة"
                } else {
                  "تم حجز الخدمة الفلاحية وإرسال الطلب للمصلحة المختصة"
                },
              isErrorFeedback = false,
            )
          }
        }
        .onFailure { error ->
          _editorState.update {
            it.copy(
              feedbackMessage = error.localizedMessage ?: "تعذر تسجيل حجز الخدمة",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun cancelServiceBooking(bookingId: String) {
    viewModelScope.launch {
      repository
        .deleteServiceBooking(bookingId)
        .onSuccess {
          _editorState.update {
            it.copy(feedbackMessage = "تم إلغاء حجز الخدمة الفلاحية", isErrorFeedback = false)
          }
        }
        .onFailure { error ->
          _editorState.update {
            it.copy(
              feedbackMessage = error.localizedMessage ?: "تعذر إلغاء الحجز",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun updateOperatorProfilePreferences(operatorName: String, defaultWilaya: String) {
    _editorState.update { state ->
      val cleanOp = operatorName.trim().ifBlank { state.operatorDisplayName }.take(95)
      val cleanWilaya = defaultWilaya.trim().ifBlank { state.defaultWilaya }.take(75)
      state.copy(
        operatorDisplayName = cleanOp,
        defaultWilaya = cleanWilaya,
        activeParcel = state.activeParcel.copy(wilaya = cleanWilaya),
        feedbackMessage = "تم حفظ إعدادات المستثمرة والولاية الافتراضية بنجاح",
        isErrorFeedback = false,
      )
    }
  }

  fun refreshAgriWeather() {
    val coords = _editorState.value.activeParcel.coordinatesLabel
    _editorState.update { it.copy(isLoadingAgriWeather = true, agriWeatherError = null) }
    viewModelScope.launch {
      OpenMeteoAgriWeatherClient.fetchAgriWeatherForCoordinates(coords)
        .onSuccess { report ->
          _editorState.update {
            it.copy(
              agriWeatherReport = report,
              isLoadingAgriWeather = false,
              agriWeatherError = null,
            )
          }
        }
        .onFailure { err ->
          _editorState.update {
            it.copy(
              isLoadingAgriWeather = false,
              agriWeatherError = err.localizedMessage ?: "تعذر جلب بيانات الطقس الزراعي المباشرة",
            )
          }
        }
    }
  }

  fun saveOnboardingDraft(
    displayName: String,
    email: String,
    selectedRoles: List<String>,
    defaultRole: String,
    step: Int,
  ) {
    val repo = roleRepository ?: return
    viewModelScope.launch {
      repo.saveOnboardingProgress(
        displayName = displayName,
        email = email,
        preferredLanguage = _editorState.value.language,
        selectedRoleCodes = selectedRoles,
        chosenDefaultRole = defaultRole,
        step = step,
        completed = false,
      )
    }
  }

  fun completeMultiRoleOnboarding(
    displayName: String,
    email: String,
    selectedRoles: List<String>,
    defaultRole: String,
    profilesToSave: List<RoleSpecificProfile>,
    intendedSectionAfterLogin: WorkspaceSection? = null,
  ) {
    val repo = roleRepository ?: return
    _editorState.update { it.copy(isOnboardingSaving = true, onboardingErrorMessage = null) }
    viewModelScope.launch {
      val result =
        repo.saveOnboardingProgress(
          displayName = displayName,
          email = email,
          preferredLanguage = _editorState.value.language,
          selectedRoleCodes = selectedRoles,
          chosenDefaultRole = defaultRole,
          step = 5,
          completed = true,
        )
      result
        .onSuccess { savedAccount ->
          profilesToSave.forEach { profile -> repo.saveRoleSpecificProfile(profile) }
          val resolved = savedAccount.resolvedActiveRole() ?: UserRole.FARMER
          val allowed = RoleNavigationConfig.sidebarSectionsForRole(resolved)
          val landingSection =
            intendedSectionAfterLogin?.takeIf { it in allowed }
              ?: WorkspaceSection.ROLE_DASHBOARD
          hasAppliedInitialLanding = true
          _editorState.update {
            it.copy(
              isOnboardingSaving = false,
              onboardingErrorMessage = null,
              activeRole = resolved,
              activeSection = landingSection,
              operatorDisplayName = savedAccount.displayName,
              feedbackMessage = "تم تخصيص حسابك المهني وتفعيل واجهة (${resolved.localizedTitle(it.language)}) بنجاح",
              isErrorFeedback = false,
            )
          }
        }
        .onFailure { err ->
          _editorState.update {
            it.copy(
              isOnboardingSaving = false,
              onboardingErrorMessage = err.localizedMessage ?: "تعذر حفظ إعدادات الأنشطة المهنية",
            )
          }
        }
    }
  }

  fun switchActiveRole(targetRole: UserRole) {
    val repo = roleRepository
    val allowedSections = RoleNavigationConfig.sidebarSectionsForRole(targetRole)
    _editorState.update { state ->
      val nextSec =
        if (state.activeSection in allowedSections && state.activeSection != WorkspaceSection.PARCEL_DELINEATION) {
          WorkspaceSection.ROLE_DASHBOARD
        } else if (state.activeSection !in allowedSections) {
          WorkspaceSection.ROLE_DASHBOARD
        } else {
          WorkspaceSection.ROLE_DASHBOARD
        }
      state.copy(
        activeRole = targetRole,
        activeSection = nextSec,
        feedbackMessage =
          when (state.language) {
            AppLanguage.AR -> "تم التبديل الفوري إلى فضاء: ${targetRole.titleAr}"
            AppLanguage.FR -> "Basculement vers l'espace : ${targetRole.titleFr}"
            AppLanguage.EN -> "Switched active role to: ${targetRole.titleEn}"
          },
        isErrorFeedback = false,
      )
    }
    if (repo != null) {
      viewModelScope.launch {
        repo.updateDefaultRole(targetRole.code).onFailure { err ->
          _editorState.update {
            it.copy(
              feedbackMessage = err.localizedMessage ?: "تعذر تثبيت النشاط الافتراضي",
              isErrorFeedback = true,
            )
          }
        }
      }
    }
  }

  fun addRolesToCurrentAccount(newRoleCodes: List<String>, makeFirstActive: Boolean = true) {
    val repo = roleRepository ?: return
    viewModelScope.launch {
      repo
        .addRolesToAccount(newRoleCodes, makeFirstActive = makeFirstActive)
        .onSuccess { updated ->
          val nextRole = updated.resolvedActiveRole() ?: _editorState.value.activeRole
          _editorState.update { state ->
            state.copy(
              activeRole = nextRole,
              feedbackMessage =
                when (state.language) {
                  AppLanguage.AR -> "تمت إضافة النشاط المهني الجديد وتحديث صلاحياتك بنجاح"
                  AppLanguage.FR -> "Nouvelle activité professionnelle ajoutée avec succès"
                  AppLanguage.EN -> "New professional activity added to your account"
                },
              isErrorFeedback = false,
            )
          }
        }
        .onFailure { err ->
          _editorState.update {
            it.copy(
              feedbackMessage = err.localizedMessage ?: "تعذر إضافة النشاط المهني",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun removeRoleFromCurrentAccount(roleCode: String) {
    val repo = roleRepository ?: return
    viewModelScope.launch {
      repo
        .removeRoleFromAccount(roleCode, blockIfActiveObligations = true)
        .onSuccess { updated ->
          val nextRole = updated.resolvedActiveRole() ?: UserRole.FARMER
          _editorState.update { state ->
            state.copy(
              activeRole = nextRole,
              feedbackMessage =
                when (state.language) {
                  AppLanguage.AR -> "تمت إزالة النشاط من الواجهة مع الحفاظ الكامل على سجلاتك السابقة"
                  AppLanguage.FR -> "Activité retirée avec conservation de votre historique"
                  AppLanguage.EN -> "Role removed safely while preserving historical records"
                },
              isErrorFeedback = false,
            )
          }
        }
        .onFailure { err ->
          val raw = err.message.orEmpty()
          val friendlyMsg =
            if (raw.startsWith("ACTIVE_OBLIGATIONS:")) {
              val count = raw.split(":").getOrNull(1) ?: "1"
              when (_editorState.value.language) {
                AppLanguage.AR ->
                  "تنبيه: لديك ($count) عروض نشطة أو طلبات قيد التنفيذ تحت هذا النشاط. يرجى إيقافها أو إتمامها أولاً قبل إزالة النشاط."
                AppLanguage.FR ->
                  "Attention : vous avez ($count) offres actives ou commandes en cours sous ce rôle. Mettez-les en pause ou terminez-les d'abord."
                AppLanguage.EN ->
                  "Warning: You have ($count) active offers or open orders under this role. Pause or complete them before removing the role."
              }
            } else {
              err.localizedMessage ?: "لا يمكن إزالة النشاط الوحيد المتبقي في الحساب"
            }
          _editorState.update {
            it.copy(feedbackMessage = friendlyMsg, isErrorFeedback = true)
          }
        }
    }
  }

  fun saveRoleProfileDetails(profile: RoleSpecificProfile) {
    val repo = roleRepository ?: return
    viewModelScope.launch {
      repo
        .saveRoleSpecificProfile(profile)
        .onSuccess {
          _editorState.update { state ->
            state.copy(
              feedbackMessage =
                when (state.language) {
                  AppLanguage.AR -> "تم حفظ وتحديث البطاقة المهنية للنشاط بنجاح"
                  AppLanguage.FR -> "Profil professionnel enregistré avec succès"
                  AppLanguage.EN -> "Professional role profile saved"
                },
              isErrorFeedback = false,
            )
          }
        }
        .onFailure { err ->
          _editorState.update {
            it.copy(
              feedbackMessage = err.localizedMessage ?: "تعذر حفظ البطاقة المهنية",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun publishCatalogOffer(item: PartnerCatalogItem) {
    val repo = roleRepository ?: return
    viewModelScope.launch {
      repo
        .publishPartnerCatalogItem(item)
        .onSuccess {
          _editorState.update { state ->
            state.copy(
              feedbackMessage =
                when (state.language) {
                  AppLanguage.AR -> "تم نشر عرضك المهني في المنصة بنجاح"
                  AppLanguage.FR -> "Votre offre a été publiée dans le catalogue"
                  AppLanguage.EN -> "Offer published to marketplace catalog"
                },
              isErrorFeedback = false,
            )
          }
        }
        .onFailure { err ->
          _editorState.update {
            it.copy(
              feedbackMessage = err.localizedMessage ?: "تعذر نشر العرض المهني",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun toggleCatalogOfferStatus(item: PartnerCatalogItem) {
    val repo = roleRepository ?: return
    val next = if (item.status == "active") "paused" else "active"
    viewModelScope.launch {
      repo.updatePartnerCatalogStatus(item.id, next).onSuccess {
        _editorState.update { state ->
          state.copy(
            feedbackMessage =
              if (next == "paused") "تم إيقاف العرض مؤقتًا" else "تم إعادة تفعيل العرض",
            isErrorFeedback = false,
          )
        }
      }
    }
  }

  fun deleteCatalogOffer(itemId: String) {
    val repo = roleRepository ?: return
    viewModelScope.launch {
      repo.deletePartnerCatalogItem(itemId).onSuccess {
        _editorState.update { it.copy(feedbackMessage = "تم حذف العرض المهني", isErrorFeedback = false) }
      }
    }
  }

  fun submitMarketplaceOrder(order: MarketplaceOrder) {
    val repo = roleRepository ?: return
    viewModelScope.launch {
      repo
        .createMarketplaceOrder(order)
        .onSuccess {
          _editorState.update { state ->
            state.copy(
              feedbackMessage =
                when (state.language) {
                  AppLanguage.AR -> "تم إرسال الطلب بنجاح وجارٍ إشعار المتعاملين المعنيين"
                  AppLanguage.FR -> "Demande envoyée avec succès aux opérateurs concernés"
                  AppLanguage.EN -> "Order/request submitted successfully"
                },
              isErrorFeedback = false,
            )
          }
        }
        .onFailure { err ->
          _editorState.update {
            it.copy(
              feedbackMessage = err.localizedMessage ?: "تعذر إرسال الطلب",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun submitQuotationOnOrder(
    order: MarketplaceOrder,
    quotedPriceDzd: Double,
    quoteNotes: String,
  ) {
    val repo = roleRepository ?: return
    val isBroadcastClaim = order.providerId.isBlank() && order.status == "pending"
    viewModelScope.launch {
      repo
        .submitOrderQuotation(
          orderId = order.id,
          isBroadcastClaim = isBroadcastClaim,
          quotedPriceDzd = quotedPriceDzd,
          quoteNotes = quoteNotes,
        )
        .onSuccess {
          _editorState.update {
            it.copy(feedbackMessage = "تم تقديم عرض السعر للمتعامل بنجاح", isErrorFeedback = false)
          }
        }
        .onFailure { err ->
          _editorState.update {
            it.copy(
              feedbackMessage = err.localizedMessage ?: "تعذر إرسال عرض السعر",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun updateMarketplaceOrderState(orderId: String, nextStatus: String) {
    val repo = roleRepository ?: return
    viewModelScope.launch {
      repo.updateOrderStatus(orderId, nextStatus).onSuccess {
        _editorState.update {
          it.copy(feedbackMessage = "تم تحديث حالة الطلب بنجاح", isErrorFeedback = false)
        }
      }
    }
  }

  fun sendDirectPlatformMessage(
    senderName: String,
    recipientId: String,
    recipientName: String,
    subject: String,
    body: String,
  ) {
    val repo = roleRepository ?: return
    val activeRoleCode = _editorState.value.activeRole.code
    viewModelScope.launch {
      repo
        .sendMessage(
          senderName = senderName,
          senderRole = activeRoleCode,
          recipientId = recipientId,
          recipientName = recipientName,
          subject = subject,
          body = body,
        )
        .onSuccess {
          _editorState.update {
            it.copy(feedbackMessage = "تم إرسال الرسالة المباشرة بنجاح", isErrorFeedback = false)
          }
        }
        .onFailure { err ->
          _editorState.update {
            it.copy(
              feedbackMessage = err.localizedMessage ?: "تعذر إرسال الرسالة",
              isErrorFeedback = true,
            )
          }
        }
    }
  }

  fun markPlatformMessageRead(messageId: String) {
    val repo = roleRepository ?: return
    viewModelScope.launch { repo.markMessageAsRead(messageId) }
  }

  companion object {
    private const val TAG = "FellahVM"
    private const val STOP_TIMEOUT_MILLIS = 5000L

    fun provideFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val app =
          checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing from CreationExtras" }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        // Configure durable 100 MB on-device PersistentCacheSettings before first Firestore use
        runCatching {
          db.firestoreSettings =
            FirebaseFirestoreSettings.Builder(db.firestoreSettings)
              .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                  .setSizeBytes(100L * 1024L * 1024L)
                  .build()
              )
              .build()
        }
        val offlineDb = FellahOfflineDatabase.getInstance(app)
        val offlineCacheRepo = OfflineMapCacheRepository(offlineDb.offlineMapRegionDao())
        FellahViewModel(
          repository = FellahRepository(db),
          currentUserId = currentUserId,
          offlineMapCacheRepository = offlineCacheRepo,
          connectivityFlow = NetworkConnectivityObserver.observeConnectivity(app),
          roleRepository = RoleAccountRepository(db),
        )
      }
    }
  }
}
