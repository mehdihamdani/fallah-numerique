package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.MarketplaceOrder
import com.example.data.model.PartnerCatalogItem
import com.example.data.model.PlatformMessage
import com.example.data.model.UserRole
import com.example.ui.viewmodel.UiState
import com.example.util.RoleStrings

/**
 * Role-specific Partner Catalog Manager (`PartnerCatalogScreen`) for:
 * - `transporter` (vehicles, refrigerated/flatbed capacity, routes)
 * - `supplier` (seeds, seedlings, fertilizers, irrigation gear, stock & pricing)
 * - `service_provider` (plowing, harvesting, irrigation, soil analysis, consulting)
 * - `storage_provider` (warehouses, cold rooms, available tonnage, sorting/packaging)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PartnerCatalogScreen(
  activeRole: UserRole,
  language: AppLanguage,
  providerDisplayName: String,
  defaultWilaya: String,
  myCatalogState: UiState<List<PartnerCatalogItem>>,
  onPublishItem: (PartnerCatalogItem) -> Unit,
  onToggleStatus: (PartnerCatalogItem) -> Unit,
  onDeleteItem: (String) -> Unit,
  onBackToDashboard: () -> Unit,
) {
  BackHandler(onBack = onBackToDashboard)

  val categoryPresets = rememberCategoryPresetsForRole(activeRole)
  var selectedCategory by remember(activeRole) { mutableStateOf(categoryPresets.first().first) }
  var titleInput by remember(activeRole) { mutableStateOf("") }
  var wilayaInput by remember(defaultWilaya) { mutableStateOf(defaultWilaya) }
  var priceInput by remember(activeRole) { mutableStateOf("4500") }
  var unitInput by remember(activeRole) {
    mutableStateOf(
      when (activeRole) {
        UserRole.TRANSPORTER -> "د.ج / رحلة"
        UserRole.SUPPLIER -> "د.ج / قنطار"
        UserRole.SERVICE_PROVIDER -> "د.ج / هكتار"
        UserRole.STORAGE_PROVIDER -> "د.ج / طن / شهر"
        else -> "د.ج / وحدة"
      }
    )
  }
  var quantityInput by remember(activeRole) { mutableStateOf("25") }
  var descriptionInput by remember(activeRole) { mutableStateOf("") }

  Column(
    modifier = Modifier.fillMaxWidth().testTag("partner_catalog_screen"),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = RoleStrings.catalogSectionHeadline(language, activeRole),
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.ExtraBold,
    )

    // Publish Form Card
    Card(
      shape = RoundedCornerShape(18.dp),
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
      modifier =
        Modifier.fillMaxWidth()
          .border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            RoundedCornerShape(18.dp),
          ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text =
            RoleStrings.tr(
              language,
              ar = "إضافة ونشر عرض جديد تحت صفة (${activeRole.titleAr})",
              fr = "Publier une nouvelle offre (${activeRole.titleFr})",
              en = "Publish New Offer (${activeRole.titleEn})",
            ),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          categoryPresets.forEach { (code, label) ->
            FilterChip(
              selected = selectedCategory == code,
              onClick = { selectedCategory = code },
              label = { Text(label) },
              modifier = Modifier.testTag("catalog_cat_$code"),
            )
          }
        }

        OutlinedTextField(
          value = titleInput,
          onValueChange = { titleInput = it },
          label = {
            Text(
              RoleStrings.tr(
                language,
                ar = "عنوان العرض / المركبة / المستلزم / غرفة التبريد",
                fr = "Intitulé de l'offre / véhicule / intrant / entrepôt",
                en = "Offer / Vehicle / Input / Facility Title",
              )
            )
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("catalog_title_input"),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedTextField(
            value = priceInput,
            onValueChange = { priceInput = it },
            label = { Text(RoleStrings.tr(language, "السعر (د.ج)", "Prix (DZD)", "Price (DZD)")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("catalog_price_input"),
          )
          OutlinedTextField(
            value = unitInput,
            onValueChange = { unitInput = it },
            label = { Text(RoleStrings.tr(language, "الوحدة", "Unité", "Unit")) },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("catalog_unit_input"),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedTextField(
            value = quantityInput,
            onValueChange = { quantityInput = it },
            label = {
              Text(
                RoleStrings.tr(
                  language,
                  ar = "الكمية أو السعة المتاحة",
                  fr = "Stock / Capacité dispo.",
                  en = "Stock / Capacity",
                )
              )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("catalog_capacity_input"),
          )
          OutlinedTextField(
            value = wilayaInput,
            onValueChange = { wilayaInput = it },
            label = { Text(RoleStrings.tr(language, "الولاية / التغطية", "Wilaya", "Wilaya")) },
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("catalog_wilaya_input"),
          )
        }

        OutlinedTextField(
          value = descriptionInput,
          onValueChange = { descriptionInput = it },
          label = {
            Text(
              RoleStrings.tr(
                language,
                ar = "المواصفات التقنية (درجة التبريد، الفرز والتوضيب، شروط التوصيل...)",
                fr = "Détails techniques (température, tri/emballage, livraison…)",
                en = "Technical specifications (temperature, grading/packaging, delivery…)",
              )
            )
          },
          modifier = Modifier.fillMaxWidth().testTag("catalog_description_input"),
        )

        Button(
          onClick = {
            val cleanTitle =
              titleInput.trim().ifBlank {
                categoryPresets.firstOrNull { it.first == selectedCategory }?.second ?: "Offre"
              }
            val parsedPrice = priceInput.replace(",", ".").toDoubleOrNull() ?: 0.0
            val parsedCap = quantityInput.replace(",", ".").toDoubleOrNull() ?: 1.0
            onPublishItem(
              PartnerCatalogItem(
                providerRole = activeRole.code,
                providerName = providerDisplayName,
                title = cleanTitle,
                category = selectedCategory,
                wilaya = wilayaInput,
                priceDzd = parsedPrice,
                unitLabel = unitInput,
                availableQuantityOrCapacity = parsedCap,
                description = descriptionInput,
              )
            )
            titleInput = ""
            descriptionInput = ""
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(50.dp).testTag("catalog_publish_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            RoleStrings.tr(language, "نشر العرض في المنصة", "Publier l'offre", "Publish Offer"),
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    // Existing Published Items for Active Role
    val myRoleItems =
      ((myCatalogState as? UiState.Success)?.data ?: emptyList()).filter {
        it.providerRole == activeRole.code
      }
    Text(
      text =
        RoleStrings.tr(
          language,
          ar = "عروضي المسجلة (${myRoleItems.size})",
          fr = "Mes offres enregistrées (${myRoleItems.size})",
          en = "My Registered Offers (${myRoleItems.size})",
        ),
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
    )

    myRoleItems.forEach { item ->
      Card(
        shape = RoundedCornerShape(14.dp),
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
          ),
        modifier =
          Modifier.fillMaxWidth()
            .border(
              1.dp,
              MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
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
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "${item.wilaya} • السعة/الكمية: ${item.availableQuantityOrCapacity} • ${item.priceDzd.toInt()} ${item.unitLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (item.description.isNotBlank()) {
                Text(
                  text = item.description,
                  style = MaterialTheme.typography.bodySmall,
                )
              }
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color =
                if (item.status == "active") MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
              Text(
                text =
                  if (item.status == "active") RoleStrings.tr(language, "نشط", "Actif", "Active")
                  else RoleStrings.tr(language, "متوقف مؤقتًا", "En pause", "Paused"),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedButton(
              onClick = { onToggleStatus(item) },
              modifier = Modifier.height(48.dp).testTag("toggle_catalog_${item.id}"),
            ) {
              Icon(
                imageVector =
                  if (item.status == "active") Icons.Filled.PauseCircleOutline
                  else Icons.Filled.PlayCircleOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                if (item.status == "active") RoleStrings.tr(language, "إيقاف مؤقت", "Pause", "Pause")
                else RoleStrings.tr(language, "تفعيل", "Activer", "Activate")
              )
            }
            IconButton(
              onClick = { onDeleteItem(item.id) },
              modifier = Modifier.size(48.dp).testTag("delete_catalog_${item.id}"),
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = RoleStrings.tr(language, "حذف", "Supprimer", "Delete"),
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
      }
    }
  }
}

/**
 * B2B Orders, Quotations & Storage/Transport Reservations Screen (`OrdersAndQuotesScreen`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OrdersAndQuotesScreen(
  activeRole: UserRole,
  language: AppLanguage,
  currentUserId: String,
  requesterDisplayName: String,
  defaultWilaya: String,
  myOrdersState: UiState<List<MarketplaceOrder>>,
  openRoleOrders: List<MarketplaceOrder>,
  onCreateOrder: (MarketplaceOrder) -> Unit,
  onSubmitQuote: (MarketplaceOrder, Double, String) -> Unit,
  onUpdateOrderStatus: (String, String) -> Unit,
  onBackToDashboard: () -> Unit,
) {
  BackHandler(onBack = onBackToDashboard)

  var selectedTab by remember { mutableIntStateOf(0) }
  var targetRoleCode by remember(activeRole) {
    mutableStateOf(
      if (activeRole == UserRole.TRADER) UserRole.FARMER.code else UserRole.TRANSPORTER.code
    )
  }
  var itemTitle by remember { mutableStateOf("") }
  var quantityText by remember { mutableStateOf("10") }
  var unitText by remember { mutableStateOf("طن") }
  var budgetText by remember { mutableStateOf("65000") }
  var notesText by remember { mutableStateOf("") }
  var quoteModalOrder by remember { mutableStateOf<MarketplaceOrder?>(null) }
  var quotePriceInput by remember { mutableStateOf("") }
  var quoteNotesInput by remember { mutableStateOf("") }

  val myOrders = (myOrdersState as? UiState.Success)?.data ?: emptyList()

  Column(
    modifier = Modifier.fillMaxWidth().testTag("orders_and_quotes_screen"),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text =
        RoleStrings.tr(
          language,
          ar = "الطلبات التجارية، عروض الأسعار، والحجوزات",
          fr = "Commandes B2B, Devis & Réservations",
          en = "B2B Orders, Quotations & Reservations",
        ),
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.ExtraBold,
    )

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        label = {
          Text(
            RoleStrings.tr(
              language,
              ar = "طلباتي وحجوزاتي (${myOrders.size})",
              fr = "Mes commandes (${myOrders.size})",
              en = "My Orders (${myOrders.size})",
            )
          )
        },
        modifier = Modifier.testTag("orders_tab_mine"),
      )
      FilterChip(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        label = {
          Text(
            RoleStrings.tr(
              language,
              ar = "طلبات مفتوحة للتسعير (${openRoleOrders.size})",
              fr = "Demandes ouvertes (${openRoleOrders.size})",
              en = "Open Requests (${openRoleOrders.size})",
            )
          )
        },
        modifier = Modifier.testTag("orders_tab_open"),
      )
      FilterChip(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        label = {
          Text(RoleStrings.tr(language, "+ طلب شراء / خدمة جديد", "+ Nouvelle demande", "+ New Request"))
        },
        modifier = Modifier.testTag("orders_tab_create"),
      )
    }

    when (selectedTab) {
      0 -> {
        if (myOrders.isEmpty()) {
          Text(
            text =
              RoleStrings.tr(
                language,
                ar = "لا توجد طلبات مسجلة بعد. اضغط على (+ طلب شراء / خدمة جديد) لإنشاء طلب.",
                fr = "Aucune commande enregistrée.",
                en = "No orders yet. Tap '+ New Request' to create one.",
              ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        myOrders.forEach { order ->
          val isRequester = order.requesterId == currentUserId
          Card(
            shape = RoundedCornerShape(14.dp),
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
              ),
            modifier =
              Modifier.fillMaxWidth()
                .border(
                  1.dp,
                  MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
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
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${order.itemTitle} (${order.quantity} ${order.unitLabel})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "${order.wilaya} • السعر المقترح: ${order.proposedPriceDzd.toInt()} DZD",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  if (order.notes.isNotBlank()) {
                    Text(text = order.notes, style = MaterialTheme.typography.bodySmall)
                  }
                }
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                  Text(
                    text = RoleStrings.orderStatusLabel(order.status, language),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }

              FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isRequester && order.status == "quoted") {
                  Button(
                    onClick = { onUpdateOrderStatus(order.id, "accepted") },
                    modifier = Modifier.height(48.dp),
                  ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(RoleStrings.tr(language, "قبول عرض السعر", "Accepter devis", "Accept Quote"))
                  }
                }
                if (order.status == "accepted") {
                  Button(
                    onClick = { onUpdateOrderStatus(order.id, "completed") },
                    modifier = Modifier.height(48.dp),
                  ) {
                    Text(RoleStrings.tr(language, "تأكيد الإنجاز والتسليم", "Clôturer livré", "Mark Completed"))
                  }
                }
                if (isRequester && order.status in listOf("pending", "quoted")) {
                  OutlinedButton(
                    onClick = { onUpdateOrderStatus(order.id, "cancelled") },
                    modifier = Modifier.height(48.dp),
                  ) {
                    Text(RoleStrings.tr(language, "إلغاء", "Annuler", "Cancel"))
                  }
                }
              }
            }
          }
        }
      }

      1 -> {
        if (openRoleOrders.isEmpty()) {
          Text(
            text =
              RoleStrings.tr(
                language,
                ar = "لا توجد طلبات مفتوحة موجهة لنشاط (${activeRole.titleAr}) حالياً.",
                fr = "Aucune demande ouverte pour (${activeRole.titleFr}).",
                en = "No open broadcast requests targeting (${activeRole.titleEn}) right now.",
              ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        openRoleOrders.forEach { order ->
          Card(
            shape = RoundedCornerShape(14.dp),
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
              ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                  text = "${order.itemTitle} • ${order.quantity} ${order.unitLabel}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "${order.requesterName} — ${order.wilaya}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Button(
                onClick = {
                  quoteModalOrder = order
                  quotePriceInput = order.proposedPriceDzd.toInt().toString()
                  quoteNotesInput = ""
                },
                modifier = Modifier.height(48.dp).testTag("submit_quote_btn_${order.id}"),
              ) {
                Text(RoleStrings.tr(language, "تقديم عرض سعر", "Proposer devis", "Submit Quote"))
              }
            }
          }
        }
      }

      2 -> {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text =
                RoleStrings.tr(
                  language,
                  ar = "إرسال طلب شراء جملة أو طلب عرض سعر / حجز تخزين",
                  fr = "Créer une demande d'achat, devis transport ou réservation froid",
                  en = "Create Purchase Request, Freight Quote, or Cold Storage Booking",
                ),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              UserRole.entries.forEach { target ->
                FilterChip(
                  selected = targetRoleCode == target.code,
                  onClick = { targetRoleCode = target.code },
                  label = { Text(target.localizedTitle(language)) },
                )
              }
            }

            OutlinedTextField(
              value = itemTitle,
              onValueChange = { itemTitle = it },
              label = { Text(RoleStrings.tr(language, "المنتج أو الخدمة المطلوبة", "Produit / Service", "Requested Item")) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("order_title_input"),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = { Text(RoleStrings.tr(language, "الكمية", "Quantité", "Quantity")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = unitText,
                onValueChange = { unitText = it },
                label = { Text(RoleStrings.tr(language, "الوحدة", "Unité", "Unit")) },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = budgetText,
                onValueChange = { budgetText = it },
                label = { Text("DZD") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
              )
            }

            OutlinedTextField(
              value = notesText,
              onValueChange = { notesText = it },
              label = { Text(RoleStrings.tr(language, "ملاحظات التسليم / الجودة", "Notes", "Delivery Notes")) },
              modifier = Modifier.fillMaxWidth(),
            )

            Button(
              onClick = {
                val orderType =
                  when (targetRoleCode) {
                    UserRole.TRANSPORTER.code -> "transport_quote"
                    UserRole.STORAGE_PROVIDER.code -> "storage_reservation"
                    UserRole.SUPPLIER.code -> "supply_order"
                    UserRole.SERVICE_PROVIDER.code -> "service_quote"
                    else -> "purchase_request"
                  }
                onCreateOrder(
                  MarketplaceOrder(
                    requesterName = requesterDisplayName,
                    requesterRole = activeRole.code,
                    providerId = "",
                    targetRole = targetRoleCode,
                    orderType = orderType,
                    itemTitle = itemTitle.ifBlank { "طلب توريد فلاحي" },
                    wilaya = defaultWilaya,
                    quantity = quantityText.replace(",", ".").toDoubleOrNull() ?: 1.0,
                    unitLabel = unitText,
                    proposedPriceDzd = budgetText.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    notes = notesText,
                  )
                )
                itemTitle = ""
                notesText = ""
                selectedTab = 0
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("create_order_submit_btn"),
            ) {
              Text(RoleStrings.tr(language, "إرسال الطلب للمتعاملين", "Envoyer la demande", "Submit Request"))
            }
          }
        }
      }
    }
  }

  val modalOrd = quoteModalOrder
  if (modalOrd != null) {
    AlertDialog(
      onDismissRequest = { quoteModalOrder = null },
      title = { Text(RoleStrings.tr(language, "تقديم عرض سعر رسمي", "Soumettre un devis", "Submit Quotation")) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("${modalOrd.itemTitle} (${modalOrd.quantity} ${modalOrd.unitLabel})")
          OutlinedTextField(
            value = quotePriceInput,
            onValueChange = { quotePriceInput = it },
            label = { Text(RoleStrings.tr(language, "السعر المقترح (د.ج)", "Montant proposé (DZD)", "Quoted Price (DZD)")) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          )
          OutlinedTextField(
            value = quoteNotesInput,
            onValueChange = { quoteNotesInput = it },
            label = { Text(RoleStrings.tr(language, "تفاصيل العرض وموعد التنفيذ", "Délais & conditions", "Quote Terms")) },
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val price = quotePriceInput.replace(",", ".").toDoubleOrNull() ?: modalOrd.proposedPriceDzd
            onSubmitQuote(modalOrd, price, quoteNotesInput)
            quoteModalOrder = null
          }
        ) {
          Text(RoleStrings.tr(language, "إرسال العرض", "Envoyer", "Send Quote"))
        }
      },
      dismissButton = {
        TextButton(onClick = { quoteModalOrder = null }) {
          Text(RoleStrings.tr(language, "إلغاء", "Annuler", "Cancel"))
        }
      },
    )
  }
}

/**
 * Shared Cross-Role Account Inbox (`SharedMessagesScreen`).
 */
