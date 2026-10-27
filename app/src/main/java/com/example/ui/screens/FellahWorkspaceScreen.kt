package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.WorkspaceSection
import com.example.ui.auth.HexagonShape
import com.example.ui.components.CropsAndCampaignCard
import com.example.ui.components.InteractiveSatelliteMapSection
import com.example.ui.components.SoilCharacteristicsCard
import com.example.ui.viewmodel.FellahViewModel
import com.example.util.ParcelPdfExporter
import com.google.firebase.auth.FirebaseUser
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun FellahWorkspaceScreen(
  viewModel: FellahViewModel,
  currentUser: FirebaseUser,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val editorState by viewModel.editorState.collectAsStateWithLifecycle()
  val userParcelsState by viewModel.userParcelsState.collectAsStateWithLifecycle()
  val marketListingsState by viewModel.marketListingsState.collectAsStateWithLifecycle()
  val transportRequestsState by viewModel.transportRequestsState.collectAsStateWithLifecycle()
  val serviceBookingsState by viewModel.serviceBookingsState.collectAsStateWithLifecycle()

  val layoutDirection =
    if (editorState.language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

  // 1. Real PDF Export Launcher
  val createPdfLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
      if (uri != null) {
        val res =
          ParcelPdfExporter.exportParcelFicheToUri(
            context = context,
            uri = uri,
            parcel = editorState.activeParcel,
          )
        if (res.isSuccess) {
          viewModel.notifyMessage("تم تصدير وحفظ بطاقة السجل الفلاحي (PDF) بنجاح")
        } else {
          viewModel.notifyMessage("تعذر إنشاء ملف PDF: ${res.exceptionOrNull()?.localizedMessage}", true)
        }
      }
    }

  // 2. Real Lab Document / Certificate Picker (Zero storage permission required)
  val openDocumentLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        val fileName = resolveDisplayName(context, uri) ?: "certificat_inraa.pdf"
        viewModel.attachLabDocument(fileName)
      }
    }

  // 3. Real Android GPS Permission + LocationManager Lookup
  val locationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
      val granted =
        grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      if (granted) {
        readDeviceGpsCoordinates(
          context = context,
          onCoordinates = { lat, lon -> viewModel.updateCoordinatesFromGps(lat, lon) },
          onUnavailable = {
            viewModel.notifyMessage(
              "تم منح صلاحية الموقع — بانتظار إشارة القمر الصناعي GPS على الجهاز",
              isError = false,
            )
          },
        )
      } else {
        viewModel.notifyMessage(
          "يلزم السماح بصلاحية الموقع لالتقاط إحداثيات GPS المباشرة",
          isError = true,
        )
      }
    }

  CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val isExpandedScreen = maxWidth >= 920.dp
      val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
      val scope = rememberCoroutineScope()

      if (isExpandedScreen) {
        // Canonical Tablet / Desktop / Landscape Shell with Permanent Operator Sidebar
        Row(
          modifier =
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
            modifier = Modifier.width(276.dp).fillMaxHeight(),
          ) {
            OperatorSidebarContent(
              activeSection = editorState.activeSection,
              userDisplayName = currentUser.displayName ?: "Coopérative El Baraka",
              userEmail = currentUser.email ?: "Mascara (Zone Ouest)",
              onSelectSection = { viewModel.navigateToSection(it) },
              onSignOut = onSignOut,
            )
          }

          Scaffold(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
              TopOperatorBar(
                isCompact = false,
                searchQuery = editorState.searchQuery,
                language = editorState.language,
                userDisplayName = currentUser.displayName ?: "Agriculteur",
                onOpenDrawer = {},
                onSearchChanged = viewModel::updateSearchQuery,
                onLanguageSelected = viewModel::setLanguage,
                onNewHarvest = viewModel::startNewParcelDraft,
                onSignOut = onSignOut,
              )
            },
            bottomBar = {
              Column(modifier = Modifier.fillMaxWidth()) {
                if (editorState.activeSection == WorkspaceSection.PARCEL_DELINEATION) {
                  StickyGlobalActionsDock(
                    areaHectares = editorState.activeParcel.areaHectares,
                    isSaving = editorState.isSaving,
                    onLinkToMarket = { viewModel.setShowPublishMarketDialog(true) },
                    onSaveParcel = viewModel::saveCurrentParcel,
                    includeBottomNavigationInsets = false,
                  )
                }
                FellahBottomNavigationBar(
                  activeSection = editorState.activeSection,
                  language = editorState.language,
                  onSelectSection = viewModel::navigateToSection,
                )
              }
            },
          ) { innerPadding ->
            WorkspaceBodyContent(
              isTwoColumnGrid = true,
              editorState = editorState,
              userParcelsState = userParcelsState,
              marketListingsState = marketListingsState,
              transportRequestsState = transportRequestsState,
              serviceBookingsState = serviceBookingsState,
              currentUser = currentUser,
              viewModel = viewModel,
              onSignOut = onSignOut,
              onExportPdf = {
                val safeCode =
                  editorState.activeParcel.ilotNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                createPdfLauncher.launch("Fiche_Agrologique_$safeCode.pdf")
              },
              onBrowseLabDoc = {
                openDocumentLauncher.launch(arrayOf("application/pdf", "image/*"))
              },
              onRequestLiveGps = {
                locationPermissionLauncher.launch(
                  arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                  )
                )
              },
              modifier = Modifier.padding(innerPadding),
            )
          }
        }
      } else {
        // Mobile Handheld Shell with Modal Navigation Drawer & Sticky Bottom Actions
        ModalNavigationDrawer(
          drawerState = drawerState,
          drawerContent = {
            ModalDrawerSheet(
              drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              modifier = Modifier.width(292.dp),
            ) {
              OperatorSidebarContent(
                activeSection = editorState.activeSection,
                userDisplayName = currentUser.displayName ?: "Coopérative El Baraka",
                userEmail = currentUser.email ?: "Mascara (Zone Ouest)",
                onSelectSection = { section ->
                  viewModel.navigateToSection(section)
                  scope.launch { drawerState.close() }
                },
                onSignOut = onSignOut,
              )
            }
          },
        ) {
          Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
              TopOperatorBar(
                isCompact = true,
                searchQuery = editorState.searchQuery,
                language = editorState.language,
                userDisplayName = currentUser.displayName ?: "Agriculteur",
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onSearchChanged = viewModel::updateSearchQuery,
                onLanguageSelected = viewModel::setLanguage,
                onNewHarvest = viewModel::startNewParcelDraft,
                onSignOut = onSignOut,
              )
            },
            bottomBar = {
              Column(modifier = Modifier.fillMaxWidth()) {
                if (editorState.activeSection == WorkspaceSection.PARCEL_DELINEATION) {
                  StickyGlobalActionsDock(
                    areaHectares = editorState.activeParcel.areaHectares,
                    isSaving = editorState.isSaving,
                    onLinkToMarket = { viewModel.setShowPublishMarketDialog(true) },
                    onSaveParcel = viewModel::saveCurrentParcel,
                    includeBottomNavigationInsets = false,
                  )
                }
                FellahBottomNavigationBar(
                  activeSection = editorState.activeSection,
                  language = editorState.language,
                  onSelectSection = viewModel::navigateToSection,
                )
              }
            },
          ) { innerPadding ->
            WorkspaceBodyContent(
              isTwoColumnGrid = false,
              editorState = editorState,
              userParcelsState = userParcelsState,
              marketListingsState = marketListingsState,
              transportRequestsState = transportRequestsState,
              serviceBookingsState = serviceBookingsState,
              currentUser = currentUser,
              viewModel = viewModel,
              onSignOut = onSignOut,
              onExportPdf = {
                val safeCode =
                  editorState.activeParcel.ilotNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                createPdfLauncher.launch("Fiche_Agrologique_$safeCode.pdf")
              },
              onBrowseLabDoc = {
                openDocumentLauncher.launch(arrayOf("application/pdf", "image/*"))
              },
              onRequestLiveGps = {
                locationPermissionLauncher.launch(
                  arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                  )
                )
              },
              modifier = Modifier.padding(innerPadding),
            )
          }
        }
      }
    }

    // Interactive Dialogs & Sheets
    if (editorState.showNdviHistorySheet) {
      NdviHistoryDialog(
        parcel = editorState.activeParcel,
        onDismiss = { viewModel.setShowNdviHistory(false) },
      )
    }

    if (editorState.showCadastralEditDialog) {
      CadastralEditDialog(
        parcel = editorState.activeParcel,
        onDismiss = { viewModel.setShowCadastralEditDialog(false) },
        onSave = viewModel::updateCadastralDetails,
      )
    }

    if (editorState.showPublishMarketDialog) {
      PublishMarketDialog(
        parcel = editorState.activeParcel,
        defaultOperatorName = currentUser.displayName ?: "Coopérative El Baraka",
        onDismiss = { viewModel.setShowPublishMarketDialog(false) },
        onConfirmPublish = viewModel::publishParcelToMarket,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkspaceBodyContent(
  isTwoColumnGrid: Boolean,
  editorState: com.example.ui.viewmodel.ParcelEditorState,
  userParcelsState: com.example.ui.viewmodel.UiState<List<com.example.data.model.AgriculturalParcel>>,
  marketListingsState: com.example.ui.viewmodel.UiState<List<com.example.data.model.MarketListing>>,
  transportRequestsState: com.example.ui.viewmodel.UiState<List<com.example.data.model.TransportRequest>>,
  serviceBookingsState: com.example.ui.viewmodel.UiState<List<com.example.data.model.ServiceBooking>>,
  currentUser: FirebaseUser,
  viewModel: FellahViewModel,
  onSignOut: () -> Unit,
  onExportPdf: () -> Unit,
  onBrowseLabDoc: () -> Unit,
  onRequestLiveGps: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Feedback Toast Banner
    AnimatedVisibility(visible = editorState.feedbackMessage != null) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color =
          if (editorState.isErrorFeedback) MaterialTheme.colorScheme.errorContainer
          else MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = null,
              tint =
                if (editorState.isErrorFeedback) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = editorState.feedbackMessage ?: "",
              style = MaterialTheme.typography.labelLarge,
              color =
                if (editorState.isErrorFeedback) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
          IconButton(
            onClick = viewModel::dismissFeedback,
            modifier = Modifier.size(36.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Close,
              contentDescription = "إغلاق التنبيه",
              tint =
                if (editorState.isErrorFeedback) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }

    when (editorState.activeSection) {
      WorkspaceSection.PARCEL_DELINEATION -> {
        // Top Context & Spatial Status Header matching Prototype 1:1
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.weight(1f, fill = false),
          ) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHighest,
              shadowElevation = 1.dp,
              modifier = Modifier.size(54.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Filled.MyLocation,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(28.dp),
                )
              }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text(
                text = "فضاء الفلاح • ESPACE EXPLOITANT • السجل العقاري والتقني",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text =
                  if (editorState.language == AppLanguage.FR) {
                    "Délimitation Parcellaire & Fiche Agrologique"
                  } else {
                    "${stringResource(R.string.screen_title_ar)} ${stringResource(R.string.screen_title_fr)}"
                  },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = stringResource(R.string.screen_subtitle_ar),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainer,
              modifier = Modifier.height(48.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(
                  modifier =
                    Modifier.size(10.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                  text = stringResource(R.string.wgs84_active),
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLowest,
              shadowElevation = 2.dp,
              modifier =
                Modifier.height(48.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable(onClick = onExportPdf)
                  .testTag("export_pdf_button"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.PictureAsPdf,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  text = stringResource(R.string.export_pdf),
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }
            }
          }
        }

        // Main Workspace Grid (7/5 Split on Expanded, Vertical Stack on Mobile)
        if (isTwoColumnGrid) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.Top,
          ) {
            InteractiveSatelliteMapSection(
              parcel = editorState.activeParcel,
              mapLayerMode = editorState.mapLayerMode,
              mapTool = editorState.mapTool,
              onSelectMapLayer = viewModel::setMapLayerMode,
              onToggleMapTool = viewModel::toggleMapTool,
              onMapTap = viewModel::onMapTapped,
              onVertexDrag = viewModel::updatePolygonVertex,
              onResetPolygon = viewModel::resetPolygonToDefault,
              onRedrawNewPolygon = viewModel::startFreshTrianglePolygon,
              onRequestLiveGps = onRequestLiveGps,
              onOpenCadastralEditor = { viewModel.setShowCadastralEditDialog(true) },
              onOpenNdviHistory = { viewModel.setShowNdviHistory(true) },
              modifier = Modifier.weight(0.58f),
            )

            Column(
              modifier = Modifier.weight(0.42f),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              SoilCharacteristicsCard(
                parcel = editorState.activeParcel,
                isSlidersExpanded = editorState.isSoilSlidersExpanded,
                onToggleSliders = viewModel::toggleSoilSliders,
                onSelectTexture = viewModel::selectSoilTexture,
                onPhChanged = viewModel::updateSoilPh,
                onOrganicMatterChanged = viewModel::updateOrganicMatter,
                onSalinityChanged = viewModel::updateSalinityEc,
                onCycleDrainage = viewModel::cycleDrainageStatus,
                onBrowseLabDocument = onBrowseLabDoc,
              )

              CropsAndCampaignCard(
                parcel = editorState.activeParcel,
                onSelectMainCrop = viewModel::selectMainCrop,
                onUpdateIrrigation = viewModel::updateIrrigationSystem,
                onUpdateYield = viewModel::updateEstimatedYield,
                onUpdatePlantingYear = viewModel::updatePlantingYear,
                onUpdateIntercropping = viewModel::updateIntercropping,
              )
            }
          }
        } else {
          InteractiveSatelliteMapSection(
            parcel = editorState.activeParcel,
            mapLayerMode = editorState.mapLayerMode,
            mapTool = editorState.mapTool,
            onSelectMapLayer = viewModel::setMapLayerMode,
            onToggleMapTool = viewModel::toggleMapTool,
            onMapTap = viewModel::onMapTapped,
            onVertexDrag = viewModel::updatePolygonVertex,
            onResetPolygon = viewModel::resetPolygonToDefault,
            onRedrawNewPolygon = viewModel::startFreshTrianglePolygon,
            onRequestLiveGps = onRequestLiveGps,
            onOpenCadastralEditor = { viewModel.setShowCadastralEditDialog(true) },
            onOpenNdviHistory = { viewModel.setShowNdviHistory(true) },
          )

          SoilCharacteristicsCard(
            parcel = editorState.activeParcel,
            isSlidersExpanded = editorState.isSoilSlidersExpanded,
            onToggleSliders = viewModel::toggleSoilSliders,
            onSelectTexture = viewModel::selectSoilTexture,
            onPhChanged = viewModel::updateSoilPh,
            onOrganicMatterChanged = viewModel::updateOrganicMatter,
            onSalinityChanged = viewModel::updateSalinityEc,
            onCycleDrainage = viewModel::cycleDrainageStatus,
            onBrowseLabDocument = onBrowseLabDoc,
          )

          CropsAndCampaignCard(
            parcel = editorState.activeParcel,
            onSelectMainCrop = viewModel::selectMainCrop,
            onUpdateIrrigation = viewModel::updateIrrigationSystem,
            onUpdateYield = viewModel::updateEstimatedYield,
            onUpdatePlantingYear = viewModel::updatePlantingYear,
            onUpdateIntercropping = viewModel::updateIntercropping,
          )
        }
      }

      WorkspaceSection.MY_PARCELS -> {
        MyParcelsRegistryScreen(
          parcelsState = userParcelsState,
          searchQuery = editorState.searchQuery,
          onSelectParcel = viewModel::loadParcelIntoEditor,
          onDeleteParcel = viewModel::deleteParcel,
          onNewParcel = viewModel::startNewParcelDraft,
          onBackToDelineation = {
            viewModel.navigateToSection(WorkspaceSection.PARCEL_DELINEATION)
          },
        )
      }

      WorkspaceSection.PUBLIC_MARKET -> {
        PublicMarketScreen(
          listingsState = marketListingsState,
          currentUserId = currentUser.uid,
          searchQuery = editorState.searchQuery,
          onOpenPublishModal = { viewModel.setShowPublishMarketDialog(true) },
          onMarkSold = viewModel::markMarketListingAsSold,
          onBackToDelineation = {
            viewModel.navigateToSection(WorkspaceSection.PARCEL_DELINEATION)
          },
        )
      }

      WorkspaceSection.TRANSPORT -> {
        TransportLogisticsScreen(
          transportState = transportRequestsState,
          defaultWilaya = editorState.defaultWilaya,
          activeParcelRef = editorState.activeParcel.ilotNumber,
          onSubmitTransport = viewModel::submitTransportRequest,
          onAdvanceStatus = viewModel::advanceTransportStatus,
          onCancelRequest = viewModel::cancelTransportRequest,
          onBackToDelineation = {
            viewModel.navigateToSection(WorkspaceSection.PARCEL_DELINEATION)
          },
        )
      }

      WorkspaceSection.SERVICES -> {
        AgriServicesScreen(
          bookingsState = serviceBookingsState,
          defaultWilaya = editorState.defaultWilaya,
          activeParcelRef = editorState.activeParcel.ilotNumber,
          onSubmitBooking = viewModel::submitServiceBooking,
          onCancelBooking = viewModel::cancelServiceBooking,
          onBackToDelineation = {
            viewModel.navigateToSection(WorkspaceSection.PARCEL_DELINEATION)
          },
        )
      }

      WorkspaceSection.SETTINGS -> {
        val parcelsCount =
          (userParcelsState as? com.example.ui.viewmodel.UiState.Success)?.data?.size ?: 0
        OperatorSettingsScreen(
          userDisplayName = currentUser.displayName ?: editorState.operatorDisplayName,
          userEmail = currentUser.email ?: "operator@fellah-numerique.dz",
          operatorName = editorState.operatorDisplayName,
          defaultWilaya = editorState.defaultWilaya,
          currentLanguage = editorState.language,
          currentMapLayer = editorState.mapLayerMode,
          savedParcelsCount = parcelsCount,
          onSavePreferences = viewModel::updateOperatorProfilePreferences,
          onLanguageSelected = viewModel::setLanguage,
          onMapLayerSelected = viewModel::setMapLayerMode,
          onOpenMyParcels = { viewModel.navigateToSection(WorkspaceSection.MY_PARCELS) },
          onExportCurrentParcelPdf = onExportPdf,
          onSignOut = onSignOut,
          onBackToDelineation = {
            viewModel.navigateToSection(WorkspaceSection.PARCEL_DELINEATION)
          },
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun OperatorSidebarContent(
  activeSection: WorkspaceSection,
  userDisplayName: String,
  userEmail: String,
  onSelectSection: (WorkspaceSection) -> Unit,
  onSignOut: () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(16.dp),
    verticalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
      // Brand Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          modifier =
            Modifier.size(46.dp)
              .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
              ),
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFFFEFEFE),
          shadowElevation = 1.dp,
        ) {
          Image(
            painter = painterResource(R.drawable.img_fellah_logo),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(3.dp),
          )
        }
        Column {
          Text(
            text = "Fellah Numérique",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Espace Opérateur • فضاء الفلاح",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Active Role Card
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AccountTree,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "Rôle actif:",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primary,
          ) {
            Text(
              text = "Agriculteur",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
          }
        }
      }

      // Navigation Links
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SidebarNavItem(
          icon = Icons.Filled.Dashboard,
          titleAr = WorkspaceSection.PARCEL_DELINEATION.titleAr,
          subtitleFr = WorkspaceSection.PARCEL_DELINEATION.titleFr,
          selected = activeSection == WorkspaceSection.PARCEL_DELINEATION,
          onClick = { onSelectSection(WorkspaceSection.PARCEL_DELINEATION) },
          testTag = "nav_delineation",
        )
        SidebarNavItem(
          icon = Icons.Filled.Inventory2,
          titleAr = WorkspaceSection.MY_PARCELS.titleAr,
          subtitleFr = WorkspaceSection.MY_PARCELS.titleFr,
          selected = activeSection == WorkspaceSection.MY_PARCELS,
          onClick = { onSelectSection(WorkspaceSection.MY_PARCELS) },
          testTag = "nav_my_parcels",
        )
        SidebarNavItem(
          icon = Icons.Filled.Storefront,
          titleAr = WorkspaceSection.PUBLIC_MARKET.titleAr,
          subtitleFr = WorkspaceSection.PUBLIC_MARKET.titleFr,
          selected = activeSection == WorkspaceSection.PUBLIC_MARKET,
          onClick = { onSelectSection(WorkspaceSection.PUBLIC_MARKET) },
          testTag = "nav_public_market",
        )
        SidebarNavItem(
          icon = Icons.Filled.LocalShipping,
          titleAr = WorkspaceSection.TRANSPORT.titleAr,
          subtitleFr = WorkspaceSection.TRANSPORT.titleFr,
          selected = activeSection == WorkspaceSection.TRANSPORT,
          onClick = { onSelectSection(WorkspaceSection.TRANSPORT) },
          testTag = "nav_transport",
        )
        SidebarNavItem(
          icon = Icons.Filled.Handyman,
          titleAr = WorkspaceSection.SERVICES.titleAr,
          subtitleFr = WorkspaceSection.SERVICES.titleFr,
          selected = activeSection == WorkspaceSection.SERVICES,
          onClick = { onSelectSection(WorkspaceSection.SERVICES) },
          testTag = "nav_services",
        )
        SidebarNavItem(
          icon = Icons.Filled.Settings,
          titleAr = WorkspaceSection.SETTINGS.titleAr,
          subtitleFr = WorkspaceSection.SETTINGS.titleFr,
          selected = activeSection == WorkspaceSection.SETTINGS,
          onClick = { onSelectSection(WorkspaceSection.SETTINGS) },
          testTag = "nav_settings",
        )
      }
    }

    // Bottom Assistance + User Card
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Call,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = "Assistance",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text = "+213 23 45 67 89",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = userDisplayName,
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = userEmail,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          IconButton(
            onClick = onSignOut,
            modifier = Modifier.size(48.dp).testTag("sidebar_sign_out_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = stringResource(R.string.sign_out),
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SidebarNavItem(
  icon: ImageVector,
  titleAr: String,
  subtitleFr: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint =
          if (selected) MaterialTheme.colorScheme.onPrimary
          else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
      )
      Column {
        Text(
          text = titleAr,
          style = MaterialTheme.typography.labelLarge,
          color =
            if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = subtitleFr,
          style = MaterialTheme.typography.bodySmall,
          color =
            if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun TopOperatorBar(
  isCompact: Boolean,
  searchQuery: String,
  language: AppLanguage,
  userDisplayName: String,
  onOpenDrawer: () -> Unit,
  onSearchChanged: (String) -> Unit,
  onLanguageSelected: (AppLanguage) -> Unit,
  onNewHarvest: () -> Unit,
  onSignOut: () -> Unit,
) {
  var showProfileMenu by remember { mutableStateOf(false) }

  Surface(
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    shadowElevation = 2.dp,
    modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.weight(1f),
      ) {
        if (isCompact) {
          IconButton(
            onClick = onOpenDrawer,
            modifier = Modifier.size(48.dp).testTag("open_drawer_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Menu,
              contentDescription = "القائمة الجانبية",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
          Surface(
            modifier =
              Modifier.size(38.dp)
                .border(
                  width = 1.dp,
                  color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                  shape = RoundedCornerShape(10.dp),
                ),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFEFEFE),
          ) {
            Image(
              painter = painterResource(R.drawable.img_fellah_logo),
              contentDescription = stringResource(R.string.app_name),
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize().padding(2.dp),
            )
          }
          Text(
            text = "Fellah Numérique",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        } else {
          Surface(
            modifier =
              Modifier.size(42.dp)
                .border(
                  width = 1.dp,
                  color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                  shape = RoundedCornerShape(10.dp),
                ),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFEFEFE),
            shadowElevation = 1.dp,
          ) {
            Image(
              painter = painterResource(R.drawable.img_fellah_logo),
              contentDescription = stringResource(R.string.app_name),
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize().padding(3.dp),
            )
          }
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            singleLine = true,
            placeholder = {
              Text(
                stringResource(R.string.search_placeholder),
                style = MaterialTheme.typography.bodySmall,
              )
            },
            leadingIcon = {
              Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            shape = RoundedCornerShape(10.dp),
            colors =
              OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
              ),
            modifier = Modifier.width(340.dp).testTag("top_search_input"),
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        // Language Switcher FR / AR / EN
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
          Row(modifier = Modifier.padding(2.dp)) {
            AppLanguage.entries.forEach { lang ->
              val active = language == lang
              Surface(
                shape = RoundedCornerShape(6.dp),
                color =
                  if (active) MaterialTheme.colorScheme.surfaceContainerLowest
                  else Color.Transparent,
                modifier =
                  Modifier.clip(RoundedCornerShape(6.dp))
                    .clickable { onLanguageSelected(lang) }
                    .testTag("lang_${lang.code}"),
              ) {
                Text(
                  text = lang.code,
                  style = MaterialTheme.typography.labelSmall,
                  color =
                    if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                )
              }
            }
          }
        }

        if (!isCompact) {
          Button(
            onClick = onNewHarvest,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(44.dp).testTag("top_new_harvest_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Nouvelle Récolte", style = MaterialTheme.typography.labelLarge)
          }
        } else {
          IconButton(
            onClick = onNewHarvest,
            modifier = Modifier.size(48.dp).testTag("top_new_harvest_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = "قطعة جديدة",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }

        Box {
          IconButton(
            onClick = { showProfileMenu = true },
            modifier = Modifier.size(48.dp).testTag("profile_menu_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Notifications,
              contentDescription = userDisplayName,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          DropdownMenu(
            expanded = showProfileMenu,
            onDismissRequest = { showProfileMenu = false },
          ) {
            DropdownMenuItem(
              text = { Text("المشغل: $userDisplayName") },
              onClick = { showProfileMenu = false },
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.sign_out)) },
              leadingIcon = {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
              },
              onClick = {
                showProfileMenu = false
                onSignOut()
              },
            )
          }
        }
      }
    }
  }
}

@Composable
fun FellahBottomNavigationBar(
  activeSection: WorkspaceSection,
  language: AppLanguage,
  onSelectSection: (WorkspaceSection) -> Unit,
  modifier: Modifier = Modifier,
) {
  val items =
    listOf(
      Triple(WorkspaceSection.PARCEL_DELINEATION, Icons.Filled.Map, Icons.Outlined.Map),
      Triple(WorkspaceSection.PUBLIC_MARKET, Icons.Filled.Storefront, Icons.Outlined.Storefront),
      Triple(WorkspaceSection.TRANSPORT, Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping),
      Triple(WorkspaceSection.SERVICES, Icons.Filled.Handyman, Icons.Outlined.Handyman),
      Triple(WorkspaceSection.SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings),
    )

  Surface(
    tonalElevation = 3.dp,
    shadowElevation = 8.dp,
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    modifier = modifier.fillMaxWidth().testTag("bottom_navigation_bar"),
  ) {
    NavigationBar(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      tonalElevation = 0.dp,
    ) {
      items.forEach { (section, filledIcon, outlinedIcon) ->
        val selected =
          activeSection == section ||
            (section == WorkspaceSection.PARCEL_DELINEATION &&
              activeSection == WorkspaceSection.MY_PARCELS)
        val label = section.bottomLabel(language)
        NavigationBarItem(
          selected = selected,
          onClick = { onSelectSection(section) },
          icon = {
            Icon(
              imageVector = if (selected) filledIcon else outlinedIcon,
              contentDescription = label,
            )
          },
          label = {
            Text(
              text = label,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              maxLines = 1,
            )
          },
          colors =
            NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary,
              indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
          modifier = Modifier.testTag("bottom_nav_${section.name.lowercase()}"),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StickyGlobalActionsDock(
  areaHectares: Double,
  isSaving: Boolean,
  onLinkToMarket: () -> Unit,
  onSaveParcel: () -> Unit,
  includeBottomNavigationInsets: Boolean = true,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    shadowElevation = 6.dp,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          1.dp,
          MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
          RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        )
        .let { base ->
          if (includeBottomNavigationInsets) {
            base.windowInsetsPadding(WindowInsets.navigationBars)
          } else {
            base
          }
        },
  ) {
    FlowRow(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalArrangement = Arrangement.spacedBy(10.dp),
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
              imageVector = Icons.Filled.Inventory,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(22.dp),
            )
          }
        }
        Column {
          Text(
            text = stringResource(R.string.dock_ready_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text =
              "تم تثبيت المضلع الجغرافي (${String.format(Locale.US, "%.2f", areaHectares)} Ha) بنجاح مع تسجيل كافة المؤشرات الزراعية.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onLinkToMarket,
          enabled = !isSaving,
          shape = RoundedCornerShape(10.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainer,
              contentColor = MaterialTheme.colorScheme.onSurface,
            ),
          modifier = Modifier.height(48.dp).testTag("link_market_dock_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Storefront,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.btn_link_market),
            style = MaterialTheme.typography.labelLarge,
          )
        }

        Button(
          onClick = onSaveParcel,
          enabled = !isSaving,
          shape = RoundedCornerShape(10.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier = Modifier.height(48.dp).testTag("save_parcel_dock_button"),
        ) {
          if (isSaving) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
            )
          } else {
            Icon(
              imageVector = Icons.Filled.Save,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.btn_save_parcel),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

private fun resolveDisplayName(context: Context, uri: Uri): String? {
  return runCatching {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
    }
  }.getOrNull() ?: uri.lastPathSegment
}

@SuppressLint("MissingPermission")
private fun readDeviceGpsCoordinates(
  context: Context,
  onCoordinates: (Double, Double) -> Unit,
  onUnavailable: () -> Unit,
) {
  val locationManager =
    context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: run {
      onUnavailable()
      return
    }
  val gpsLoc =
    runCatching { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
  val netLoc =
    runCatching { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }
      .getOrNull()
  val best = gpsLoc ?: netLoc
  if (best != null) {
    onCoordinates(best.latitude, best.longitude)
  } else {
    onUnavailable()
  }
}
