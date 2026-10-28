package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AgriServiceCategory
import com.example.data.model.AgriServiceDomain
import com.example.data.model.CropVarietyType
import com.example.data.model.MarketListing
import com.example.data.model.ServiceBooking
import com.example.data.network.AgriWeatherReport
import com.example.ui.viewmodel.UiState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgriServicesScreen(
  bookingsState: UiState<List<ServiceBooking>>,
  defaultWilaya: String,
  activeParcelRef: String,
  onSubmitBooking: (String, String, String, String, String) -> Unit,
  onCancelBooking: (String) -> Unit,
  onBackToDelineation: () -> Unit,
  modifier: Modifier = Modifier,
  marketListingsState: UiState<List<MarketListing>> = UiState.Success(emptyList()),
  agriWeatherReport: AgriWeatherReport? = null,
  isLoadingAgriWeather: Boolean = false,
  agriWeatherError: String? = null,
  onRefreshAgriWeather: () -> Unit = {},
  onNavigateToTransportSection: () -> Unit = {},
  onNavigateToMarketSection: () -> Unit = {},
) {
  BackHandler(onBack = onBackToDelineation)

  var selectedDomainFilter by remember { mutableStateOf<AgriServiceDomain?>(null) }
  var selectedCategory by remember { mutableStateOf<AgriServiceCategory?>(null) }
  var showMarketPricesDialog by remember { mutableStateOf(false) }
  var showAgriWeatherDialog by remember { mutableStateOf(false) }

  val visibleDomains =
    remember(selectedDomainFilter) {
      if (selectedDomainFilter == null) AgriServiceDomain.entries
      else listOf(selectedDomainFilter!!)
    }

  Column(
    modifier = modifier.fillMaxWidth().testTag("agri_services_catalog_root"),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Screen Header
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = stringResource(R.string.services_screen_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = stringResource(R.string.services_screen_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // Domain Filter Chips (All + 5 Domains)
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = selectedDomainFilter == null,
        onClick = { selectedDomainFilter = null },
        label = { Text("جميع الأقسام (${AgriServiceCategory.primaryCatalogEntries.size})") },
        leadingIcon =
          if (selectedDomainFilter == null) {
            {
              Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            }
          } else null,
        modifier = Modifier.testTag("service_domain_filter_all"),
      )

      AgriServiceDomain.entries.forEach { domain ->
        val selected = selectedDomainFilter == domain
        FilterChip(
          selected = selected,
          onClick = { selectedDomainFilter = if (selected) null else domain },
          label = { Text(domain.titleAr) },
          leadingIcon = {
            Icon(
              imageVector = domainIcon(domain),
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
          },
          modifier = Modifier.testTag("service_domain_filter_${domain.code}"),
        )
      }
    }

    // Categorized Agricultural Services Sections
    visibleDomains.forEach { domain ->
      val domainServices = AgriServiceCategory.byDomain(domain)
      if (domainServices.isNotEmpty()) {
        ServiceDomainSection(
          domain = domain,
          services = domainServices,
          onSelectService = { service ->
            when (service) {
              AgriServiceCategory.MARKET_PRICES_SERVICE -> showMarketPricesDialog = true
              AgriServiceCategory.AGRI_WEATHER_SERVICE -> {
                onRefreshAgriWeather()
                showAgriWeatherDialog = true
              }
              else -> selectedCategory = service
            }
          },
          onSecondaryShortcut = { service ->
            if (service == AgriServiceCategory.AGRICULTURAL_TRANSPORT_SERVICE) {
              onNavigateToTransportSection()
            } else if (service == AgriServiceCategory.MARKET_PRICES_SERVICE) {
              onNavigateToMarketSection()
            }
          },
        )
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

    // Active Bookings Section
    Text(
      text = "حجوزاتي وطلبات الخدمات الفلاحية المسجلة:",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )

    when (bookingsState) {
      UiState.Loading -> {
        Box(
          modifier = Modifier.fillMaxWidth().padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      }
      is UiState.Error -> {
        Text(text = bookingsState.message, color = MaterialTheme.colorScheme.error)
      }
      is UiState.Success -> {
        if (bookingsState.data.isEmpty()) {
          EmptyLogisticsPlaceholder(
            icon = Icons.Filled.Handyman,
            title = "لا توجد حجوزات خدمات نشطة",
            subtitle = "اختر أي خدمة من أقسام الإنتاج الزراعي، المستلزمات، التبريد، النقل، أو الاستشارات أعلاه لحجز تدخل مباشر لقطعتك الفلاحية.",
            actionLabel = "طلب حرث وتسوية الأرض",
            onAction = { selectedCategory = AgriServiceCategory.PLOWING_LEVELING },
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bookingsState.data.forEach { booking ->
              val cat = AgriServiceCategory.fromCode(booking.serviceCategory)
              Card(
                shape = RoundedCornerShape(12.dp),
                colors =
                  CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                  ),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = MaterialTheme.colorScheme.secondaryContainer,
                      modifier = Modifier.size(40.dp),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = serviceIcon(cat),
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(20.dp),
                        )
                      }
                    }
                    Column {
                      Text(
                        text = booking.serviceTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text = "${cat.domain.titleAr} • ${booking.parcelReference} • ${booking.wilaya} • الموعد: ${booking.preferredDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      if (booking.notes.isNotBlank()) {
                        Text(
                          text = "التفاصيل: ${booking.notes}",
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.primary,
                        )
                      }
                    }
                  }
                  IconButton(
                    onClick = { onCancelBooking(booking.id) },
                    modifier = Modifier.size(48.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.DeleteOutline,
                      contentDescription = "إلغاء الحجز",
                      tint = MaterialTheme.colorScheme.error,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  val activeService = selectedCategory
  if (activeService != null) {
    BookServiceDialog(
      service = activeService,
      defaultWilaya = defaultWilaya,
      activeParcelRef = activeParcelRef,
      onDismiss = { selectedCategory = null },
      onConfirm = { wilaya, date, notes ->
        onSubmitBooking(activeService.code, activeService.titleAr, wilaya, date, notes)
        selectedCategory = null
      },
    )
  }

  if (showMarketPricesDialog) {
    MarketPricesAdvisoryDialog(
      marketListingsState = marketListingsState,
      defaultWilaya = defaultWilaya,
      activeParcelRef = activeParcelRef,
      onDismiss = { showMarketPricesDialog = false },
      onOpenMarketTab = {
        showMarketPricesDialog = false
        onNavigateToMarketSection()
      },
      onSubscribePriceAlert = { notes ->
        onSubmitBooking(
          AgriServiceCategory.MARKET_PRICES_SERVICE.code,
          AgriServiceCategory.MARKET_PRICES_SERVICE.titleAr,
          defaultWilaya,
          "تنبيه يومي فوري",
          notes,
        )
        showMarketPricesDialog = false
      },
    )
  }

  if (showAgriWeatherDialog) {
    AgriWeatherStationDialog(
      weatherReport = agriWeatherReport,
      isLoading = isLoadingAgriWeather,
      errorMessage = agriWeatherError,
      defaultWilaya = defaultWilaya,
      activeParcelRef = activeParcelRef,
      onRefresh = onRefreshAgriWeather,
      onDismiss = { showAgriWeatherDialog = false },
      onSubscribeFrostWindAlert = { notes ->
        onSubmitBooking(
          AgriServiceCategory.AGRI_WEATHER_SERVICE.code,
          AgriServiceCategory.AGRI_WEATHER_SERVICE.titleAr,
          defaultWilaya,
          "متابعة موسمية مستمرة",
          notes,
        )
        showAgriWeatherDialog = false
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceDomainSection(
  domain: AgriServiceDomain,
  services: List<AgriServiceCategory>,
  onSelectService: (AgriServiceCategory) -> Unit,
  onSecondaryShortcut: (AgriServiceCategory) -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth().testTag("domain_section_${domain.code}"),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Section Banner Header
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceContainer,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(34.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = domainIcon(domain),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          Column {
            Text(
              text = domain.titleAr,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = domain.subtitleFr,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
          Text(
            text = "${services.size} خدمات",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }
    }

    // Cards inside this Domain
    services.forEach { service ->
      Card(
        shape = RoundedCornerShape(16.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
          ),
        modifier =
          Modifier.fillMaxWidth()
            .border(
              1.dp,
              MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
              RoundedCornerShape(16.dp),
            )
            .testTag("service_card_${service.code}"),
      ) {
        FlowRow(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.weight(1f, fill = false),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.secondaryContainer,
              modifier = Modifier.size(48.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = serviceIcon(service),
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp),
                )
              }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
              ) {
                Text(
                  text = service.providerBadge,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
              }
              Text(
                text = service.titleAr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = service.descriptionAr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (service == AgriServiceCategory.AGRICULTURAL_TRANSPORT_SERVICE) {
              OutlinedButton(
                onClick = { onSecondaryShortcut(service) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp).testTag("open_transport_tab_from_services"),
              ) {
                Icon(
                  Icons.Filled.LocalShipping,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text("أسطول النقل")
              }
            }
            Button(
              onClick = { onSelectService(service) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("book_service_${service.code}"),
            ) {
              Text(service.actionLabelAr, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BookServiceDialog(
  service: AgriServiceCategory,
  defaultWilaya: String,
  activeParcelRef: String,
  onDismiss: () -> Unit,
  onConfirm: (String, String, String) -> Unit,
) {
  var wilaya by remember(defaultWilaya) { mutableStateOf(defaultWilaya) }
  var dateText by remember { mutableStateOf("2026-11-05") }
  var specOrQuantity by remember(service) { mutableStateOf("") }
  var notes by remember(service) { mutableStateOf("") }

  val quantityFieldLabel =
    when (service) {
      AgriServiceCategory.PLOWING_LEVELING,
      AgriServiceCategory.HARVESTING_PICKING -> "المساحة المستهدفة (هكتار)"
      AgriServiceCategory.TRACTOR_MACHINERY_RENTAL -> "نوع الآلة ومدة الكراء (أيام / ساعات)"
      AgriServiceCategory.IRRIGATION_INSTALL_MAINT -> "نوع شبكة السقي (تقطير / رش محوري / صيانة)"
      AgriServiceCategory.FARM_LABOR_SUPPLY -> "عدد العمال المطلوب ونوع الأشغال"
      AgriServiceCategory.SEEDS_SEEDLINGS -> "الصنف والكمية المطلوبة (قنطار / عدد الشتلات)"
      AgriServiceCategory.FERTILIZERS_SUPPLY -> "نوع السماد والكمية (قنطار NPK / عضوي)"
      AgriServiceCategory.COLD_STORAGE_WAREHOUSES -> "الكمية للتخزين (طن) ودرجة التبريد"
      AgriServiceCategory.AGRICULTURAL_TRANSPORT_SERVICE -> "الحمولة (طن) ووجهة التسليم"
      AgriServiceCategory.AGRONOMIST_CONSULTATION -> "المحصول وموضوع الاستشارة الميدانية"
      else -> "تفاصيل إضافية للقطعة أو العينات"
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text(service.titleAr, fontWeight = FontWeight.ExtraBold)
        Text(
          text = "${service.domain.titleAr} • ${service.providerBadge}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "القطعة المرجعية: $activeParcelRef",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = wilaya,
          onValueChange = { wilaya = it },
          label = { Text("الولاية / المحيط الفلاحي") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = dateText,
          onValueChange = { dateText = it },
          label = { Text("التاريخ المفضل للتنفيذ أو التسليم") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = specOrQuantity,
          onValueChange = { specOrQuantity = it },
          label = { Text(quantityFieldLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("service_spec_quantity_input"),
        )
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("ملاحظات إضافية (اختياري)") },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val combinedNotes =
            listOf(specOrQuantity.trim(), notes.trim())
              .filter { it.isNotEmpty() }
              .joinToString(" — ")
          onConfirm(wilaya, dateText, combinedNotes)
        },
        modifier = Modifier.testTag("confirm_service_booking_button"),
      ) {
        Text("تأكيد الطلب")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
  )
}

@Composable
private fun MarketPricesAdvisoryDialog(
  marketListingsState: UiState<List<MarketListing>>,
  defaultWilaya: String,
  activeParcelRef: String,
  onDismiss: () -> Unit,
  onOpenMarketTab: () -> Unit,
  onSubscribePriceAlert: (String) -> Unit,
) {
  val activeListings =
    (marketListingsState as? UiState.Success)?.data ?: emptyList()

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
          Text("أسعار السوق الفلاحية اليومية", fontWeight = FontWeight.ExtraBold)
          Text(
            text = "مرصد الأسعار المرجعية وعروض السوق العام • $defaultWilaya",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "متوسط الأسعار المرجعية وأحدث العروض النشطة في المنصة (دج / كغ):",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
        )

        CropVarietyType.entries.forEach { crop ->
          val matching = activeListings.filter { it.cropType == crop.code }
          val avgPrice =
            if (matching.isNotEmpty()) {
              matching.map { it.pricePerKgDzd }.average()
            } else {
              crop.defaultPriceKgDzd
            }
          val totalTons = matching.sumOf { it.availableTons }

          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = crop.titleAr,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text =
                    if (matching.isNotEmpty()) {
                      "${matching.size} عروض حية (${String.format(Locale.US, "%.1f", totalTons)} طن متاح)"
                    } else {
                      "السعر المرجعي لأسواق الجملة (${crop.subtitleFr})"
                    },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text(
                  text = "${String.format(Locale.US, "%.0f", avgPrice)} دج/كغ",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
              }
            }
          }
        }

        FilledTonalButton(
          onClick = onOpenMarketTab,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().height(46.dp),
        ) {
          Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("الانتقال إلى السوق العام الفلاحي", fontWeight = FontWeight.Bold)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSubscribePriceAlert("تفعيل نشرة وتنبيهات أسعار السوق اليومية للقطعة $activeParcelRef")
        },
        modifier = Modifier.testTag("subscribe_market_prices_btn"),
      ) {
        Text("تفعيل تنبيه الأسعار")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
  )
}

@Composable
private fun AgriWeatherStationDialog(
  weatherReport: AgriWeatherReport?,
  isLoading: Boolean,
  errorMessage: String?,
  defaultWilaya: String,
  activeParcelRef: String,
  onRefresh: () -> Unit,
  onDismiss: () -> Unit,
  onSubscribeFrostWindAlert: (String) -> Unit,
) {
  LaunchedEffect(Unit) {
    if (weatherReport == null && !isLoading) {
      onRefresh()
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
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
          Icon(Icons.Filled.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Column {
            Text("الطقس الزراعي الميداني", fontWeight = FontWeight.ExtraBold)
            Text(
              text = "$defaultWilaya • القطعة $activeParcelRef",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        IconButton(onClick = onRefresh, modifier = Modifier.size(40.dp)) {
          Icon(Icons.Filled.Refresh, contentDescription = "تحديث النشرة الجوية")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (isLoading) {
          Box(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              CircularProgressIndicator()
              Text(
                text = "جارٍ جلب الرصد الجوي الفلاحي المباشر للإحداثيات...",
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }
        } else if (weatherReport != null) {
          // Current Field Metrics
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
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
                  Icon(
                    Icons.Filled.Thermostat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                  Text(
                    text = "${String.format(Locale.US, "%.1f", weatherReport.currentTempC)}°C",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
                Text(
                  text = "إحداثيات: ${weatherReport.coordinatesLabel}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                WeatherStatBadge("الرطوبة", "${weatherReport.relativeHumidityPercent}%")
                WeatherStatBadge(
                  "الرياح",
                  "${String.format(Locale.US, "%.0f", weatherReport.windSpeedKmh)} كم/سا",
                )
                WeatherStatBadge(
                  "التبخر ET0",
                  "${String.format(Locale.US, "%.1f", weatherReport.dailyEt0Mm)} ملم",
                )
                WeatherStatBadge(
                  "رطوبة التربة",
                  "${String.format(Locale.US, "%.0f", weatherReport.soilMoisturePercent)}%",
                )
              }
            }
          }

          // Agronomic Recommendation
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Filled.Air,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = weatherReport.irrigationAdviceAr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }

          // 5-Day Forecast
          Text(
            text = "توقعات 5 أيام القادمة ومؤشر الاحتياج المائي (ET0):",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
          )
          weatherReport.forecastDays.forEach { day ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceContainer,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = day.date,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "${String.format(Locale.US, "%.0f", day.minTempC)}° / ${String.format(Locale.US, "%.0f", day.maxTempC)}°C",
                  style = MaterialTheme.typography.bodySmall,
                )
                Text(
                  text = "أمطار ${String.format(Locale.US, "%.1f", day.precipitationMm)} ملم • ET0 ${String.format(Locale.US, "%.1f", day.et0Mm)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        } else {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text =
                errorMessage
                  ?: "تعذر الاتصال بمحطة الرصد الجوي حالياً (وضع أوفلاين). يمكنك تفعيل اشتراك التنبيهات الجوية الميدانية أدناه.",
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSubscribeFrostWindAlert("تفعيل تنبيهات الصقيع والرياح وجدولة السقي للقطعة $activeParcelRef")
        },
        modifier = Modifier.testTag("subscribe_agri_weather_btn"),
      ) {
        Text("تفعيل تنبيهات الطقس والسقي")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
  )
}

@Composable
private fun WeatherStatBadge(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
  }
}

private fun domainIcon(domain: AgriServiceDomain): ImageVector =
  when (domain) {
    AgriServiceDomain.AGRICULTURAL_PRODUCTION -> Icons.Filled.Agriculture
    AgriServiceDomain.EQUIPMENT_AND_SUPPLIES -> Icons.Filled.Inventory2
    AgriServiceDomain.POST_HARVEST -> Icons.Filled.AcUnit
    AgriServiceDomain.TRADE_AND_TRANSPORT -> Icons.Filled.LocalShipping
    AgriServiceDomain.CONSULTING_AND_DIGITAL -> Icons.Filled.SupportAgent
  }

private fun serviceIcon(category: AgriServiceCategory): ImageVector =
  when (category) {
    AgriServiceCategory.PLOWING_LEVELING -> Icons.Filled.Landscape
    AgriServiceCategory.TRACTOR_MACHINERY_RENTAL -> Icons.Filled.Agriculture
    AgriServiceCategory.IRRIGATION_INSTALL_MAINT -> Icons.Filled.WaterDrop
    AgriServiceCategory.FARM_LABOR_SUPPLY -> Icons.Filled.Groups
    AgriServiceCategory.HARVESTING_PICKING -> Icons.Filled.Grass
    AgriServiceCategory.SEEDS_SEEDLINGS -> Icons.Filled.Spa
    AgriServiceCategory.FERTILIZERS_SUPPLY -> Icons.Filled.Eco
    AgriServiceCategory.COLD_STORAGE_WAREHOUSES -> Icons.Filled.AcUnit
    AgriServiceCategory.AGRICULTURAL_TRANSPORT_SERVICE -> Icons.Filled.LocalShipping
    AgriServiceCategory.AGRONOMIST_CONSULTATION -> Icons.Filled.Psychology
    AgriServiceCategory.MARKET_PRICES_SERVICE -> Icons.AutoMirrored.Filled.ShowChart
    AgriServiceCategory.AGRI_WEATHER_SERVICE -> Icons.Filled.Cloud
    AgriServiceCategory.SOIL_LAB_INRAA -> Icons.Filled.Science
    AgriServiceCategory.DRONE_NDVI_SCAN -> Icons.Filled.FlightTakeoff
  }
