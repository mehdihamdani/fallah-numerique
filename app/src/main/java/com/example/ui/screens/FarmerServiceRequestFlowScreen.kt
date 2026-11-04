package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AgriculturalParcel
import com.example.data.model.AppLanguage
import com.example.data.model.MarketplaceOrder
import com.example.data.model.PartnerCatalogItem
import com.example.data.model.UserRatingRecord
import com.example.data.model.UserRole
import com.example.data.offline.FarmerPlainOrderStatus
import com.example.data.offline.FarmerServiceChoice
import com.example.data.offline.FarmerServiceDraft
import com.example.data.offline.FarmerServiceFlowHelper
import com.example.data.offline.FarmerServiceTile
import com.example.data.offline.FarmerServiceWizardStage
import com.example.util.LocalizedResourceHelper
import com.example.util.trStringResource
import java.text.NumberFormat
import java.util.Locale

/**
 * Low-literacy 3-step Agricultural Service Request Flow for Algerian farmers:
 * - Service Grid: 5 large tiles (Workers, Machinery, Transport, Analysis & scanning, Other)
 *   + secondary "Call my engineer" button + quick button to check plain-word request status.
 * - Step 1: Choose specific service from a short translated list.
 * - Step 2: Choose date from 3 suggested slots over the next 7 days + 1 "Other date" option
 *   (no free calendar shown by default).
 * - Step 3: Plain summary with indicative reference price from `PartnerCatalogItem`, pre-filled
 *   first parcel / wilaya / commune / centroid, or a clear visual blocker if the farmer has no parcel.
 * - Post-Submission Status Screen: Plain words only ("Waiting for a provider", "A provider accepted", "Done")
 *   with 1..5 star provider rating (`UserRatingRecord`) after completion.
 */
