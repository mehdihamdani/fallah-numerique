package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AgriServiceCategory
import com.example.data.model.ServiceBooking
import com.example.data.model.TransportRequest
import com.example.data.model.TransportVehicleType
import com.example.ui.viewmodel.UiState
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransportLogisticsScreen(
  transportState: UiState<List<TransportRequest>>,
  defaultWilaya: String,
  activeParcelRef: String,
  onSubmitTransport: (String, String, String, Double, String) -> Unit,
  onAdvanceStatus: (TransportRequest) -> Unit,
  onCancelRequest: (String) -> Unit,
  onBackToDelineation: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackToDelineation)
  var selectedVehicle by remember { mutableStateOf(TransportVehicleType.REFRIGERATED_TRUCK) }
  var showRequestDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Header
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Column(modifier = Modifier.weight(1f, fill = false)) {
        Text(
          text = stringResource(R.string.transport_screen_title),
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = stringResource(R.string.transport_screen_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Button(
        onClick = { showRequestDialog = true },
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(48.dp).testTag("open_transport_dialog_button"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("طلب شاحنة نقل", fontWeight = FontWeight.Bold)
      }
    }

    // Fleet Type Selector Grid
    Text(
      text = "أسطول النقل الفلاحي المتاح (Sélectionner le type de camion):",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      maxItemsInEachRow = 2,
    ) {
      TransportVehicleType.entries.forEach { vehicle ->
        val isSelected = selectedVehicle == vehicle
        Card(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .clickable {
                selectedVehicle = vehicle
                showRequestDialog = true
              }
              .border(
                width = if (isSelected) 2.dp else 1.dp,
                color =
                  if (isSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
              )
              .testTag("vehicle_card_${vehicle.code}"),
          shape = RoundedCornerShape(14.dp),
          colors =
            CardDefaults.cardColors(
              containerColor =
                if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surfaceContainerLowest
            ),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector =
                  if (vehicle == TransportVehicleType.REFRIGERATED_TRUCK) Icons.Filled.AcUnit
                  else Icons.Filled.LocalShipping,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
              )
              Text(
                text = vehicle.titleAr,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Text(
              text = vehicle.subtitleFr,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = vehicle.capacityLabel,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    // Active Shipments List
    Text(
      text = "سجل الإرساليات وطلبات الشحن الجارية:",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface,
    )

    when (transportState) {
      UiState.Loading -> {
        Box(
          modifier = Modifier.fillMaxWidth().padding(28.dp),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      }
      is UiState.Error -> {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = transportState.message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
          )
        }
      }
      is UiState.Success -> {
        if (transportState.data.isEmpty()) {
          EmptyLogisticsPlaceholder(
            icon = Icons.Filled.LocalShipping,
            title = "لا توجد طلبات نقل مسجلة حالياً",
            subtitle = "اضغط على «طلب شاحنة نقل» لجدولة نقل محصولك من القطعة الفلاحية إلى أسواق الجملة أو غرف التبريد.",
            actionLabel = "جدولة شحنة جديدة",
            onAction = { showRequestDialog = true },
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            transportState.data.forEach { req ->
              TransportOrderCard(
                request = req,
                onAdvanceStatus = { onAdvanceStatus(req) },
                onCancel = { onCancelRequest(req.id) },
              )
            }
          }
        }
      }
    }
  }

  if (showRequestDialog) {
    RequestTransportDialog(
      initialVehicle = selectedVehicle,
      defaultWilaya = defaultWilaya,
      activeParcelRef = activeParcelRef,
      onDismiss = { showRequestDialog = false },
      onConfirm = { vehicleCode, origin, dest, tons, date ->
        onSubmitTransport(vehicleCode, origin, dest, tons, date)
        showRequestDialog = false
      },
    )
  }
}

@Composable
private fun TransportOrderCard(
  request: TransportRequest,
  onAdvanceStatus: () -> Unit,
  onCancel: () -> Unit,
) {
  val vehicle = TransportVehicleType.fromCode(request.vehicleType)
  val statusLabel =
    when (request.status) {
      "pending" -> "قيد التجهيز (En attente)"
      "in_transit" -> "في الطريق (En transit)"
      else -> "تم التسليم (Livré)"
    }
  val isDelivered = request.status == "delivered"

  Card(
    shape = RoundedCornerShape(14.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          1.dp,
          MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
          RoundedCornerShape(14.dp),
        ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.LocalShipping,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column {
            Text(
              text = vehicle.titleAr,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "القطعة: ${request.parcelReference} • الحمولة: ${String.format(Locale.US, "%.1f", request.cargoTons)} طن",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Surface(
          shape = RoundedCornerShape(50),
          color =
            if (isDelivered) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.tertiaryContainer,
        ) {
          Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color =
              if (isDelivered) MaterialTheme.colorScheme.onSecondaryContainer
              else MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Place,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.secondary,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = "من ${request.originWilaya} ⬅ إلى ${request.destinationMarket} (${request.scheduledDate})",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      if (!isDelivered) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(onClick = onCancel, modifier = Modifier.size(48.dp)) {
            Icon(
              imageVector = Icons.Filled.DeleteOutline,
              contentDescription = "إلغاء الطلب",
              tint = MaterialTheme.colorScheme.error,
            )
          }
          OutlinedButton(
            onClick = onAdvanceStatus,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(44.dp),
          ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (request.status == "pending") "تأكيد انطلاق الشاحنة" else "تأكيد الوصول والتسليم")
          }
        }
      }
    }
  }
}

@Composable
private fun RequestTransportDialog(
  initialVehicle: TransportVehicleType,
  defaultWilaya: String,
  activeParcelRef: String,
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, Double, String) -> Unit,
) {
  var origin by remember { mutableStateOf(defaultWilaya) }
  var destination by remember { mutableStateOf("سوق الجملة الكاليتوس - الجزائر") }
  var cargoTonsText by remember { mutableStateOf("18.0") }
  var dateText by remember { mutableStateOf("2026-11-02") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("حجز شاحنة نقل فلاحي (${initialVehicle.titleAr})") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "القطعة المرجعية: $activeParcelRef",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
          value = origin,
          onValueChange = { origin = it },
          label = { Text("ولاية ومنطقة الانطلاق") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = destination,
          onValueChange = { destination = it },
          label = { Text("الوجهة (سوق الجملة / التبريد / الميناء)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = cargoTonsText,
          onValueChange = { cargoTonsText = it },
          label = { Text("الكمية المشحونة (طن)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = dateText,
          onValueChange = { dateText = it },
          label = { Text("تاريخ الشحن المطلوب") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val tons = cargoTonsText.toDoubleOrNull()?.coerceIn(0.1, 5000.0) ?: 15.0
          onConfirm(initialVehicle.code, origin, destination, tons, dateText)
        },
        modifier = Modifier.testTag("confirm_transport_request_button"),
      ) {
        Text("تأكيد طلب النقل")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
  )
}

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
) {
  BackHandler(onBack = onBackToDelineation)
  var selectedCategory by remember { mutableStateOf<AgriServiceCategory?>(null) }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Column {
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

    // Catalog of Agricultural & Technical Services
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      AgriServiceCategory.entries.forEach { service ->
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
              ),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.Top,
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(46.dp),
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
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = { selectedCategory = service },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(48.dp).testTag("book_service_${service.code}"),
            ) {
              Text("حجز", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Active Bookings Section
    Text(
      text = "حجوزاتي وطلبات التدخل التقني:",
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
            subtitle = "اختر إحدى الخدمات التقنية أو المخبرية أعلاه لحجز موعد ميداني لقطعتك الفلاحية.",
            actionLabel = "حجز تحليل تربة INRAA",
            onAction = { selectedCategory = AgriServiceCategory.SOIL_LAB_INRAA },
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bookingsState.data.forEach { booking ->
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
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = booking.serviceTitle,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "${booking.parcelReference} • ${booking.wilaya} • الموعد: ${booking.preferredDate}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (booking.notes.isNotBlank()) {
                      Text(
                        text = "ملاحظات: ${booking.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                      )
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
}

@Composable
private fun BookServiceDialog(
  service: AgriServiceCategory,
  defaultWilaya: String,
  activeParcelRef: String,
  onDismiss: () -> Unit,
  onConfirm: (String, String, String) -> Unit,
) {
  var wilaya by remember { mutableStateOf(defaultWilaya) }
  var dateText by remember { mutableStateOf("2026-11-05") }
  var notes by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(service.titleAr) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "${service.providerBadge} • القطعة: $activeParcelRef",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
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
          label = { Text("التاريخ المفضل للتدخل") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
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
        onClick = { onConfirm(wilaya, dateText, notes) },
        modifier = Modifier.testTag("confirm_service_booking_button"),
      ) {
        Text("تأكيد الحجز")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
  )
}

@Composable
private fun EmptyLogisticsPlaceholder(
  icon: ImageVector,
  title: String,
  subtitle: String,
  actionLabel: String,
  onAction: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(38.dp),
      )
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(4.dp))
      OutlinedButton(onClick = onAction, modifier = Modifier.height(48.dp)) {
        Text(actionLabel, fontWeight = FontWeight.Bold)
      }
    }
  }
}

private fun serviceIcon(category: AgriServiceCategory): ImageVector =
  when (category) {
    AgriServiceCategory.SOIL_LAB_INRAA -> Icons.Filled.Science
    AgriServiceCategory.DRONE_NDVI_SCAN -> Icons.Filled.FlightTakeoff
    AgriServiceCategory.FORAGE_DRILLING -> Icons.Filled.WaterDrop
    AgriServiceCategory.HARVESTER_RENTAL -> Icons.Filled.Agriculture
    AgriServiceCategory.AGRONOMIC_ADVISORY -> Icons.Filled.Psychology
  }
