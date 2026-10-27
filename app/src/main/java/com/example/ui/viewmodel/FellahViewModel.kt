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
import com.example.data.model.NormalizedPoint
import com.example.data.model.ServiceBooking
import com.example.data.model.SoilTextureType
import com.example.data.model.TransportRequest
import com.example.data.model.WorkspaceSection
import com.example.data.repository.FellahRepository
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
)

class FellahViewModel(
  private val repository: FellahRepository,
  private val currentUserId: String,
) : ViewModel() {

  private val _editorState = MutableStateFlow(ParcelEditorState())
  val editorState: StateFlow<ParcelEditorState> = _editorState.asStateFlow()

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

  fun setLanguage(language: AppLanguage) {
    _editorState.update { it.copy(language = language) }
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
    val current = _editorState.value.activeParcel
    _editorState.update { it.copy(isSaving = true, feedbackMessage = null) }
    viewModelScope.launch {
      val result = repository.saveParcel(current)
      result
        .onSuccess { savedId ->
          val refreshed = repository.getParcelById(savedId).getOrNull()
          _editorState.update { state ->
            state.copy(
              activeParcel = refreshed ?: state.activeParcel.copy(id = savedId, userId = currentUserId),
              isSaving = false,
              feedbackMessage =
                "تم حفظ وتثبيت القطعة الفلاحية (${"%.2f".format(current.areaHectares)} Ha) في السجل السحابي بنجاح",
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
    viewModelScope.launch {
      repository
        .createTransportRequest(request)
        .onSuccess {
          _editorState.update {
            it.copy(
              feedbackMessage = "تم تسجيل طلب النقل الفلاحي بنجاح وجارٍ تنسيق الشاحنة",
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
    viewModelScope.launch {
      repository
        .createServiceBooking(booking)
        .onSuccess {
          _editorState.update {
            it.copy(
              feedbackMessage = "تم حجز الخدمة الفلاحية وإرسال الطلب للمصلحة المختصة",
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

  companion object {
    private const val TAG = "FellahVM"
    private const val STOP_TIMEOUT_MILLIS = 5000L

    fun provideFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val app =
          checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing from CreationExtras" }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        FellahViewModel(
          repository = FellahRepository(db),
          currentUserId = currentUserId,
        )
      }
    }
  }
}
