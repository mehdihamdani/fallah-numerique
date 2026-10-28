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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Newspaper
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.ai.FellahPlatformDataSnapshot
import com.example.data.model.AppLanguage
import com.example.data.model.RoleNavigationConfig
import com.example.data.model.UserRole
import com.example.data.model.WorkspaceSection
import com.example.ui.auth.HexagonShape
import com.example.ui.components.CropsAndCampaignCard
import com.example.ui.components.InteractiveSatelliteMapSection
import com.example.ui.components.OfflineRegionsManagerDialog
import com.example.ui.components.SoilCharacteristicsCard
import com.example.ui.viewmodel.FellahAiViewModel
import com.example.ui.viewmodel.FellahMatinViewModel
import com.example.ui.viewmodel.FellahViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.ParcelPdfExporter
import com.example.util.RoleStrings
import com.google.firebase.auth.FirebaseUser
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun FellahWorkspaceScreen(
  viewModel: FellahViewModel,
  currentUser: FirebaseUser,
  onSignOut: () -> Unit,
  customAiViewModel: FellahAiViewModel? = null,
  customMatinViewModel: FellahMatinViewModel? = null,
) {
  val context = LocalContext.current
  val editorState by viewModel.editorState.collectAsStateWithLifecycle()
  val userParcelsState by viewModel.userParcelsState.collectAsStateWithLifecycle()
  val marketListingsState by viewModel.marketListingsState.collectAsStateWithLifecycle()
  val transportRequestsState by viewModel.transportRequestsState.collectAsStateWithLifecycle()
  val serviceBookingsState by viewModel.serviceBookingsState.collectAsStateWithLifecycle()
  val offlineRegions by viewModel.offlineRegionsState.collectAsStateWithLifecycle()
  val pendingOfflineWrites by viewModel.pendingOfflineWritesCount.collectAsStateWithLifecycle()
  val userAccountState by viewModel.userAccountState.collectAsStateWithLifecycle()
  val roleProfiles by viewModel.roleProfilesState.collectAsStateWithLifecycle()
  val activePartnerCatalogState by viewModel.activePartnerCatalogState.collectAsStateWithLifecycle()
  val userPartnerCatalogState by viewModel.userPartnerCatalogState.collectAsStateWithLifecycle()
  val userOrdersState by viewModel.userOrdersState.collectAsStateWithLifecycle()
  val openRoleOrders by viewModel.openRoleOrdersState.collectAsStateWithLifecycle()
  val accountMessages by viewModel.accountMessagesState.collectAsStateWithLifecycle()

  val assignedRoles =
    ((userAccountState as? UiState.Success)?.data?.parsedAssignedRoles())
      ?.takeIf { it.isNotEmpty() } ?: listOf(editorState.activeRole)

  val aiViewModel: FellahAiViewModel =
    customAiViewModel
      ?: viewModel(
        key = "fellah_ai_${currentUser.uid}",
        factory = FellahAiViewModel.provideFactory(authenticatedUserId = currentUser.uid),
      )
  val matinViewModel: FellahMatinViewModel =
    customMatinViewModel
      ?: viewModel(
        key = "fellah_matin_${currentUser.uid}",
        factory = FellahMatinViewModel.provideFactory(authenticatedUserId = currentUser.uid),
      )
  var showAiModalSheet by rememberSaveable { mutableStateOf(false) }

  val activeMarketListingsList =
    (marketListingsState as? UiState.Success)?.data ?: emptyList()
  val activePartnerCatalogList =
    (activePartnerCatalogState as? UiState.Success)?.data ?: emptyList()

  val aiPlatformSnapshot =
    remember(
      currentUser.uid,
      editorState.activeRole,
      assignedRoles,
      activeMarketListingsList,
      activePartnerCatalogList,
      editorState.activeParcel,
      editorState.agriWeatherReport,
      editorState.defaultWilaya,
    ) {
      FellahPlatformDataSnapshot(
        isAuthenticated = true,
        callerUserId = currentUser.uid,
        activeRole = editorState.activeRole,
        assignedRoles = assignedRoles,
        activeMarketListings = activeMarketListingsList,
        activePartnerCatalog = activePartnerCatalogList,
        ownActiveParcel = editorState.activeParcel,
        verifiedWeatherReport = editorState.agriWeatherReport,
        defaultWilaya = editorState.defaultWilaya,
      )
    }

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
              activeRole = editorState.activeRole,
              assignedRoles = assignedRoles,
              language = editorState.language,
              userDisplayName = currentUser.displayName ?: "Coopérative El Baraka",
              userEmail = currentUser.email ?: "Mascara (Zone Ouest)",
              onSelectSection = { viewModel.navigateToSection(it) },
              onSwitchRole = viewModel::switchActiveRole,
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
                activeRole = editorState.activeRole,
                assignedRoles = assignedRoles,
                userDisplayName = currentUser.displayName ?: "Agriculteur",
                isEffectiveOffline = editorState.isEffectiveOffline,
                pendingOfflineWrites = pendingOfflineWrites,
                onOpenOfflineManager = { viewModel.setShowOfflineRegionsDialog(true) },
                onOpenDrawer = {},
                onSearchChanged = viewModel::updateSearchQuery,
                onLanguageSelected = viewModel::setLanguage,
                onSwitchRole = viewModel::switchActiveRole,
                onManageRolesInSettings = { viewModel.navigateToSection(WorkspaceSection.SETTINGS) },
                onOpenFellahAi = { showAiModalSheet = true },
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
                  activeRole = editorState.activeRole,
                )
              }
            },
          ) { innerPadding ->
            WorkspaceBodyContent(
              isTwoColumnGrid = true,
              editorState = editorState,
              assignedRoles = assignedRoles,
              roleProfiles = roleProfiles,
              activePartnerCatalogState = activePartnerCatalogState,
              userPartnerCatalogState = userPartnerCatalogState,
              userOrdersState = userOrdersState,
              openRoleOrders = openRoleOrders,
              accountMessages = accountMessages,
              userParcelsState = userParcelsState,
              marketListingsState = marketListingsState,
              transportRequestsState = transportRequestsState,
              serviceBookingsState = serviceBookingsState,
              offlineRegions = offlineRegions,
              pendingOfflineWrites = pendingOfflineWrites,
              currentUser = currentUser,
              viewModel = viewModel,
              aiViewModel = aiViewModel,
              matinViewModel = matinViewModel,
              aiPlatformSnapshot = aiPlatformSnapshot,
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
                activeRole = editorState.activeRole,
                assignedRoles = assignedRoles,
                language = editorState.language,
                userDisplayName = currentUser.displayName ?: "Coopérative El Baraka",
                userEmail = currentUser.email ?: "Mascara (Zone Ouest)",
                onSelectSection = { section ->
                  viewModel.navigateToSection(section)
                  scope.launch { drawerState.close() }
                },
                onSwitchRole = { role ->
                  viewModel.switchActiveRole(role)
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
                activeRole = editorState.activeRole,
                assignedRoles = assignedRoles,
                userDisplayName = currentUser.displayName ?: "Agriculteur",
                isEffectiveOffline = editorState.isEffectiveOffline,
                pendingOfflineWrites = pendingOfflineWrites,
                onOpenOfflineManager = { viewModel.setShowOfflineRegionsDialog(true) },
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onSearchChanged = viewModel::updateSearchQuery,
                onLanguageSelected = viewModel::setLanguage,
                onSwitchRole = viewModel::switchActiveRole,
                onManageRolesInSettings = { viewModel.navigateToSection(WorkspaceSection.SETTINGS) },
                onOpenFellahAi = { showAiModalSheet = true },
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
                  activeRole = editorState.activeRole,
                )
              }
            },
          ) { innerPadding ->
            WorkspaceBodyContent(
              isTwoColumnGrid = false,
              editorState = editorState,
              assignedRoles = assignedRoles,
              roleProfiles = roleProfiles,
              activePartnerCatalogState = activePartnerCatalogState,
              userPartnerCatalogState = userPartnerCatalogState,
              userOrdersState = userOrdersState,
              openRoleOrders = openRoleOrders,
              accountMessages = accountMessages,
              userParcelsState = userParcelsState,
              marketListingsState = marketListingsState,
              transportRequestsState = transportRequestsState,
              serviceBookingsState = serviceBookingsState,
              offlineRegions = offlineRegions,
              pendingOfflineWrites = pendingOfflineWrites,
              currentUser = currentUser,
              viewModel = viewModel,
              aiViewModel = aiViewModel,
              matinViewModel = matinViewModel,
              aiPlatformSnapshot = aiPlatformSnapshot,
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

    if (editorState.showOfflineRegionsDialog) {
      OfflineRegionsManagerDialog(
        currentParcelName = editorState.activeParcel.name,
        isEffectiveOffline = editorState.isEffectiveOffline,
        isManualOfflineMode = editorState.isManualOfflineMode,
        isDownloading = editorState.isDownloadingOfflineRegion,
        isSyncingQueue = editorState.isSyncingOfflineQueue,
        pendingWritesCount = pendingOfflineWrites,
        downloadedRegions = offlineRegions,
        onToggleManualOffline = viewModel::toggleManualOfflineMode,
        onSyncPendingNow = viewModel::syncPendingOfflineQueueNow,
        onDownloadCurrentParcel = viewModel::downloadCurrentParcelOfflineRegion,
        onDownloadPresetRegion = viewModel::downloadPresetOfflineRegion,
        onOpenCachedRegion = viewModel::openOfflineCachedRegion,
        onDeleteRegion = viewModel::deleteOfflineRegion,
        onDismiss = { viewModel.setShowOfflineRegionsDialog(false) },
      )
    }

    if (showAiModalSheet) {
      FellahAiModalSheet(
        aiViewModel = aiViewModel,
        language = editorState.language,
        activeRole = editorState.activeRole,
        platformSnapshot = aiPlatformSnapshot,
        onDismiss = { showAiModalSheet = false },
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkspaceBodyContent(
  isTwoColumnGrid: Boolean,
  editorState: com.example.ui.viewmodel.ParcelEditorState,
  assignedRoles: List<UserRole>,
  roleProfiles: Map<String, com.example.data.model.RoleSpecificProfile>,
  activePartnerCatalogState: UiState<List<com.example.data.model.PartnerCatalogItem>>,
  userPartnerCatalogState: UiState<List<com.example.data.model.PartnerCatalogItem>>,
  userOrdersState: UiState<List<com.example.data.model.MarketplaceOrder>>,
  openRoleOrders: List<com.example.data.model.MarketplaceOrder>,
  accountMessages: List<com.example.data.model.PlatformMessage>,
  userParcelsState: com.example.ui.viewmodel.UiState<List<com.example.data.model.AgriculturalParcel>>,
  marketListingsState: com.example.ui.viewmodel.UiState<List<com.example.data.model.MarketListing>>,
  transportRequestsState: com.example.ui.viewmodel.UiState<List<com.example.data.model.TransportRequest>>,
  serviceBookingsState: com.example.ui.viewmodel.UiState<List<com.example.data.model.ServiceBooking>>,
  offlineRegions: List<com.example.data.offline.OfflineMapRegionEntity>,
  pendingOfflineWrites: Int,
  currentUser: FirebaseUser,
  viewModel: FellahViewModel,
  aiViewModel: FellahAiViewModel,
  matinViewModel: FellahMatinViewModel,
  aiPlatformSnapshot: FellahPlatformDataSnapshot,
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
              preferGoogleMapsEngine = editorState.preferGoogleMapsEngine,
              isEffectiveOffline = editorState.isEffectiveOffline,
              downloadedRegionsCount = offlineRegions.size,
              onToggleMapEngine = viewModel::setPreferGoogleMapsEngine,
              onGoogleMapLatLngTap = viewModel::onGoogleMapLatLngTapped,
              onGoogleMapVertexDrag = viewModel::onGoogleMapVertexDragged,
              onOpenOfflineManager = { viewModel.setShowOfflineRegionsDialog(true) },
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
            preferGoogleMapsEngine = editorState.preferGoogleMapsEngine,
            isEffectiveOffline = editorState.isEffectiveOffline,
            downloadedRegionsCount = offlineRegions.size,
            onToggleMapEngine = viewModel::setPreferGoogleMapsEngine,
            onGoogleMapLatLngTap = viewModel::onGoogleMapLatLngTapped,
            onGoogleMapVertexDrag = viewModel::onGoogleMapVertexDragged,
            onOpenOfflineManager = { viewModel.setShowOfflineRegionsDialog(true) },
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
            val backTarget =
              if (editorState.activeRole == UserRole.FARMER) WorkspaceSection.PARCEL_DELINEATION
              else WorkspaceSection.ROLE_DASHBOARD
            viewModel.navigateToSection(backTarget)
          },
          partnerCatalogState = activePartnerCatalogState,
          activeRole = editorState.activeRole,
          language = editorState.language,
          requesterDisplayName = currentUser.displayName ?: editorState.operatorDisplayName,
          onSubmitMarketOrder = viewModel::submitMarketplaceOrder,
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
            val backTarget =
              if (editorState.activeRole == UserRole.FARMER) WorkspaceSection.PARCEL_DELINEATION
              else WorkspaceSection.ROLE_DASHBOARD
            viewModel.navigateToSection(backTarget)
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
            val backTarget =
              if (editorState.activeRole == UserRole.FARMER) WorkspaceSection.PARCEL_DELINEATION
              else WorkspaceSection.ROLE_DASHBOARD
            viewModel.navigateToSection(backTarget)
          },
          marketListingsState = marketListingsState,
          agriWeatherReport = editorState.agriWeatherReport,
          isLoadingAgriWeather = editorState.isLoadingAgriWeather,
          agriWeatherError = editorState.agriWeatherError,
          onRefreshAgriWeather = viewModel::refreshAgriWeather,
          onNavigateToTransportSection = {
            viewModel.navigateToSection(WorkspaceSection.TRANSPORT)
          },
          onNavigateToMarketSection = {
            viewModel.navigateToSection(WorkspaceSection.PUBLIC_MARKET)
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
            val backTarget =
              if (editorState.activeRole == UserRole.FARMER) WorkspaceSection.PARCEL_DELINEATION
              else WorkspaceSection.ROLE_DASHBOARD
            viewModel.navigateToSection(backTarget)
          },
          isEffectiveOffline = editorState.isEffectiveOffline,
          isManualOfflineMode = editorState.isManualOfflineMode,
          downloadedRegionsCount = offlineRegions.size,
          downloadedRegionsMb = offlineRegions.sumOf { it.sizeMb },
          pendingWritesCount = pendingOfflineWrites,
          onToggleManualOffline = viewModel::toggleManualOfflineMode,
          onOpenOfflineMapsManager = { viewModel.setShowOfflineRegionsDialog(true) },
          onSyncPendingWrites = viewModel::syncPendingOfflineQueueNow,
          onClearOfflineCache = viewModel::clearAllOfflineRegions,
          assignedRoles = assignedRoles,
          activeRole = editorState.activeRole,
          roleProfiles = roleProfiles,
          onSwitchActiveRole = viewModel::switchActiveRole,
          onAddRoles = { codes -> viewModel.addRolesToCurrentAccount(codes, makeFirstActive = true) },
          onRemoveRole = viewModel::removeRoleFromCurrentAccount,
          onSaveRoleProfile = viewModel::saveRoleProfileDetails,
        )
      }

      WorkspaceSection.ROLE_DASHBOARD -> {
        RoleDashboardScreen(
          activeRole = editorState.activeRole,
          assignedRoles = assignedRoles,
          language = editorState.language,
          operatorDisplayName = currentUser.displayName ?: editorState.operatorDisplayName,
          roleProfile = roleProfiles[editorState.activeRole.code],
          parcels = (userParcelsState as? UiState.Success)?.data ?: emptyList(),
          marketListings = (marketListingsState as? UiState.Success)?.data ?: emptyList(),
          partnerCatalog = (activePartnerCatalogState as? UiState.Success)?.data ?: emptyList(),
          myCatalogItems = (userPartnerCatalogState as? UiState.Success)?.data ?: emptyList(),
          myOrders = (userOrdersState as? UiState.Success)?.data ?: emptyList(),
          openRoleOrders = openRoleOrders,
          transportRequests = (transportRequestsState as? UiState.Success)?.data ?: emptyList(),
          serviceBookings = (serviceBookingsState as? UiState.Success)?.data ?: emptyList(),
          messages = accountMessages,
          currentUserId = currentUser.uid,
          onNavigateToSection = viewModel::navigateToSection,
          onSwitchRole = viewModel::switchActiveRole,
          onOpenPublishHarvestDialog = { viewModel.setShowPublishMarketDialog(true) },
          matinViewModel = matinViewModel,
        )
      }

      WorkspaceSection.PARTNER_CATALOG -> {
        PartnerCatalogScreen(
          activeRole = editorState.activeRole,
          language = editorState.language,
          providerDisplayName = currentUser.displayName ?: editorState.operatorDisplayName,
          defaultWilaya = editorState.defaultWilaya,
          myCatalogState = userPartnerCatalogState,
          onPublishItem = viewModel::publishCatalogOffer,
          onToggleStatus = viewModel::toggleCatalogOfferStatus,
          onDeleteItem = viewModel::deleteCatalogOffer,
          onBackToDashboard = { viewModel.navigateToSection(WorkspaceSection.ROLE_DASHBOARD) },
        )
      }

      WorkspaceSection.ORDERS_AND_REQUESTS -> {
        OrdersAndQuotesScreen(
          activeRole = editorState.activeRole,
          language = editorState.language,
          currentUserId = currentUser.uid,
          requesterDisplayName = currentUser.displayName ?: editorState.operatorDisplayName,
          defaultWilaya = editorState.defaultWilaya,
          myOrdersState = userOrdersState,
          openRoleOrders = openRoleOrders,
          onCreateOrder = viewModel::submitMarketplaceOrder,
          onSubmitQuote = viewModel::submitQuotationOnOrder,
          onUpdateOrderStatus = viewModel::updateMarketplaceOrderState,
          onBackToDashboard = { viewModel.navigateToSection(WorkspaceSection.ROLE_DASHBOARD) },
        )
      }

      WorkspaceSection.MESSAGES_INBOX -> {
        SharedMessagesScreen(
          language = editorState.language,
          currentUserId = currentUser.uid,
          senderDisplayName = currentUser.displayName ?: editorState.operatorDisplayName,
          messages = accountMessages,
          onSendMessage = { recId, recName, subj, body ->
            viewModel.sendDirectPlatformMessage(
              senderName = currentUser.displayName ?: editorState.operatorDisplayName,
              recipientId = recId,
              recipientName = recName,
              subject = subj,
              body = body,
            )
          },
          onMarkRead = viewModel::markPlatformMessageRead,
          onBackToDashboard = { viewModel.navigateToSection(WorkspaceSection.ROLE_DASHBOARD) },
        )
      }

      WorkspaceSection.FELLAH_AI -> {
        FellahAiAssistantPanel(
          aiViewModel = aiViewModel,
          language = editorState.language,
          activeRole = editorState.activeRole,
          platformSnapshot = aiPlatformSnapshot,
          isOverlaySheet = false,
          onCloseOrBack = { viewModel.navigateToSection(WorkspaceSection.ROLE_DASHBOARD) },
        )
      }

      WorkspaceSection.FELLAH_MATIN -> {
        FellahMatinFullScreen(
          matinViewModel = matinViewModel,
          language = editorState.language,
          activeRole = editorState.activeRole,
          isAuthenticated = true,
          verifiedMarketListings =
            (marketListingsState as? UiState.Success)?.data ?: emptyList(),
          verifiedWeatherReport = editorState.agriWeatherReport,
          isLoadingWeather = editorState.isLoadingAgriWeather,
          weatherError = editorState.agriWeatherError,
          onRefreshWeather = viewModel::refreshAgriWeather,
          onBack = { viewModel.navigateToSection(WorkspaceSection.ROLE_DASHBOARD) },
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun OperatorSidebarContent(
  activeSection: WorkspaceSection,
  activeRole: UserRole,
  assignedRoles: List<UserRole>,
  language: AppLanguage,
  userDisplayName: String,
  userEmail: String,
  onSelectSection: (WorkspaceSection) -> Unit,
  onSwitchRole: (UserRole) -> Unit,
  onSignOut: () -> Unit,
) {
  var showSidebarRoleMenu by remember { mutableStateOf(false) }
  val sidebarSections = RoleNavigationConfig.sidebarSectionsForRole(activeRole)

  Column(
    modifier =
      Modifier.fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .verticalScroll(rememberScrollState())
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
            text = activeRole.localizedTitle(language),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Active Role Switcher Card
      Box {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHighest,
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .clickable { showSidebarRoleMenu = true }
              .testTag("sidebar_role_switcher_card"),
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
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
              Text(
                text = RoleStrings.activeRoleLabel(language),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.primary,
            ) {
              Text(
                text = activeRole.localizedTitle(language),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
        }

        DropdownMenu(
          expanded = showSidebarRoleMenu,
          onDismissRequest = { showSidebarRoleMenu = false },
        ) {
          assignedRoles.forEach { r ->
            DropdownMenuItem(
              text = { Text(r.localizedTitle(language)) },
              leadingIcon = { Icon(RoleStrings.iconForRole(r), contentDescription = null) },
              onClick = {
                showSidebarRoleMenu = false
                if (r != activeRole) onSwitchRole(r)
              },
            )
          }
          DropdownMenuItem(
            text = { Text(RoleStrings.addAnotherRoleLabel(language)) },
            onClick = {
              showSidebarRoleMenu = false
              onSelectSection(WorkspaceSection.SETTINGS)
            },
          )
        }
      }

      // Role-Adapted Navigation Links
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        sidebarSections.forEach { sec ->
          SidebarNavItem(
            icon = filledIconForSection(sec),
            titleAr = sec.localizedTitle(language),
            subtitleFr = sec.titleFr,
            selected = activeSection == sec,
            onClick = { onSelectSection(sec) },
            testTag = "nav_${sec.name.lowercase()}",
          )
        }
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
  activeRole: UserRole,
  assignedRoles: List<UserRole>,
  userDisplayName: String,
  isEffectiveOffline: Boolean,
  pendingOfflineWrites: Int,
  onOpenOfflineManager: () -> Unit,
  onOpenDrawer: () -> Unit,
  onSearchChanged: (String) -> Unit,
  onLanguageSelected: (AppLanguage) -> Unit,
  onSwitchRole: (UserRole) -> Unit,
  onManageRolesInSettings: () -> Unit,
  onOpenFellahAi: () -> Unit,
  onNewHarvest: () -> Unit,
  onSignOut: () -> Unit,
) {
  var showProfileMenu by remember { mutableStateOf(false) }
  var showRoleSwitcherMenu by remember { mutableStateOf(false) }

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
              Modifier.size(36.dp)
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
            modifier = Modifier.width(280.dp).testTag("top_search_input"),
          )
        }

        // Prominent Header Role Switcher Pill
        Box {
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .clickable { showRoleSwitcherMenu = true }
                .testTag("top_role_switcher_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = RoleStrings.iconForRole(activeRole),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = activeRole.localizedTitle(language),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Icon(
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
              )
            }
          }

          DropdownMenu(
            expanded = showRoleSwitcherMenu,
            onDismissRequest = { showRoleSwitcherMenu = false },
          ) {
            DropdownMenuItem(
              text = {
                Text(
                  text = RoleStrings.switchRoleMenuTitle(language),
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                )
              },
              onClick = {},
              enabled = false,
            )
            assignedRoles.forEach { roleOption ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = roleOption.localizedTitle(language),
                    fontWeight = if (roleOption == activeRole) FontWeight.Bold else FontWeight.Normal,
                  )
                },
                leadingIcon = {
                  Icon(RoleStrings.iconForRole(roleOption), contentDescription = null)
                },
                onClick = {
                  showRoleSwitcherMenu = false
                  if (roleOption != activeRole) {
                    onSwitchRole(roleOption)
                  }
                },
                modifier = Modifier.testTag("switcher_option_${roleOption.code}"),
              )
            }
            HorizontalDivider()
            DropdownMenuItem(
              text = {
                Text(
                  text = RoleStrings.addAnotherRoleLabel(language),
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold,
                )
              },
              onClick = {
                showRoleSwitcherMenu = false
                onManageRolesInSettings()
              },
              modifier = Modifier.testTag("switcher_add_role_option"),
            )
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        // Non-intrusive Fellah AI Modal Sheet Launcher (preserves current screen task)
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.tertiaryContainer,
          modifier =
            Modifier.heightIn(min = 48.dp)
              .clip(RoundedCornerShape(50))
              .clickable(onClick = onOpenFellahAi)
              .testTag("open_fellah_ai_sheet_button"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription =
                RoleStrings.tr(
                  language,
                  ar = "مساعد فلاح الذكي Fellah AI",
                  fr = "Assistant Fellah AI",
                  en = "Fellah AI Assistant",
                ),
              tint = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "Fellah AI",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
          }
        }

        // Offline / Cloud Sync Status Pill
        Surface(
          shape = RoundedCornerShape(50),
          color =
            if (isEffectiveOffline) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.primaryContainer,
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .clickable(onClick = onOpenOfflineManager)
              .testTag("top_bar_offline_status_pill"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector =
                if (isEffectiveOffline) Icons.Filled.CloudOff else Icons.Filled.CloudDone,
              contentDescription =
                if (isEffectiveOffline) stringResource(R.string.offline_mode_active_chip)
                else stringResource(R.string.online_mode_active_chip),
              tint =
                if (isEffectiveOffline) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(15.dp),
            )
            if (!isCompact || isEffectiveOffline || pendingOfflineWrites > 0) {
              Text(
                text =
                  when {
                    isEffectiveOffline && pendingOfflineWrites > 0 ->
                      "أوفلاين ($pendingOfflineWrites)"
                    isEffectiveOffline -> "أوفلاين"
                    pendingOfflineWrites > 0 -> "مزامنة ($pendingOfflineWrites)"
                    else -> "متصل"
                  },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color =
                  if (isEffectiveOffline) MaterialTheme.colorScheme.onSecondaryContainer
                  else MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }

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

        if (activeRole == UserRole.FARMER) {
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
  activeRole: UserRole? = null,
) {
  val targetSections =
    if (activeRole != null) {
      RoleNavigationConfig.bottomBarSectionsForRole(activeRole)
    } else {
      listOf(
        WorkspaceSection.PARCEL_DELINEATION,
        WorkspaceSection.PUBLIC_MARKET,
        WorkspaceSection.TRANSPORT,
        WorkspaceSection.SERVICES,
        WorkspaceSection.SETTINGS,
      )
    }
  val items =
    targetSections.map { sec ->
      Triple(sec, filledIconForSection(sec), outlinedIconForSection(sec))
    }

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

private fun filledIconForSection(section: WorkspaceSection): ImageVector =
  when (section) {
    WorkspaceSection.ROLE_DASHBOARD -> Icons.Filled.Dashboard
    WorkspaceSection.PARCEL_DELINEATION -> Icons.Filled.Map
    WorkspaceSection.MY_PARCELS -> Icons.Filled.Inventory2
    WorkspaceSection.PUBLIC_MARKET -> Icons.Filled.Storefront
    WorkspaceSection.PARTNER_CATALOG -> Icons.Filled.Inventory2
    WorkspaceSection.ORDERS_AND_REQUESTS -> Icons.Filled.Assignment
    WorkspaceSection.TRANSPORT -> Icons.Filled.LocalShipping
    WorkspaceSection.SERVICES -> Icons.Filled.Handyman
    WorkspaceSection.MESSAGES_INBOX -> Icons.Filled.Mail
    WorkspaceSection.SETTINGS -> Icons.Filled.Settings
    WorkspaceSection.FELLAH_AI -> Icons.Filled.AutoAwesome
    WorkspaceSection.FELLAH_MATIN -> Icons.Filled.Newspaper
  }

private fun outlinedIconForSection(section: WorkspaceSection): ImageVector =
  when (section) {
    WorkspaceSection.ROLE_DASHBOARD -> Icons.Outlined.Dashboard
    WorkspaceSection.PARCEL_DELINEATION -> Icons.Outlined.Map
    WorkspaceSection.MY_PARCELS -> Icons.Outlined.Inventory2
    WorkspaceSection.PUBLIC_MARKET -> Icons.Outlined.Storefront
    WorkspaceSection.PARTNER_CATALOG -> Icons.Outlined.Inventory2
    WorkspaceSection.ORDERS_AND_REQUESTS -> Icons.Outlined.Assignment
    WorkspaceSection.TRANSPORT -> Icons.Outlined.LocalShipping
    WorkspaceSection.SERVICES -> Icons.Outlined.Handyman
    WorkspaceSection.MESSAGES_INBOX -> Icons.Outlined.Mail
    WorkspaceSection.SETTINGS -> Icons.Outlined.Settings
    WorkspaceSection.FELLAH_AI -> Icons.Outlined.AutoAwesome
    WorkspaceSection.FELLAH_MATIN -> Icons.Outlined.Newspaper
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