@Composable
fun FarmerServiceRequestFlowScreen(
  language: AppLanguage,
  draft: FarmerServiceDraft,
  parcels: List<AgriculturalParcel>,
  partnerCatalog: List<PartnerCatalogItem>,
  myOrders: List<MarketplaceOrder>,
  publishedRatings: List<UserRatingRecord>,
  currentUserId: String,
  isSubmitting: Boolean,
  onSelectTile: (FarmerServiceTile) -> Unit,
  onSelectServiceChoice: (FarmerServiceChoice) -> Unit,
  onCallMyEngineer: () -> Unit,
  onSelectDate: (isoDate: String, isCustom: Boolean) -> Unit,
  onStepBack: () -> Unit,
  onOpenStatusScreen: () -> Unit,
  onStartNewRequest: () -> Unit,
  onSendRequest: (localizedTitle: String, localizedUnit: String) -> Unit,
  onOpenMyLand: () -> Unit,
  onBackToHome: () -> Unit,
  onSubmitProviderRating: (providerId: String, serviceTitle: String, stars: Int, orderId: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val displayLocale = remember(language) { LocalizedResourceHelper.localeForLanguage(language) }
  val suggestedSlots =
    remember(displayLocale) {
      FarmerServiceFlowHelper.generateSuggestedSlotsNext7Days(displayLocale = displayLocale)
    }
  val otherDateOptions =
    remember(displayLocale) {
      FarmerServiceFlowHelper.generateOtherDateOptions(displayLocale = displayLocale)
    }
  val parcelPrefill = remember(parcels) { FarmerServiceFlowHelper.resolveFirstParcelPrefill(parcels) }

  val myServiceRequests =
    remember(myOrders, currentUserId) {
      myOrders.filter { it.requesterId == currentUserId && it.orderType == "service_quote" }
    }

  var showOtherDateDialog by rememberSaveable { mutableStateOf(false) }
  var ratingTargetOrder by remember { mutableStateOf<MarketplaceOrder?>(null) }

  BackHandler {
    if (draft.wizardStage == FarmerServiceWizardStage.GRID) {
      onBackToHome()
    } else {
      onStepBack()
    }
  }

  Column(
    modifier = modifier.fillMaxWidth().testTag("farmer_service_flow_root"),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Top Header with large >= 56dp Back Button & Status Shortcut
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedButton(
        onClick = {
          if (draft.wizardStage == FarmerServiceWizardStage.GRID) onBackToHome() else onStepBack()
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.heightIn(min = 56.dp).testTag("farmer_service_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = trStringResource(language, R.string.farmer_srv_btn_back),
          modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text =
            if (draft.wizardStage == FarmerServiceWizardStage.GRID) {
              trStringResource(language, R.string.farmer_srv_btn_home)
            } else {
              trStringResource(language, R.string.farmer_srv_btn_back)
            },
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
        )
      }

      if (draft.wizardStage != FarmerServiceWizardStage.STATUS_SCREEN && myServiceRequests.isNotEmpty()) {
        Button(
          onClick = onOpenStatusScreen,
          shape = RoundedCornerShape(14.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
          modifier = Modifier.heightIn(min = 56.dp).testTag("farmer_open_status_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.HourglassTop,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = trStringResource(language, R.string.farmer_srv_my_requests_btn, myServiceRequests.size),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    when (draft.wizardStage) {
      FarmerServiceWizardStage.GRID -> {
        FarmerServiceGridSection(
          language = language,
          onSelectTile = onSelectTile,
          onCallMyEngineer = onCallMyEngineer,
        )
      }

      FarmerServiceWizardStage.STEP_1_SERVICE -> {
        val activeTile =
          FarmerServiceTile.fromCodeOrNull(draft.selectedTileCode) ?: FarmerServiceTile.WORKERS
        val choices = remember(activeTile) { FarmerServiceChoice.choicesForTile(activeTile) }
        StepHeaderCard(
          language = language,
          stepNumber = 1,
          title = trStringResource(language, R.string.farmer_step1_choose_service),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          choices.forEach { choice ->
            LargeChoiceCard(
              title = trStringResource(language, choice.labelRes),
              icon = iconForTile(activeTile),
              isSelected = draft.selectedCategoryCode == choice.categoryCode,
              onClick = { onSelectServiceChoice(choice) },
              testTag = "farmer_step1_choice_${choice.categoryCode}",
            )
          }
        }
      }

      FarmerServiceWizardStage.STEP_2_DATE -> {
        StepHeaderCard(
          language = language,
          stepNumber = 2,
          title = trStringResource(language, R.string.farmer_step2_choose_date),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          suggestedSlots.forEach { slot ->
            LargeChoiceCard(
              title = trStringResource(language, slot.templateRes, slot.readableDayMonth),
              icon = Icons.Filled.Event,
              isSelected = !draft.isCustomDate && draft.selectedDateIso == slot.isoDate,
              onClick = { onSelectDate(slot.isoDate, false) },
              testTag = "farmer_step2_slot_${slot.slotIndex}",
            )
          }

          val otherLabel =
            if (draft.isCustomDate && draft.selectedDateIso.isNotBlank()) {
              trStringResource(language, R.string.farmer_slot_other_date_selected, draft.selectedDateIso)
            } else {
              trStringResource(language, R.string.farmer_slot_other_date)
            }

          OutlinedButton(
            onClick = { showOtherDateDialog = true },
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            modifier =
              Modifier.fillMaxWidth()
                .heightIn(min = 64.dp)
                .testTag("farmer_step2_other_date_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.CalendarMonth,
              contentDescription = null,
              modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = otherLabel,
              fontSize = 19.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      FarmerServiceWizardStage.STEP_3_CONFIRM -> {
        val selectedChoice =
          FarmerServiceChoice.fromCategoryCodeOrNull(draft.selectedCategoryCode)
            ?: FarmerServiceChoice.FARM_LABOR
        val localizedServiceTitle = trStringResource(language, selectedChoice.labelRes)
        val localizedUnitLabel = trStringResource(language, selectedChoice.unitLabelRes)

        val dateDisplay =
          remember(draft.selectedDateIso, draft.isCustomDate, suggestedSlots, otherDateOptions) {
            suggestedSlots
              .firstOrNull { it.isoDate == draft.selectedDateIso && !draft.isCustomDate }
              ?.readableDayMonth
              ?: otherDateOptions.firstOrNull { it.first == draft.selectedDateIso }?.second
              ?: draft.selectedDateIso
          }

        val indicativePrice =
          remember(partnerCatalog, selectedChoice.categoryCode, parcelPrefill?.wilaya) {
            FarmerServiceFlowHelper.findIndicativeReferencePrice(
              catalogItems = partnerCatalog,
              serviceCategoryCode = selectedChoice.categoryCode,
              farmerWilaya = parcelPrefill?.wilaya.orEmpty(),
            )
          }

        StepHeaderCard(
          language = language,
          stepNumber = 3,
          title = trStringResource(language, R.string.farmer_step3_confirm_title),
        )

        FarmerStep3ConfirmationSection(
          language = language,
          localizedServiceTitle = localizedServiceTitle,
          dateDisplay = dateDisplay,
          parcelPrefill = parcelPrefill,
          indicativePrice = indicativePrice,
          hasRetryFailure = draft.hasFailedSendToRetry,
          isSubmitting = isSubmitting,
          onSendRequest = { onSendRequest(localizedServiceTitle, localizedUnitLabel) },
          onOpenMyLand = onOpenMyLand,
        )
      }

      FarmerServiceWizardStage.STATUS_SCREEN -> {
        FarmerPlainStatusSection(
          language = language,
          orders = myServiceRequests,
          publishedRatings = publishedRatings,
          currentUserId = currentUserId,
          highlightedOrderId = draft.lastSubmittedOrderId,
          onStartNewRequest = onStartNewRequest,
          onRateOrderProvider = { ratingTargetOrder = it },
        )
      }
    }
  }

  // Optional "Other date" modal — opened ONLY when farmer explicitly taps "Other date"
  if (showOtherDateDialog) {
    AlertDialog(
      onDismissRequest = { showOtherDateDialog = false },
      title = {
        Text(
          text = trStringResource(language, R.string.farmer_slot_other_dialog_title),
          fontSize = 20.sp,
          fontWeight = FontWeight.ExtraBold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          otherDateOptions.forEachIndexed { idx, (iso, readable) ->
            Button(
              onClick = {
                showOtherDateDialog = false
                onSelectDate(iso, true)
              },
              shape = RoundedCornerShape(14.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.secondaryContainer,
                  contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
              modifier =
                Modifier.fillMaxWidth()
                  .heightIn(min = 56.dp)
                  .testTag("farmer_other_date_option_$idx"),
            ) {
              Icon(Icons.Filled.Event, contentDescription = null, modifier = Modifier.size(22.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = readable, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        OutlinedButton(
          onClick = { showOtherDateDialog = false },
          modifier = Modifier.heightIn(min = 56.dp),
        ) {
          Text(
            text = trStringResource(language, R.string.farmer_slot_other_cancel),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      },
    )
  }

  // Post-completion 1..5 Star Provider Rating Dialog (`UserRatingRecord`)
  ratingTargetOrder?.let { targetOrder ->
    var selectedStars by rememberSaveable(targetOrder.id) { mutableIntStateOf(5) }
    val localizedServiceName =
      FarmerServiceChoice.fromCategoryCodeOrNull(targetOrder.serviceCategory)?.let {
        trStringResource(language, it.labelRes)
      } ?: targetOrder.itemTitle

    AlertDialog(
      onDismissRequest = { ratingTargetOrder = null },
      title = {
        Text(
          text = trStringResource(language, R.string.farmer_rate_dialog_title),
          fontSize = 20.sp,
          fontWeight = FontWeight.ExtraBold,
        )
      },
      text = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          (1..5).forEach { star ->
            IconButton(
              onClick = { selectedStars = star },
              modifier = Modifier.size(56.dp).testTag("farmer_rating_star_$star"),
            ) {
              Icon(
                imageVector =
                  if (star <= selectedStars) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = "$star",
                tint =
                  if (star <= selectedStars) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(40.dp),
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onSubmitProviderRating(
              targetOrder.providerId,
              localizedServiceName,
              selectedStars,
              targetOrder.id,
            )
            ratingTargetOrder = null
          },
          modifier = Modifier.heightIn(min = 56.dp).testTag("farmer_rating_submit_button"),
        ) {
          Text(
            text = trStringResource(language, R.string.farmer_rate_submit_btn),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { ratingTargetOrder = null },
          modifier = Modifier.heightIn(min = 56.dp),
        ) {
          Text(
            text = trStringResource(language, R.string.farmer_slot_other_cancel),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      },
    )
  }
}

@Composable
private fun FarmerServiceGridSection(
  language: AppLanguage,
  onSelectTile: (FarmerServiceTile) -> Unit,
  onCallMyEngineer: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Text(
      text = trStringResource(language, R.string.farmer_srv_title),
      fontSize = 24.sp,
      fontWeight = FontWeight.ExtraBold,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text = trStringResource(language, R.string.farmer_srv_subtitle),
      fontSize = 18.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // Five large tiles (>= 84dp height, >= 20sp bold labels, distinct icons)
    FarmerServiceTile.entries.forEach { tile ->
      Card(
        shape = RoundedCornerShape(20.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier =
          Modifier.fillMaxWidth()
            .heightIn(min = 84.dp)
            .clickable { onSelectTile(tile) }
            .testTag("farmer_service_tile_${tile.tileCode}"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = iconForTile(tile),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp),
              )
            }
          }
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = trStringResource(language, tile.titleRes),
              fontSize = 21.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = trStringResource(language, tile.subtitleRes),
              fontSize = 18.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Secondary button inside the service screen: "Call my engineer" (moved from Home)
    OutlinedButton(
      onClick = onCallMyEngineer,
      shape = RoundedCornerShape(18.dp),
      border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary),
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 64.dp)
          .testTag("farmer_secondary_call_engineer_button"),
    ) {
      Icon(
        imageVector = Icons.Filled.PhoneInTalk,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(28.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(horizontalAlignment = Alignment.Start) {
        Text(
          text = trStringResource(language, R.string.farmer_srv_call_engineer),
          fontSize = 19.sp,
          fontWeight = FontWeight.ExtraBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = trStringResource(language, R.string.farmer_srv_call_engineer_sub),
          fontSize = 16.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun StepHeaderCard(
  language: AppLanguage,
  stepNumber: Int,
  title: String,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = trStringResource(language, R.string.farmer_step_progress, stepNumber),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
      Text(
        text = title,
        fontSize = 22.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    }
  }
}

@Composable
private fun LargeChoiceCard(
  title: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isSelected) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surfaceContainerLowest
      ),
    border =
      BorderStroke(
        width = if (isSelected) 3.dp else 1.5.dp,
        color =
          if (isSelected) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.outlineVariant,
      ),
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 68.dp)
        .clickable(onClick = onClick)
        .testTag(testTag),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Icon(
        imageVector = if (isSelected) Icons.Filled.CheckCircle else icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(30.dp),
      )
      Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun FarmerStep3ConfirmationSection(
  language: AppLanguage,
  localizedServiceTitle: String,
  dateDisplay: String,
  parcelPrefill: com.example.data.offline.ParcelLocationPrefill?,
  indicativePrice: com.example.data.offline.IndicativePriceMatch?,
  hasRetryFailure: Boolean,
  isSubmitting: Boolean,
  onSendRequest: () -> Unit,
  onOpenMyLand: () -> Unit,
) {
  val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }

  // Summary Card
  Card(
    shape = RoundedCornerShape(20.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth().testTag("farmer_step3_summary_card"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      SummaryRow(
        icon = Icons.Filled.Build,
        label = trStringResource(language, R.string.farmer_summary_service_label),
        value = localizedServiceTitle,
      )
      HorizontalDivider()
      SummaryRow(
        icon = Icons.Filled.Event,
        label = trStringResource(language, R.string.farmer_summary_date_label),
        value = dateDisplay,
      )
      HorizontalDivider()
      SummaryRow(
        icon = Icons.Filled.Payments,
        label = trStringResource(language, R.string.farmer_summary_indicative_price_label),
        value =
          if (indicativePrice != null) {
            trStringResource(
              language,
              R.string.farmer_summary_indicative_price_value,
              numberFormat.format(indicativePrice.priceDzd.toLong()),
              indicativePrice.unitLabel,
            )
          } else {
            trStringResource(language, R.string.farmer_summary_indicative_price_none)
          },
      )

      if (parcelPrefill != null) {
        HorizontalDivider()
        SummaryRow(
          icon = Icons.Filled.Map,
          label = trStringResource(language, R.string.farmer_summary_land_label),
          value = parcelPrefill.parcelName,
        )
        SummaryRow(
          icon = Icons.Filled.LocationOn,
          label = trStringResource(language, R.string.farmer_summary_location_label),
          value =
            listOf(parcelPrefill.wilaya, parcelPrefill.commune)
              .filter { it.isNotBlank() }
              .joinToString(" — "),
        )
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.secondaryContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(24.dp),
            )
            Text(
              text = trStringResource(language, R.string.farmer_summary_gps_ready),
              fontSize = 18.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
      }
    }
  }

  // Slow-connection local draft reassurance badge
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.Save,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp),
      )
      Text(
        text = trStringResource(language, R.string.farmer_draft_saved_badge),
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }

  // High-contrast No-Parcel Blocker OR Send / Retry Action
  if (parcelPrefill == null) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
      border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
      modifier = Modifier.fillMaxWidth().testTag("farmer_no_parcel_blocker_card"),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.LocationOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(36.dp),
          )
          Text(
            text = trStringResource(language, R.string.farmer_no_parcel_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
        Text(
          text = trStringResource(language, R.string.farmer_no_parcel_explanation),
          fontSize = 18.sp,
          color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Button(
          onClick = onOpenMyLand,
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = 60.dp)
              .testTag("farmer_no_parcel_open_my_land_btn"),
        ) {
          Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = trStringResource(language, R.string.farmer_no_parcel_go_my_land),
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
          )
        }
      }
    }
  } else {
    if (hasRetryFailure) {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth().testTag("farmer_slow_network_retry_card"),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.WifiOff,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.size(32.dp),
            )
            Text(
              text = trStringResource(language, R.string.farmer_send_retry_title),
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onErrorContainer,
            )
          }
          Button(
            onClick = onSendRequest,
            enabled = !isSubmitting,
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth()
                .heightIn(min = 60.dp)
                .testTag("farmer_retry_send_button"),
          ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = trStringResource(language, R.string.farmer_btn_retry_send),
              fontSize = 19.sp,
              fontWeight = FontWeight.ExtraBold,
            )
          }
        }
      }
    }

    Button(
      onClick = onSendRequest,
      enabled = !isSubmitting,
      shape = RoundedCornerShape(18.dp),
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 64.dp)
          .testTag("farmer_send_request_button"),
    ) {
      if (isSubmitting) {
        CircularProgressIndicator(
          color = MaterialTheme.colorScheme.onPrimary,
          strokeWidth = 3.dp,
          modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = trStringResource(language, R.string.farmer_btn_sending),
          fontSize = 20.sp,
          fontWeight = FontWeight.ExtraBold,
        )
      } else {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = null,
          modifier = Modifier.size(26.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = trStringResource(language, R.string.farmer_btn_send_request),
          fontSize = 21.sp,
          fontWeight = FontWeight.ExtraBold,
        )
      }
    }
  }
}

@Composable
private fun SummaryRow(
  icon: ImageVector,
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(28.dp),
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = label,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}

@Composable
private fun FarmerPlainStatusSection(
  language: AppLanguage,
  orders: List<MarketplaceOrder>,
  publishedRatings: List<UserRatingRecord>,
  currentUserId: String,
  highlightedOrderId: String,
  onStartNewRequest: () -> Unit,
  onRateOrderProvider: (MarketplaceOrder) -> Unit,
) {
  val numberFormat = remember { NumberFormat.getIntegerInstance(Locale.US) }

  Column(
    modifier = Modifier.fillMaxWidth().testTag("farmer_status_screen_section"),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text(
      text = trStringResource(language, R.string.farmer_status_screen_heading),
      fontSize = 24.sp,
      fontWeight = FontWeight.ExtraBold,
      color = MaterialTheme.colorScheme.onSurface,
    )

    if (orders.isEmpty()) {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.HourglassTop,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(30.dp),
          )
          Text(
            text = trStringResource(language, R.string.farmer_empty_requests_msg),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    } else {
      val sorted =
        remember(orders, highlightedOrderId) {
          orders.sortedByDescending { it.id == highlightedOrderId }
        }
      sorted.forEach { order ->
        val plainStatus = FarmerPlainOrderStatus.fromBackendStatus(order.status)
        val serviceLabel =
          FarmerServiceChoice.fromCategoryCodeOrNull(order.serviceCategory)?.let {
            trStringResource(language, it.labelRes)
          } ?: order.itemTitle
        val alreadyRated =
          publishedRatings.any {
            it.reviewerId == currentUserId &&
              it.targetUserId == order.providerId &&
              order.providerId.isNotBlank()
          }

        val statusIcon =
          when (plainStatus) {
            FarmerPlainOrderStatus.WAITING_FOR_PROVIDER -> Icons.Filled.HourglassTop
            FarmerPlainOrderStatus.PROVIDER_ACCEPTED -> Icons.Filled.CheckCircle
            FarmerPlainOrderStatus.DONE -> Icons.Filled.TaskAlt
            FarmerPlainOrderStatus.CANCELLED -> Icons.Filled.LocationOff
          }
        val statusContainer =
          when (plainStatus) {
            FarmerPlainOrderStatus.WAITING_FOR_PROVIDER -> MaterialTheme.colorScheme.secondaryContainer
            FarmerPlainOrderStatus.PROVIDER_ACCEPTED -> MaterialTheme.colorScheme.primaryContainer
            FarmerPlainOrderStatus.DONE -> MaterialTheme.colorScheme.tertiaryContainer
            FarmerPlainOrderStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceContainerHighest
          }
        val statusContentColor =
          when (plainStatus) {
            FarmerPlainOrderStatus.WAITING_FOR_PROVIDER ->
              MaterialTheme.colorScheme.onSecondaryContainer
            FarmerPlainOrderStatus.PROVIDER_ACCEPTED -> MaterialTheme.colorScheme.onPrimaryContainer
            FarmerPlainOrderStatus.DONE -> MaterialTheme.colorScheme.onTertiaryContainer
            FarmerPlainOrderStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
          }

        Card(
          shape = RoundedCornerShape(20.dp),
          colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
          border =
            BorderStroke(
              width = if (order.id == highlightedOrderId) 2.5.dp else 1.5.dp,
              color =
                if (order.id == highlightedOrderId) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
            ),
          modifier = Modifier.fillMaxWidth().testTag("farmer_status_card_${order.id}"),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = serviceLabel,
              fontSize = 21.sp,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            if (order.preferredDate.isNotBlank()) {
              Text(
                text =
                  "${trStringResource(language, R.string.farmer_summary_date_label)}: ${order.preferredDate}",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (order.proposedPriceDzd > 0.0) {
              Text(
                text =
                  trStringResource(
                    language,
                    R.string.farmer_status_agreed_price,
                    numberFormat.format(order.proposedPriceDzd.toLong()),
                  ),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = statusContainer,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  imageVector = statusIcon,
                  contentDescription = null,
                  tint = statusContentColor,
                  modifier = Modifier.size(30.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text(
                    text = trStringResource(language, plainStatus.labelRes),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusContentColor,
                  )
                  if (plainStatus != FarmerPlainOrderStatus.CANCELLED) {
                    Text(
                      text = trStringResource(language, plainStatus.descriptionRes),
                      fontSize = 17.sp,
                      color = statusContentColor,
                    )
                  }
                }
              }
            }

            if (
              plainStatus == FarmerPlainOrderStatus.DONE &&
                order.providerId.isNotBlank() &&
                order.providerId != currentUserId
            ) {
              if (alreadyRated) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surfaceContainerHigh,
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.Star,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(24.dp),
                    )
                    Text(
                      text = trStringResource(language, R.string.farmer_rate_provider_done),
                      fontSize = 18.sp,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              } else {
                Button(
                  onClick = { onRateOrderProvider(order) },
                  shape = RoundedCornerShape(14.dp),
                  modifier =
                    Modifier.fillMaxWidth()
                      .heightIn(min = 56.dp)
                      .testTag("farmer_rate_provider_btn_${order.id}"),
                ) {
                  Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(24.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = trStringResource(language, R.string.farmer_rate_provider_btn),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }
      }
    }

    Button(
      onClick = onStartNewRequest,
      shape = RoundedCornerShape(16.dp),
      modifier =
        Modifier.fillMaxWidth()
          .heightIn(min = 60.dp)
          .testTag("farmer_new_service_request_button"),
    ) {
      Text(
        text = trStringResource(language, R.string.farmer_srv_new_request_btn),
        fontSize = 19.sp,
        fontWeight = FontWeight.ExtraBold,
      )
    }
  }
}

private fun iconForTile(tile: FarmerServiceTile): ImageVector =
  when (tile) {
    FarmerServiceTile.WORKERS -> Icons.Filled.Groups
    FarmerServiceTile.MACHINERY -> Icons.Filled.Agriculture
    FarmerServiceTile.TRANSPORT -> Icons.Filled.LocalShipping
    FarmerServiceTile.ANALYSIS_SCAN -> Icons.Filled.Science
    FarmerServiceTile.OTHER -> Icons.Filled.Build
  }
