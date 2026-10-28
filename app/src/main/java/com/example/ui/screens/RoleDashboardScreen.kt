package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AgriculturalParcel
import com.example.data.model.AppLanguage
import com.example.data.model.MarketListing
import com.example.data.model.MarketplaceOrder
import com.example.data.model.PartnerCatalogItem
import com.example.data.model.PlatformMessage
import com.example.data.model.RoleSpecificProfile
import com.example.data.model.ServiceBooking
import com.example.data.model.TransportRequest
import com.example.data.model.UserRole
import com.example.data.model.WorkspaceSection
import com.example.ui.viewmodel.FellahMatinViewModel
import com.example.util.RoleStrings
import java.util.Locale

private data class RoleQuickAction(
  val id: String,
  val labelAr: String,
  val labelFr: String,
  val labelEn: String,
  val icon: ImageVector,
  val targetSection: WorkspaceSection,
)

/**
 * Personalized Role Dashboard (`RoleDashboardScreen`) adapting KPIs, progressive profile prompts,
 * quick actions, and live operational feeds to the currently active professional role:
 * - `farmer`
 * - `trader`
 * - `transporter`
 * - `supplier`
 * - `service_provider`
 * - `storage_provider`
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoleDashboardScreen(
  activeRole: UserRole,
  assignedRoles: List<UserRole>,
  language: AppLanguage,
  operatorDisplayName: String,
  roleProfile: RoleSpecificProfile?,
  parcels: List<AgriculturalParcel>,
  marketListings: List<MarketListing>,
  partnerCatalog: List<PartnerCatalogItem>,
  myCatalogItems: List<PartnerCatalogItem>,
  myOrders: List<MarketplaceOrder>,
  openRoleOrders: List<MarketplaceOrder>,
  transportRequests: List<TransportRequest>,
  serviceBookings: List<ServiceBooking>,
  messages: List<PlatformMessage>,
  currentUserId: String,
  onNavigateToSection: (WorkspaceSection) -> Unit,
  onSwitchRole: (UserRole) -> Unit,
  onOpenPublishHarvestDialog: () -> Unit,
  modifier: Modifier = Modifier,
  matinViewModel: FellahMatinViewModel? = null,
) {
  val unreadMessages = messages.count { it.recipientId == currentUserId && !it.isRead }
  val myActiveCatalog =
    myCatalogItems.filter { it.providerRole == activeRole.code && it.status == "active" }
  val activeRoleOrders =
    myOrders.filter { it.requesterRole == activeRole.code || it.targetRole == activeRole.code }

  Column(
    modifier = modifier.fillMaxWidth().testTag("role_dashboard_${activeRole.code}"),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Role Hero Banner + Multi-Role Badges
    Card(
      shape = RoundedCornerShape(20.dp),
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      modifier =
        Modifier.fillMaxWidth()
          .border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            RoundedCornerShape(20.dp),
          ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(54.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = RoleStrings.iconForRole(activeRole),
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(28.dp),
                )
              }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text(
                text =
                  "${RoleStrings.activeRoleLabel(language)} ${activeRole.localizedTitle(language)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = roleProfile?.businessName?.takeIf { it.isNotBlank() } ?: operatorDisplayName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = activeRole.localizedDescription(language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        if (assignedRoles.size > 1) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            assignedRoles.forEach { r ->
              val isCurrent = r == activeRole
              Surface(
                shape = RoundedCornerShape(50),
                color =
                  if (isCurrent) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.surfaceContainer,
                modifier =
                  Modifier.clickable { if (!isCurrent) onSwitchRole(r) }
                    .testTag("dashboard_role_pill_${r.code}"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = RoleStrings.iconForRole(r),
                    contentDescription = null,
                    tint =
                      if (isCurrent) MaterialTheme.colorScheme.onPrimary
                      else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp),
                  )
                  Text(
                    text = r.localizedTitle(language),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color =
                      if (isCurrent) MaterialTheme.colorScheme.onPrimary
                      else MaterialTheme.colorScheme.onSurface,
                  )
                }
              }
            }
          }
        }
      }
    }

    // 1b. Fellah Matin (أخبار الصباح) — Visible Daily Agricultural News Feed near top of Dashboard
    if (matinViewModel != null) {
      FellahMatinDashboardSection(
        matinViewModel = matinViewModel,
        language = language,
        activeRole = activeRole,
        onViewAllNews = { onNavigateToSection(WorkspaceSection.FELLAH_MATIN) },
      )
    }

    // 2. Non-Blocking Progressive Profile Banner (if role profile incomplete)
    if (roleProfile?.isProfileComplete != true) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth().testTag("incomplete_role_profile_banner"),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f).padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.WarningAmber,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
            )
            Text(
              text = RoleStrings.incompleteProfileBannerTitle(language, activeRole),
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
          }
          OutlinedButton(
            onClick = { onNavigateToSection(WorkspaceSection.SETTINGS) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(48.dp).testTag("complete_profile_banner_btn"),
          ) {
            Text(RoleStrings.completeProfileCta(language))
          }
        }
      }
    }

    // 3. Role-Personalized KPI Strip
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      when (activeRole) {
        UserRole.FARMER -> {
          val totalHa = parcels.sumOf { it.areaHectares }
          val myListings = marketListings.count { it.userId == currentUserId && it.status == "active" }
          DashboardKpiCard(
            title = RoleStrings.tr(language, "القطع الموثقة", "Parcelles", "Parcels"),
            value = "${parcels.size} (${String.format(Locale.US, "%.1f", totalHa)} Ha)",
            modifier = Modifier.weight(1f),
          )
          DashboardKpiCard(
            title = RoleStrings.tr(language, "إعلانات المحاصيل", "Récoltes actives", "Active Harvests"),
            value = "$myListings",
            modifier = Modifier.weight(1f),
          )
          DashboardKpiCard(
            title = RoleStrings.tr(language, "حجوزات النقل والخدمات", "Services & Transport", "Bookings"),
            value = "${transportRequests.size + serviceBookings.size}",
            modifier = Modifier.weight(1f),
          )
        }
        UserRole.TRADER -> {
          val activeHarvests = marketListings.count { it.status == "active" }
          val purchaseOrders = myOrders.count { it.requesterId == currentUserId }
          DashboardKpiCard(
            title = RoleStrings.tr(language, "محاصيل متاحة للشراء", "Offres disponibles", "Available Harvests"),
            value = "$activeHarvests",
            modifier = Modifier.weight(1f),
          )
          DashboardKpiCard(
            title = RoleStrings.tr(language, "طلبات الشراء والتوريد", "Mes commandes", "My Orders"),
            value = "$purchaseOrders",
            modifier = Modifier.weight(1f),
          )
          DashboardKpiCard(
            title = RoleStrings.tr(language, "الشركاء والموردون", "Offres partenaires", "Partner Offers"),
            value = "${partnerCatalog.size}",
            modifier = Modifier.weight(1f),
          )
        }
        UserRole.TRANSPORTER -> {
          DashboardKpiCard(
            title = RoleStrings.tr(language, "مركبات وخطوط نشطة", "Véhicules publiés", "Active Fleet"),
            value = "${myActiveCatalog.size}",
            modifier = Modifier.weight(1f),
          )
          DashboardKpiCard(
            title = RoleStrings.tr(language, "طلبات نقل متاحة للتسعير", "Demandes ouvertes", "Open Freight"),
            value = "${openRoleOrders.size}",
            modifier = Modifier.weight(1f),
          )
          DashboardKpiCard(
            title = RoleStrings.tr(language, "شحنات قيد المتابعة", "Missions actives", "Assigned Jobs"),
            value = "${activeRoleOrders.size}",
            modifier = Modifier.weight(1f),
          )
        }
        UserRole.SUPPLIER -> {
          DashboardKpiCard(
            title = RoleStrings.tr(language, "منتجات ومستلزمات منشورة", "Produits actifs", "Published Inputs"),
            value = "${myActiveCatalog.size}",
            modifier = Modifier.weight(1f),
          )
          DashboardKpiCard(
            title = RoleStrings.tr(language, "طلبات التوريد الواردة", "Demandes clients", "Customer Requests"),
            value = "${openRoleOrders.size + activeRoleOrders.size}",
            modifier = Modifier.weight(1f),
          )
          DashboardKpiCard(
            title = RoleStrings.tr(language, "رسائل غير مقروءة", "Messages non lus", "Unread Messages"),
            value = "$unreadMessages",
            modifier = Modifier.weight(1f),
          )
        }
        UserRole.SERVICE_PROVIDER -> {
          DashboardKpiCard(
            title = RoleStrings.tr(language, "خدمات ميدانية منشورة", "Prestations actives", "Active Services"),
            value = "${myActiveCatalog.size}",
            modifier = Modifier.weight(1f),
          )
          DashboardKpiCard(
            title = RoleStrings.tr(language, "طلبات عروض أسعار", "Demandes de devis", "Quote Requests"),
            value = "${openRoleOrders.size}",
            modifier = Modifier.weight(1f),
          )
          DashboardKpiCard(
            title = RoleStrings.tr(language, "تدخلات ومهام مقبولة", "Missions acceptées", "Accepted Jobs"),
            value = "${activeRoleOrders.size}",
            modifier = Modifier.weight(1f),
          )
        }
        UserRole.STORAGE_PROVIDER -> {
          val totalTons = myActiveCatalog.sumOf { it.availableQuantityOrCapacity }
          DashboardKpiCard(
            title = RoleStrings.tr(language, "غرف ومخازن نشطة", "Chambres actives", "Active Facilities"),
            value = "${myActiveCatalog.size}",
            modifier = Modifier.weight(1f),
          )
          DashboardKpiCard(
            title = RoleStrings.tr(language, "السعة المتاحة للحجز", "Capacité dispo.", "Available Tonnage"),
            value = "${String.format(Locale.US, "%.0f", totalTons)} T",
            modifier = Modifier.weight(1f),
          )
          DashboardKpiCard(
            title = RoleStrings.tr(language, "طلبات حجز التبريد", "Réservations", "Storage Requests"),
            value = "${openRoleOrders.size + activeRoleOrders.size}",
            modifier = Modifier.weight(1f),
          )
        }
      }
    }

    // 4. Role-Specific Quick Actions Grid
    val quickActions = rememberQuickActionsForRole(activeRole)
    Card(
      shape = RoundedCornerShape(18.dp),
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
      modifier =
        Modifier.fillMaxWidth()
          .border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
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
              ar = "الإجراءات السريعة لنشاط (${activeRole.titleAr})",
              fr = "Actions rapides — ${activeRole.titleFr}",
              en = "Quick Actions — ${activeRole.titleEn}",
            ),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )

        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          quickActions.forEach { qa ->
            Button(
              onClick = {
                if (qa.id == "farmer_publish_harvest") {
                  onOpenPublishHarvestDialog()
                } else {
                  onNavigateToSection(qa.targetSection)
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp).testTag("quick_action_${qa.id}"),
            ) {
              Icon(qa.icon, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = RoleStrings.tr(language, qa.labelAr, qa.labelFr, qa.labelEn),
                fontWeight = FontWeight.SemiBold,
              )
            }
          }

          OutlinedButton(
            onClick = { onNavigateToSection(WorkspaceSection.MESSAGES_INBOX) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp).testTag("quick_action_shared_messages"),
          ) {
            Icon(Icons.Filled.Mail, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              RoleStrings.tr(
                language,
                ar = "الرسائل المشتركة ($unreadMessages)",
                fr = "Messages ($unreadMessages)",
                en = "Messages ($unreadMessages)",
              )
            )
          }
        }
      }
    }

    // 5. Relevant Live Activity / Marketplace Feed for Active Role
    Card(
      shape = RoundedCornerShape(18.dp),
      colors =
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
      modifier =
        Modifier.fillMaxWidth()
          .border(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            RoundedCornerShape(18.dp),
          ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text =
            RoleStrings.tr(
              language,
              ar = "المتابعة الفورية والفرص المرتبطة بنشاطك",
              fr = "Suivi opérationnel & opportunités du rôle",
              en = "Operational Activity & Role Opportunities",
            ),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )

        if (openRoleOrders.isNotEmpty()) {
          openRoleOrders.take(3).forEach { ord ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { onNavigateToSection(WorkspaceSection.ORDERS_AND_REQUESTS) },
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${ord.itemTitle} • ${ord.quantity} ${ord.unitLabel}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "${ord.requesterName} — ${ord.wilaya}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = RoleStrings.tr(language, "تقديم عرض سعر ←", "Proposer devis →", "Submit Quote →"),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        } else if (activeRole == UserRole.FARMER && parcels.isNotEmpty()) {
          val primaryParcel = parcels.first()
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier =
              Modifier.fillMaxWidth()
                .clickable { onNavigateToSection(WorkspaceSection.PARCEL_DELINEATION) },
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${primaryParcel.name} (${primaryParcel.ilotNumber})",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "${primaryParcel.wilaya} • ${primaryParcel.areaHectares} Ha • NDVI ${primaryParcel.ndviScore}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = RoleStrings.tr(language, "فتح الخريطة", "Ouvrir carte", "Open Map"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        } else {
          Text(
            text =
              RoleStrings.tr(
                language,
                ar = "لا توجد طلبات معلقة حاليًا. استخدم الإجراءات السريعة أعلاه لنشر عرض أو استعراض السوق.",
                fr = "Aucune demande en attente. Utilisez les actions rapides ci-dessus pour publier ou explorer.",
                en = "No pending items right now. Use the quick actions above to publish offers or browse the market.",
              ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun DashboardKpiCard(
  title: String,
  value: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    shadowElevation = 2.dp,
    modifier =
      modifier.border(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        RoundedCornerShape(14.dp),
      ),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

private fun rememberQuickActionsForRole(role: UserRole): List<RoleQuickAction> =
  when (role) {
    UserRole.FARMER ->
      listOf(
        RoleQuickAction(
          id = "farmer_parcel_map",
          labelAr = "ترسيم القطعة والتربة",
          labelFr = "Délimiter une parcelle",
          labelEn = "Parcel & Soil Map",
          icon = Icons.Filled.Map,
          targetSection = WorkspaceSection.PARCEL_DELINEATION,
        ),
        RoleQuickAction(
          id = "farmer_publish_harvest",
          labelAr = "نشر محصول للبيع",
          labelFr = "Publier une récolte",
          labelEn = "Publish Harvest",
          icon = Icons.Filled.Agriculture,
          targetSection = WorkspaceSection.PUBLIC_MARKET,
        ),
        RoleQuickAction(
          id = "farmer_request_services",
          labelAr = "طلب آلات / عمالة / تبريد",
          labelFr = "Matériel, main-d'œuvre & froid",
          labelEn = "Machinery, Labor & Cold Storage",
          icon = Icons.Filled.Handyman,
          targetSection = WorkspaceSection.SERVICES,
        ),
        RoleQuickAction(
          id = "farmer_request_transport",
          labelAr = "طلب نقل فلاحي",
          labelFr = "Demander un transport",
          labelEn = "Request Transport",
          icon = Icons.Filled.LocalShipping,
          targetSection = WorkspaceSection.TRANSPORT,
        ),
      )
    UserRole.TRADER ->
      listOf(
        RoleQuickAction(
          id = "trader_browse_market",
          labelAr = "تصفح ومقارنة المحاصيل",
          labelFr = "Parcourir & comparer",
          labelEn = "Browse & Compare Offers",
          icon = Icons.Filled.Storefront,
          targetSection = WorkspaceSection.PUBLIC_MARKET,
        ),
        RoleQuickAction(
          id = "trader_create_order",
          labelAr = "إنشاء طلب شراء جملة",
          labelFr = "Créer demande d'achat",
          labelEn = "Create Purchase Request",
          icon = Icons.Filled.Assignment,
          targetSection = WorkspaceSection.ORDERS_AND_REQUESTS,
        ),
        RoleQuickAction(
          id = "trader_book_transport",
          labelAr = "تنسيق شاحنة نقل",
          labelFr = "Organiser la livraison",
          labelEn = "Book Freight Delivery",
          icon = Icons.Filled.LocalShipping,
          targetSection = WorkspaceSection.TRANSPORT,
        ),
      )
    UserRole.TRANSPORTER ->
      listOf(
        RoleQuickAction(
          id = "transporter_publish_vehicle",
          labelAr = "تسجيل شاحنة / خط نقل",
          labelFr = "Publier camion / trajet",
          labelEn = "Register Vehicle / Route",
          icon = Icons.Filled.LocalShipping,
          targetSection = WorkspaceSection.PARTNER_CATALOG,
        ),
        RoleQuickAction(
          id = "transporter_view_quotes",
          labelAr = "طلبات النقل وتقديم الأسعار",
          labelFr = "Demandes & devis",
          labelEn = "Freight Requests & Quotes",
          icon = Icons.Filled.Assignment,
          targetSection = WorkspaceSection.ORDERS_AND_REQUESTS,
        ),
      )
    UserRole.SUPPLIER ->
      listOf(
        RoleQuickAction(
          id = "supplier_publish_input",
          labelAr = "نشر بذور / أسمدة / عتاد ري",
          labelFr = "Publier semences / engrais",
          labelEn = "Publish Seeds / Fertilizers",
          icon = Icons.Filled.Inventory2,
          targetSection = WorkspaceSection.PARTNER_CATALOG,
        ),
        RoleQuickAction(
          id = "supplier_manage_orders",
          labelAr = "إدارة طلبيات الفلاحين",
          labelFr = "Gérer les commandes",
          labelEn = "Manage Customer Orders",
          icon = Icons.Filled.Assignment,
          targetSection = WorkspaceSection.ORDERS_AND_REQUESTS,
        ),
      )
    UserRole.SERVICE_PROVIDER ->
      listOf(
        RoleQuickAction(
          id = "service_publish_offer",
          labelAr = "نشر خدمة فلاحية / هندسية",
          labelFr = "Publier une prestation",
          labelEn = "Publish Agricultural Service",
          icon = Icons.Filled.Handyman,
          targetSection = WorkspaceSection.PARTNER_CATALOG,
        ),
        RoleQuickAction(
          id = "service_quotes_jobs",
          labelAr = "طلبات الخدمة وعروض الأسعار",
          labelFr = "Demandes & interventions",
          labelEn = "Service Requests & Quotes",
          icon = Icons.Filled.Assignment,
          targetSection = WorkspaceSection.ORDERS_AND_REQUESTS,
        ),
      )
    UserRole.STORAGE_PROVIDER ->
      listOf(
        RoleQuickAction(
          id = "storage_publish_warehouse",
          labelAr = "نشر غرفة تبريد / مستودع",
          labelFr = "Publier chambre froide",
          labelEn = "Publish Cold Room / Warehouse",
          icon = Icons.Filled.AcUnit,
          targetSection = WorkspaceSection.PARTNER_CATALOG,
        ),
        RoleQuickAction(
          id = "storage_reservations",
          labelAr = "إدارة حجوزات التخزين والفرز",
          labelFr = "Gérer les réservations",
          labelEn = "Manage Storage Reservations",
          icon = Icons.Filled.AddBusiness,
          targetSection = WorkspaceSection.ORDERS_AND_REQUESTS,
        ),
      )
  }