@Composable
fun SharedMessagesScreen(
  language: AppLanguage,
  currentUserId: String,
  senderDisplayName: String,
  messages: List<PlatformMessage>,
  onSendMessage: (recipientId: String, recipientName: String, subject: String, body: String) -> Unit,
  onMarkRead: (String) -> Unit,
  onBackToDashboard: () -> Unit,
) {
  BackHandler(onBack = onBackToDashboard)

  var recipientIdInput by remember { mutableStateOf("") }
  var recipientNameInput by remember { mutableStateOf("") }
  var subjectInput by remember { mutableStateOf("") }
  var bodyInput by remember { mutableStateOf("") }

  Column(
    modifier = Modifier.fillMaxWidth().testTag("shared_messages_screen"),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text =
        RoleStrings.tr(
          language,
          ar = "صندوق الرسائل المشترك بين جميع أنشطتك",
          fr = "Messagerie partagée (tous vos rôles)",
          en = "Shared Account Messages Across Roles",
        ),
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.ExtraBold,
    )

    Card(
      shape = RoundedCornerShape(16.dp),
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = RoleStrings.tr(language, "مراسلة متعامل (فلاح / تاجر / مورد / ناقل)", "Nouveau message", "New Direct Message"),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = recipientNameInput,
            onValueChange = { recipientNameInput = it },
            label = { Text(RoleStrings.tr(language, "اسم المتعامل", "Destinataire", "Recipient Name")) },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = recipientIdInput,
            onValueChange = { recipientIdInput = it },
            label = { Text(RoleStrings.tr(language, "معرف المتعامل", "ID Opérateur", "Operator ID")) },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        OutlinedTextField(
          value = subjectInput,
          onValueChange = { subjectInput = it },
          label = { Text(RoleStrings.tr(language, "الموضوع", "Objet", "Subject")) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = bodyInput,
          onValueChange = { bodyInput = it },
          label = { Text(RoleStrings.tr(language, "نص الرسالة", "Message", "Message Body")) },
          modifier = Modifier.fillMaxWidth(),
        )
        Button(
          onClick = {
            val targetId = recipientIdInput.trim().ifBlank { currentUserId }
            if (bodyInput.isNotBlank()) {
              onSendMessage(
                targetId,
                recipientNameInput.ifBlank { senderDisplayName },
                subjectInput.ifBlank { "استفسار مهني" },
                bodyInput,
              )
              subjectInput = ""
              bodyInput = ""
            }
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.height(48.dp).testTag("send_direct_message_btn"),
        ) {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(RoleStrings.tr(language, "إرسال الرسالة", "Envoyer", "Send"))
        }
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

    messages.forEach { msg ->
      val isIncoming = msg.recipientId == currentUserId
      Card(
        shape = RoundedCornerShape(14.dp),
        colors =
          CardDefaults.cardColors(
            containerColor =
              if (isIncoming && !msg.isRead) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
              else MaterialTheme.colorScheme.surfaceContainerLowest
          ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "${msg.subject} • (${UserRole.fromCode(msg.senderRole).localizedTitle(language)})",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "${msg.senderName} → ${msg.recipientName}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(text = msg.body, style = MaterialTheme.typography.bodyMedium)
          }
          if (isIncoming && !msg.isRead) {
            IconButton(
              onClick = { onMarkRead(msg.id) },
              modifier = Modifier.size(48.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.MarkEmailRead,
                contentDescription = "Mark read",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }
  }
}

private fun rememberCategoryPresetsForRole(role: UserRole): List<Pair<String, String>> =
  when (role) {
    UserRole.TRANSPORTER ->
      listOf(
        "refrigerated_truck" to "شاحنة مبردة +4°C",
        "flatbed_truck" to "شاحنة مسطحة للصناديق",
        "grain_hopper" to "شاحنة حبوب سائبة",
      )
    UserRole.SUPPLIER ->
      listOf(
        "seeds_seedlings" to "البذور والشتلات المعتمدة",
        "fertilizers_npk" to "الأسمدة والمخصبات NPK",
        "drip_irrigation" to "معدات الري بالتقطير",
        "agri_tools" to "العتاد والأدوات الفلاحية",
      )
    UserRole.SERVICE_PROVIDER ->
      listOf(
        "plowing_leveling" to "حرث وتسوية الليزر",
        "harvesting" to "الحصاد والجني وكراء الآلات",
        "irrigation_maint" to "تركيب وصيانة الري",
        "soil_agronomy" to "تحاليل التربة واستشارة مهندس",
      )
    UserRole.STORAGE_PROVIDER ->
      listOf(
        "positive_cold_room" to "غرف تبريد موجبة (+2°C إلى +8°C)",
        "negative_cold_room" to "غرف تجميد سالبة (-18°C)",
        "dry_warehouse" to "مستودعات جافة ومهواة",
        "sorting_packaging" to "فرز وتدريج وتوضيب ما بعد الحصاد",
      )
    else -> listOf("general" to "عرض مهني عام")
  }
