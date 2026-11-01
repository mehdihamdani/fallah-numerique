package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.AgriculturalParcel
import com.example.data.model.AppLanguage
import com.example.data.model.CropVarietyType
import com.example.data.model.FavoriteTargetType
import com.example.data.model.MarketListing
import com.example.data.model.MarketplaceOrder
import com.example.data.model.PartnerCatalogItem
import com.example.data.model.SoilTextureType
import com.example.data.model.UserFavoriteRecord
import com.example.data.model.UserRatingRecord
import com.example.data.model.UserReputationSummary
import com.example.data.model.UserRole
import com.example.ui.components.FavoriteToggleIconButton
import com.example.ui.components.OperatorReputationBadge
import com.example.ui.components.RateOperatorActionButton
import com.example.ui.components.RateOperatorDialog
import com.example.ui.viewmodel.UiState
import com.example.util.RoleStrings
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyParcelsRegistryScreen(
  parcelsState: UiState<List<AgriculturalParcel>>,
  searchQuery: String,
  onSelectParcel: (AgriculturalParcel) -> Unit,
  onDeleteParcel: (String) -> Unit,
  onNewParcel: () -> Unit,
  onBackToDelineation: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackToDelineation)

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "سجل القطع الفلاحية والمحاصيل المحفوظة",
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "Mes Parcelles & Récoltes Enregistrées • مزامنة سحابية فورية",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Button(
        onClick = onNewParcel,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(48.dp).testTag("registry_new_parcel_button"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("قطعة جديدة", style = MaterialTheme.typography.labelLarge)
      }
    }

    when (parcelsState) {
      is UiState.Loading -> {
        Box(
          modifier = Modifier.fillMaxWidth().padding(48.dp),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      }
      is UiState.Error -> {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = parcelsState.message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
          )
        }
      }
      is UiState.Success -> {
        val filtered =
          parcelsState.data.filter { p ->
            searchQuery.isBlank() ||
              p.name.contains(searchQuery, ignoreCase = true) ||
              p.wilaya.contains(searchQuery, ignoreCase = true) ||
              p.ilotNumber.contains(searchQuery, ignoreCase = true) ||
              p.mainCrop.contains(searchQuery, ignoreCase = true) ||
              CropVarietyType.resolveDisplayTitleAr(p.mainCrop, "غير محدد")
                .contains(searchQuery, ignoreCase = true)
          }

        val totalHa = filtered.sumOf { it.areaHectares }
        val totalTons = filtered.sumOf { it.areaHectares * it.estimatedYieldTonsPerHa }

        // Summary KPI Strip
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          SummaryMetricCard(
            label = "عدد القطع الموثقة",
            value = "${filtered.size}",
            unit = "قطعة",
            modifier = Modifier.weight(1f),
          )
          SummaryMetricCard(
            label = "المساحة الإجمالية",
            value = String.format(Locale.US, "%.2f", totalHa),
            unit = "Ha",
            modifier = Modifier.weight(1f),
          )
          SummaryMetricCard(
            label = "الإنتاج المتوقع",
            value = String.format(Locale.US, "%.1f", totalTons),
            unit = "طن",
            modifier = Modifier.weight(1f),
          )
        }

        if (filtered.isEmpty()) {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
              ),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Inventory2,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
              )
              Text(
                text = "لا توجد قطع فلاحية محفوظة بعد",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "ارسم حدود قطعتك الفلاحية في شاشة الترسيم واضغط على (حفظ وتثبيت القطعة الفلاحية) لتظهر هنا تلقائياً.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
              Button(onClick = onBackToDelineation, modifier = Modifier.height(48.dp)) {
                Icon(Icons.Filled.Map, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("العودة إلى نافذة الترسيم")
              }
            }
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            filtered.forEach { item ->
              val cropTitleAr = CropVarietyType.resolveDisplayTitleAr(item.mainCrop, "غير محدد")
              val soil = SoilTextureType.fromCode(item.soilTexture)
              Card(
                shape = RoundedCornerShape(16.dp),
                colors =
                  CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                  ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier =
                  Modifier.fillMaxWidth()
                    .border(
                      1.dp,
                      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                      RoundedCornerShape(16.dp),
                    )
                    .clickable { onSelectParcel(item) }
                    .testTag("saved_parcel_card_${item.id}"),
              ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "${item.name} • ${item.ilotNumber}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Text(
                        text = "${item.wilaya} — ${item.commune} (${item.tenureType})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                      Text(
                        text = "${String.format(Locale.US, "%.2f", item.areaHectares)} Ha",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    InfoTag("المحصول: $cropTitleAr")
                    InfoTag("التربة: ${soil.titleAr} (pH ${item.soilPh})")
                    InfoTag("NDVI ${String.format(Locale.US, "%.2f", item.ndviScore)}")
                    if (item.linkedToMarket) {
                      InfoTag("مرتبط بالسوق العام ✓", highlight = true)
                    }
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    IconButton(
                      onClick = { onDeleteParcel(item.id) },
                      modifier = Modifier.size(48.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "حذف القطعة",
                        tint = MaterialTheme.colorScheme.error,
                      )
                    }
                    OutlinedButton(
                      onClick = { onSelectParcel(item) },
                      modifier = Modifier.height(48.dp),
                    ) {
                      Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("فتح وتدقيق البطاقة")
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

private data class PendingRatingTarget(
  val targetUserId: String,
  val targetName: String,
  val targetRole: UserRole,
  val contextItemId: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublicMarketScreen(
  listingsState: UiState<List<MarketListing>>,
  currentUserId: String,
  searchQuery: String,
  onOpenPublishModal: () -> Unit,
  onMarkSold: (String) -> Unit,
  onBackToDelineation: () -> Unit,
  modifier: Modifier = Modifier,
  partnerCatalogState: UiState<List<PartnerCatalogItem>> = UiState.Success(emptyList()),
  activeRole: UserRole = UserRole.FARMER,
  language: AppLanguage = AppLanguage.AR,
  requesterDisplayName: String = "Opérateur",
  onSubmitMarketOrder: (MarketplaceOrder) -> Unit = {},
  reputationByUser: Map<String, UserReputationSummary> = emptyMap(),
  publishedRatings: List<UserRatingRecord> = emptyList(),
  userFavorites: List<UserFavoriteRecord> = emptyList(),
  onSubmitRating: (
    targetUserId: String,
    targetName: String,
    targetRole: String,
    stars: Int,
    comment: String,
    contextItemId: String,
  ) -> Unit = { _, _, _, _, _, _ -> },
  onToggleFavorite: (
    favoriteType: FavoriteTargetType,
    targetId: String,
    targetOwnerId: String,
    targetTitle: String,
    targetSubtitle: String,
    targetRole: String,
  ) -> Unit = { _, _, _, _, _, _ -> },
  onRemoveFavorite: (String) -> Unit = {},
) {
  BackHandler(onBack = onBackToDelineation)

  var selectedCropFilter by remember { mutableStateOf<String?>(null) }
  var marketSubTab by remember { mutableStateOf(0) } // 0 = Harvests, 1 = Partner Catalog, 2 = Favorites
  var favoriteFilterType by remember { mutableStateOf<FavoriteTargetType?>(null) }
  var pendingRatingTarget by remember { mutableStateOf<PendingRatingTarget?>(null) }

  val favoritedKeys =
    remember(userFavorites) {
      userFavorites.map { "${it.favoriteType}_${it.targetId}" }.toSet()
    }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text =
            RoleStrings.tr(
              language,
              ar = "السوق العام الفلاحي وعروض الشركاء",
              fr = "Marché Public Agricole & Offres Partenaires",
              en = "Public Marketplace & Partner Offers",
            ),
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text =
            RoleStrings.tr(
              language,
              ar = "محاصيل الفلاحين الموثقة + كتالوج الشركاء + التنقيط بالنجوم والمفضلة",
              fr = "Récoltes certifiées + Catalogue partenaires + Étoiles & Favoris",
              en = "Certified harvests + Partner catalog + Star Ratings & Favorites",
            ),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (activeRole == UserRole.FARMER) {
        Button(
          onClick = onOpenPublishModal,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.height(48.dp).testTag("market_publish_button"),
        ) {
          Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("نشر محصول القطعة", style = MaterialTheme.typography.labelLarge)
        }
      }
    }

    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      FilterChip(
        selected = marketSubTab == 0,
        onClick = { marketSubTab = 0 },
        label = {
          Text(RoleStrings.tr(language, "عروض المحاصيل الفلاحية", "Récoltes Agricoles", "Harvest Listings"))
        },
        modifier = Modifier.testTag("market_subtab_harvests"),
      )
      FilterChip(
        selected = marketSubTab == 1,
        onClick = { marketSubTab = 1 },
        label = {
          Text(
            RoleStrings.tr(
              language,
              ar = "مستلزمات، نقل وتبريد الشركاء",
              fr = "Intrants, Transport & Froid",
              en = "Supplies, Freight & Storage",
            )
          )
        },
        modifier = Modifier.testTag("market_subtab_partners"),
      )
      FilterChip(
        selected = marketSubTab == 2,
        onClick = { marketSubTab = 2 },
        leadingIcon = {
          Icon(
            imageVector =
              if (marketSubTab == 2) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = null,
            tint =
              if (marketSubTab == 2) Color(0xFFD32F2F)
              else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
          )
        },
        label = {
          Text(
            RoleStrings.tr(
              language,
              ar = "المفضلة (${userFavorites.size})",
              fr = "Favoris (${userFavorites.size})",
              en = "Favorites (${userFavorites.size})",
            )
          )
        },
        modifier = Modifier.testTag("market_subtab_favorites"),
      )
    }

    if (marketSubTab == 2) {
      // Dedicated Favorites Tab (Operators, Harvest Listings, Partner Offers)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = favoriteFilterType == null,
          onClick = { favoriteFilterType = null },
          label = { Text(RoleStrings.tr(language, "كل المفضلة", "Tous", "All")) },
          modifier = Modifier.testTag("favorites_filter_all"),
        )
        FavoriteTargetType.entries.forEach { favType ->
          FilterChip(
            selected = favoriteFilterType == favType,
            onClick = {
              favoriteFilterType = if (favoriteFilterType == favType) null else favType
            },
            label = { Text(favType.localizedLabel(language)) },
            modifier = Modifier.testTag("favorites_filter_${favType.code}"),
          )
        }
      }

      val filteredFavorites =
        userFavorites.filter { fav ->
          (favoriteFilterType == null || fav.favoriteType == favoriteFilterType?.code) &&
            (searchQuery.isBlank() ||
              fav.targetTitle.contains(searchQuery, ignoreCase = true) ||
              fav.targetSubtitle.contains(searchQuery, ignoreCase = true))
        }

      if (filteredFavorites.isEmpty()) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
          modifier = Modifier.fillMaxWidth().testTag("favorites_empty_card"),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.BookmarkBorder,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(44.dp),
            )
            Text(
              text =
                RoleStrings.tr(
                  language,
                  ar = "قائمة المفضلة فارغة حالياً",
                  fr = "Aucun favori enregistré pour le moment",
                  en = "No saved favorites yet",
                ),
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text =
                RoleStrings.tr(
                  language,
                  ar = "اضغط على أيقونة القلب (❤️) بجانب أي محصول أو عرض شريك أو متعامل لحفظه هنا والرجوع إليه سريعاً.",
                  fr = "Appuyez sur le cœur (❤️) à côté d'une récolte, d'une offre ou d'un opérateur pour le retrouver ici.",
                  en = "Tap the heart (❤️) next to any harvest, partner offer, or operator to bookmark it here.",
                ),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          filteredFavorites.forEach { fav ->
            val favType = FavoriteTargetType.fromCode(fav.favoriteType)
            val ownerReputation =
              reputationByUser[fav.targetOwnerId]
                ?: UserReputationSummary(targetUserId = fav.targetOwnerId)
            val roleEnum = UserRole.fromCode(fav.targetRole)

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
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    RoundedCornerShape(16.dp),
                  )
                  .testTag("favorite_saved_card_${fav.id}"),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                      Text(
                        text =
                          "${favType.localizedLabel(language)} • ${roleEnum.localizedTitle(language)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                      )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = fav.targetTitle,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                    if (fav.targetSubtitle.isNotBlank()) {
                      Text(
                        text = fav.targetSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }

                  IconButton(
                    onClick = { onRemoveFavorite(fav.id) },
                    modifier = Modifier.size(48.dp).testTag("remove_favorite_${fav.id}"),
                  ) {
                    Icon(
                      imageVector = Icons.Filled.DeleteOutline,
                      contentDescription =
                        RoleStrings.tr(language, "إزالة من المفضلة", "Retirer", "Remove"),
                      tint = MaterialTheme.colorScheme.error,
                    )
                  }
                }

                OperatorReputationBadge(summary = ownerReputation, language = language)

                if (fav.targetOwnerId.isNotBlank() && fav.targetOwnerId != currentUserId) {
                  val hasMyRating =
                    publishedRatings.any {
                      it.reviewerId == currentUserId && it.targetUserId == fav.targetOwnerId
                    }
                  RateOperatorActionButton(
                    targetIdForTag = "fav_${fav.id}",
                    hasExistingRatingByMe = hasMyRating,
                    language = language,
                    onClick = {
                      pendingRatingTarget =
                        PendingRatingTarget(
                          targetUserId = fav.targetOwnerId,
                          targetName = fav.targetTitle,
                          targetRole = roleEnum,
                          contextItemId = fav.targetId,
                        )
                    },
                  )
                }
              }
            }
          }
        }
      }
    } else if (marketSubTab == 1) {
      val catalogItems =
        ((partnerCatalogState as? UiState.Success)?.data ?: emptyList()).filter { item ->
          searchQuery.isBlank() ||
            item.title.contains(searchQuery, ignoreCase = true) ||
            item.providerName.contains(searchQuery, ignoreCase = true) ||
            item.wilaya.contains(searchQuery, ignoreCase = true)
        }
      if (catalogItems.isEmpty()) {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text =
                RoleStrings.tr(
                  language,
                  ar = "لا توجد عروض شركاء منشورة مطابقة للبحث حالياً",
                  fr = "Aucune offre partenaire active pour le moment",
                  en = "No matching partner catalog offers right now",
                ),
              style = MaterialTheme.typography.titleMedium,
            )
          }
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          catalogItems.forEach { pItem ->
            val providerRoleEnum = UserRole.fromCode(pItem.providerRole)
            val providerReputation =
              reputationByUser[pItem.userId]
                ?: UserReputationSummary(targetUserId = pItem.userId)
            val isOfferFav =
              favoritedKeys.contains("${FavoriteTargetType.PARTNER_ITEM.code}_${pItem.id}")
            val isOperatorFav =
              favoritedKeys.contains("${FavoriteTargetType.OPERATOR.code}_${pItem.userId}")
            val hasMyRating =
              publishedRatings.any {
                it.reviewerId == currentUserId && it.targetUserId == pItem.userId
              }

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
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    RoundedCornerShape(16.dp),
                  ),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "${pItem.title} • ${pItem.providerName}",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                    )
                    Text(
                      text = "${providerRoleEnum.localizedTitle(language)} — ${pItem.wilaya}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    FavoriteToggleIconButton(
                      isFavorited = isOfferFav,
                      favoriteType = FavoriteTargetType.PARTNER_ITEM,
                      targetId = pItem.id,
                      language = language,
                      onToggle = {
                        onToggleFavorite(
                          FavoriteTargetType.PARTNER_ITEM,
                          pItem.id,
                          pItem.userId,
                          "${pItem.title} • ${pItem.providerName}",
                          "${providerRoleEnum.localizedTitle(language)} — ${pItem.wilaya} (${pItem.priceDzd.toInt()} ${pItem.unitLabel})",
                          pItem.providerRole,
                        )
                      },
                    )
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                      Text(
                        text = "${pItem.priceDzd.toInt()} ${pItem.unitLabel}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                      )
                    }
                  }
                }

                OperatorReputationBadge(summary = providerReputation, language = language)

                if (pItem.description.isNotBlank()) {
                  Text(pItem.description, style = MaterialTheme.typography.bodySmall)
                }
                if (pItem.userId.isNotBlank() && pItem.userId != currentUserId) {
                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    OutlinedButton(
                      onClick = {
                        onSubmitMarketOrder(
                          MarketplaceOrder(
                            requesterName = requesterDisplayName,
                            requesterRole = activeRole.code,
                            providerId = pItem.userId,
                            targetRole = pItem.providerRole,
                            targetItemId = pItem.id,
                            orderType =
                              when (pItem.providerRole) {
                                UserRole.TRANSPORTER.code -> "transport_quote"
                                UserRole.STORAGE_PROVIDER.code -> "storage_reservation"
                                UserRole.SUPPLIER.code -> "supply_order"
                                else -> "service_quote"
                              },
                            itemTitle = pItem.title,
                            wilaya = pItem.wilaya,
                            quantity = 1.0,
                            unitLabel = pItem.unitLabel,
                            proposedPriceDzd = pItem.priceDzd,
                            notes = "طلب مباشر من السوق العام",
                          )
                        )
                      },
                      modifier = Modifier.height(48.dp).testTag("order_partner_item_${pItem.id}"),
                    ) {
                      Text(
                        RoleStrings.tr(
                          language,
                          ar = "طلب / حجز مباشر",
                          fr = "Commander / Réserver",
                          en = "Order / Reserve",
                        )
                      )
                    }

                    RateOperatorActionButton(
                      targetIdForTag = pItem.id,
                      hasExistingRatingByMe = hasMyRating,
                      language = language,
                      onClick = {
                        pendingRatingTarget =
                          PendingRatingTarget(
                            targetUserId = pItem.userId,
                            targetName = pItem.providerName.ifBlank { pItem.title },
                            targetRole = providerRoleEnum,
                            contextItemId = pItem.id,
                          )
                      },
                    )

                    OutlinedButton(
                      onClick = {
                        onToggleFavorite(
                          FavoriteTargetType.OPERATOR,
                          pItem.userId,
                          pItem.userId,
                          pItem.providerName.ifBlank { pItem.title },
                          "${providerRoleEnum.localizedTitle(language)} • ${pItem.wilaya}",
                          pItem.providerRole,
                        )
                      },
                      shape = RoundedCornerShape(10.dp),
                      modifier =
                        Modifier.height(48.dp)
                          .testTag("favorite_operator_partner_${pItem.id}"),
                    ) {
                      Icon(
                        imageVector =
                          if (isOperatorFav) Icons.Filled.Favorite
                          else Icons.Filled.PersonAddAlt1,
                        contentDescription = null,
                        tint =
                          if (isOperatorFav) Color(0xFFD32F2F)
                          else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        if (isOperatorFav) {
                          RoleStrings.tr(language, "متعامل مفضل ❤️", "Opérateur favori ❤️", "Saved Operator ❤️")
                        } else {
                          RoleStrings.tr(language, "تفضيل المتعامل", "Suivre l'opérateur", "Favorite Operator")
                        }
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    } else {
      // marketSubTab == 0: Harvest Listings
      val activeCustomCrops =
        (listingsState as? UiState.Success)
          ?.data
          ?.map { it.cropType.trim() }
          ?.filter { CropVarietyType.isCustomCrop(it) }
          ?.distinct()
          .orEmpty()

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        FilterChip(
          selected = selectedCropFilter == null,
          onClick = { selectedCropFilter = null },
          label = { Text("الكل (Tous)") },
        )
        CropVarietyType.entries.forEach { crop ->
          FilterChip(
            selected = selectedCropFilter == crop.code,
            onClick = {
              selectedCropFilter = if (selectedCropFilter == crop.code) null else crop.code
            },
            label = { Text(crop.titleAr) },
          )
        }
        activeCustomCrops.forEach { customCrop ->
          FilterChip(
            selected = selectedCropFilter.equals(customCrop, ignoreCase = true),
            onClick = {
              selectedCropFilter =
                if (selectedCropFilter.equals(customCrop, ignoreCase = true)) null else customCrop
            },
            label = { Text(customCrop) },
          )
        }
      }

      when (listingsState) {
        is UiState.Loading -> {
          Box(
            modifier = Modifier.fillMaxWidth().padding(48.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
        }
        is UiState.Error -> {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = listingsState.message,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
        is UiState.Success -> {
          val filtered =
            listingsState.data.filter { l ->
              val resolvedCropLabel = CropVarietyType.resolveDisplayTitleAr(l.cropType)
              (selectedCropFilter == null || l.cropType.trim().equals(selectedCropFilter, ignoreCase = true)) &&
                (searchQuery.isBlank() ||
                  l.operatorName.contains(searchQuery, ignoreCase = true) ||
                  l.wilaya.contains(searchQuery, ignoreCase = true) ||
                  l.parcelReference.contains(searchQuery, ignoreCase = true) ||
                  l.cropType.contains(searchQuery, ignoreCase = true) ||
                  resolvedCropLabel.contains(searchQuery, ignoreCase = true))
            }

          if (filtered.isEmpty()) {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors =
                CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Storefront,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(48.dp),
                )
                Text(
                  text = "لا توجد إعلانات بيع نشطة مطابقة حالياً",
                  style = MaterialTheme.typography.titleLarge,
                )
                Text(
                  text = "اربط قطعتك الفلاحية بإعلان بيع رسمي لعرض الكمية المتوقعة وسعر الكيلوغرام للمشترين والتعاونيات.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                )
                Button(onClick = onOpenPublishModal, modifier = Modifier.height(48.dp)) {
                  Text("ربط القطعة الحالية بإعلان بيع")
                }
              }
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              filtered.forEach { listing ->
                val cropTitleAr = CropVarietyType.resolveDisplayTitleAr(listing.cropType)
                val isMine = listing.userId == currentUserId
                val farmerReputation =
                  reputationByUser[listing.userId]
                    ?: UserReputationSummary(targetUserId = listing.userId)
                val isListingFav =
                  favoritedKeys.contains("${FavoriteTargetType.MARKET_LISTING.code}_${listing.id}")
                val isOperatorFav =
                  favoritedKeys.contains("${FavoriteTargetType.OPERATOR.code}_${listing.userId}")
                val hasMyRating =
                  publishedRatings.any {
                    it.reviewerId == currentUserId && it.targetUserId == listing.userId
                  }

                Card(
                  shape = RoundedCornerShape(16.dp),
                  colors =
                    CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                  elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                  modifier =
                    Modifier.fillMaxWidth()
                      .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        RoundedCornerShape(16.dp),
                      )
                      .testTag("market_listing_card_${listing.id}"),
                ) {
                  Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        Icon(
                          Icons.Filled.Verified,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(22.dp),
                        )
                        Column {
                          Text(
                            text = "$cropTitleAr • ${listing.operatorName}",
                            style = MaterialTheme.typography.titleLarge,
                          )
                          Text(
                            text = "${listing.wilaya} • مرجع القطعة: ${listing.parcelReference}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                          )
                        }
                      }

                      Row(verticalAlignment = Alignment.CenterVertically) {
                        FavoriteToggleIconButton(
                          isFavorited = isListingFav,
                          favoriteType = FavoriteTargetType.MARKET_LISTING,
                          targetId = listing.id,
                          language = language,
                          onToggle = {
                            onToggleFavorite(
                              FavoriteTargetType.MARKET_LISTING,
                              listing.id,
                              listing.userId,
                              "$cropTitleAr • ${listing.operatorName}",
                              "${listing.wilaya} • ${listing.pricePerKgDzd.toInt()} د.ج/كغ (${String.format(Locale.US, "%.1f", listing.availableTons)} طن)",
                              UserRole.FARMER.code,
                            )
                          },
                        )
                        Surface(
                          shape = RoundedCornerShape(8.dp),
                          color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                          Text(
                            text = "${listing.pricePerKgDzd.toInt()} د.ج / كغ",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                          )
                        }
                      }
                    }

                    OperatorReputationBadge(summary = farmerReputation, language = language)

                    FlowRow(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      InfoTag(
                        "الكمية المتوفرة: ${String.format(Locale.US, "%.1f", listing.availableTons)} طن"
                      )

                      FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        if (isMine && listing.status == "active") {
                          OutlinedButton(
                            onClick = { onMarkSold(listing.id) },
                            modifier = Modifier.height(48.dp),
                          ) {
                            Icon(
                              Icons.Filled.CheckCircle,
                              contentDescription = null,
                              modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تأكيد البيع (Vendu)")
                          }
                        } else if (!isMine && listing.status == "active") {
                          if (listing.userId.isNotBlank()) {
                            RateOperatorActionButton(
                              targetIdForTag = listing.id,
                              hasExistingRatingByMe = hasMyRating,
                              language = language,
                              onClick = {
                                pendingRatingTarget =
                                  PendingRatingTarget(
                                    targetUserId = listing.userId,
                                    targetName = listing.operatorName,
                                    targetRole = UserRole.FARMER,
                                    contextItemId = listing.id,
                                  )
                              },
                            )

                            OutlinedButton(
                              onClick = {
                                onToggleFavorite(
                                  FavoriteTargetType.OPERATOR,
                                  listing.userId,
                                  listing.userId,
                                  listing.operatorName,
                                  "${listing.wilaya} • الفلاح ($cropTitleAr)",
                                  UserRole.FARMER.code,
                                )
                              },
                              shape = RoundedCornerShape(10.dp),
                              modifier =
                                Modifier.height(48.dp)
                                  .testTag("favorite_operator_listing_${listing.id}"),
                            ) {
                              Icon(
                                imageVector =
                                  if (isOperatorFav) Icons.Filled.Favorite
                                  else Icons.Filled.PersonAddAlt1,
                                contentDescription = null,
                                tint =
                                  if (isOperatorFav) Color(0xFFD32F2F)
                                  else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                              )
                              Spacer(modifier = Modifier.width(6.dp))
                              Text(
                                if (isOperatorFav) {
                                  RoleStrings.tr(language, "فلاح مفضل ❤️", "Favori ❤️", "Saved ❤️")
                                } else {
                                  RoleStrings.tr(language, "تفضيل الفلاح", "Suivre", "Favorite Farmer")
                                }
                              )
                            }
                          }

                          Button(
                            onClick = {
                              onSubmitMarketOrder(
                                MarketplaceOrder(
                                  requesterName = requesterDisplayName,
                                  requesterRole = activeRole.code,
                                  providerId = listing.userId,
                                  targetRole = UserRole.FARMER.code,
                                  targetItemId = listing.id,
                                  orderType = "purchase_request",
                                  itemTitle = "$cropTitleAr (${listing.operatorName})",
                                  wilaya = listing.wilaya,
                                  quantity = listing.availableTons.coerceAtMost(5.0),
                                  unitLabel = "طن",
                                  proposedPriceDzd = listing.pricePerKgDzd * 1000.0,
                                  notes = "طلب شراء مباشر من السوق العام",
                                )
                              )
                            },
                            modifier = Modifier.height(48.dp).testTag("buy_harvest_${listing.id}"),
                          ) {
                            Text(
                              RoleStrings.tr(
                                language,
                                ar = "طلب شراء المحصول",
                                fr = "Acheter / Commander",
                                en = "Purchase Request",
                              )
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
        }
      }
    }
  }

  val ratingTarget = pendingRatingTarget
  if (ratingTarget != null) {
    val targetSummary =
      reputationByUser[ratingTarget.targetUserId]
        ?: UserReputationSummary(targetUserId = ratingTarget.targetUserId)
    val myExisting =
      publishedRatings.firstOrNull {
        it.reviewerId == currentUserId && it.targetUserId == ratingTarget.targetUserId
      }
    RateOperatorDialog(
      targetUserId = ratingTarget.targetUserId,
      targetName = ratingTarget.targetName,
      targetRole = ratingTarget.targetRole,
      language = language,
      reputationSummary = targetSummary,
      existingMyRating = myExisting,
      onDismiss = { pendingRatingTarget = null },
      onSubmitRating = { stars, comment ->
        onSubmitRating(
          ratingTarget.targetUserId,
          ratingTarget.targetName,
          ratingTarget.targetRole.code,
          stars,
          comment,
          ratingTarget.contextItemId,
        )
        pendingRatingTarget = null
      },
    )
  }
}

@Composable
private fun SummaryMetricCard(
  label: String,
  value: String,
  unit: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    shadowElevation = 2.dp,
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = value,
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.ExtraBold,
        )
        Text(
          text = unit,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(bottom = 4.dp),
        )
      }
    }
  }
}

@Composable
private fun InfoTag(text: String, highlight: Boolean = false) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color =
      if (highlight) MaterialTheme.colorScheme.primaryContainer
      else MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.SemiBold,
      color =
        if (highlight) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    )
  }
}

@Composable
fun NdviHistoryDialog(
  parcel: AgriculturalParcel,
  onDismiss: () -> Unit,
) {
  val readings =
    listOf(
      "ماي" to 0.61f,
      "جوان" to 0.65f,
      "جويلية" to 0.68f,
      "أوت" to 0.70f,
      "سبتمبر" to 0.72f,
      "أكتوبر" to parcel.ndviScore.toFloat().coerceIn(0.2f, 0.95f),
    )
  val primaryColor = MaterialTheme.colorScheme.primary

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(Icons.Filled.Eco, contentDescription = null, tint = primaryColor)
        Text("سجل القمر الصناعي Sentinel-2 NDVI")
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "تطور مؤشر الكتلة الحيوية والكلوروفيل لقطعة ${parcel.ilotNumber} خلال 6 أشهر:",
          style = MaterialTheme.typography.bodyMedium,
        )
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier = Modifier.fillMaxWidth().height(160.dp),
        ) {
          Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val stepX = size.width / (readings.size - 1).coerceAtLeast(1)
            val linePath = Path()
            readings.forEachIndexed { idx, (_, score) ->
              val x = idx * stepX
              val y = size.height * (1f - ((score - 0.4f) / 0.5f).coerceIn(0.05f, 0.95f))
              if (idx == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }
            drawPath(
              path = linePath,
              brush = Brush.horizontalGradient(listOf(Color(0xFF2B6A45), primaryColor)),
              style = Stroke(width = 3.5.dp.toPx()),
            )
            readings.forEachIndexed { idx, (_, score) ->
              val x = idx * stepX
              val y = size.height * (1f - ((score - 0.4f) / 0.5f).coerceIn(0.05f, 0.95f))
              drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = Offset(x, y))
            }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          readings.forEach { (month, score) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(month, style = MaterialTheme.typography.bodySmall)
              Text(
                String.format(Locale.US, "%.2f", score),
                style = MaterialTheme.typography.labelSmall,
                color = primaryColor,
              )
            }
          }
        }
        Text(
          text = "التشخيص الطيفي: تجانس رطوبي ممتاز تحت ظلال النخيل مع نشاط تمثيل ضوئي مرتفع (> 0.70).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) { Text("إغلاق") }
    },
  )
}

@Composable
fun CadastralEditDialog(
  parcel: AgriculturalParcel,
  onDismiss: () -> Unit,
  onSave: (String, String, String, String, String, String) -> Unit,
) {
  var name by remember { mutableStateOf(parcel.name) }
  var wilaya by remember { mutableStateOf(parcel.wilaya) }
  var commune by remember { mutableStateOf(parcel.commune) }
  var ilot by remember { mutableStateOf(parcel.ilotNumber) }
  var tenure by remember { mutableStateOf(parcel.tenureType) }
  var wellDepth by remember { mutableStateOf(parcel.wellDepthLabel) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("تعديل البيانات العقارية والإدارية") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("تسمية المحيط / القطاع") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = wilaya,
          onValueChange = { wilaya = it },
          label = { Text("الولاية (Wilaya)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = commune,
          onValueChange = { commune = it },
          label = { Text("البلدية والمنطقة") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = ilot,
          onValueChange = { ilot = it },
          label = { Text("رقم القطعة / الجزيرة (Îlot)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = tenure,
          onValueChange = { tenure = it },
          label = { Text("طبيعة الحيازة العقارية") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = wellDepth,
          onValueChange = { wellDepth = it },
          label = { Text("بيانات بئر السقي (Forage)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(onClick = { onSave(name, wilaya, commune, ilot, tenure, wellDepth) }) {
        Text("حفظ التعديلات")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("إلغاء") }
    },
  )
}

@Composable
fun PublishMarketDialog(
  parcel: AgriculturalParcel,
  defaultOperatorName: String,
  onDismiss: () -> Unit,
  onConfirmPublish: (String, Double, Double, String) -> Unit,
) {
  val initialCropText =
    remember(parcel.id, parcel.mainCrop) {
      if (parcel.mainCrop.isBlank()) {
        ""
      } else {
        CropVarietyType.resolveDisplayTitleAr(parcel.mainCrop, "")
      }
    }
  val defaultPriceKg =
    remember(parcel.mainCrop) { CropVarietyType.resolveDefaultPriceKgDzd(parcel.mainCrop) }
  val defaultTons =
    remember(parcel.areaHectares, parcel.estimatedYieldTonsPerHa) {
      String.format(
        Locale.US,
        "%.1f",
        (parcel.areaHectares * parcel.estimatedYieldTonsPerHa).coerceAtLeast(1.0),
      )
    }
  var cropNameInput by remember(parcel.id, parcel.mainCrop) { mutableStateOf(initialCropText) }
  var operatorName by remember { mutableStateOf(defaultOperatorName) }
  var tonsInput by remember { mutableStateOf(defaultTons) }
  var priceInput by
    remember(defaultPriceKg) { mutableStateOf(String.format(Locale.US, "%.0f", defaultPriceKg)) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("ربط القطعة بإعلان بيع في السوق العام") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text =
            if (cropNameInput.trim().isEmpty()) {
              "اكتب اسم منتوجك الفلاحي الخاص بحرية ليتم توثيق العرض بمرجع القطعة (${parcel.ilotNumber})."
            } else {
              "سيتم توثيق العرض تلقائياً بمرجع القطعة (${parcel.ilotNumber}) ومنتوج (${cropNameInput.trim()})."
            },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = cropNameInput,
          onValueChange = { cropNameInput = it.take(80) },
          label = { Text("اسم المنتوج / المحصول المعروض للبيع") },
          placeholder = { Text("مثال: قمح صلب، بطيخ أحمر، ثوم، دقلة نور...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("publish_market_crop_input"),
        )
        OutlinedTextField(
          value = operatorName,
          onValueChange = { operatorName = it },
          label = { Text("اسم المستثمرة أو التعاونية الفلاحية") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = tonsInput,
          onValueChange = { tonsInput = it },
          label = { Text("الكمية المعروضة للبيع (طن)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = priceInput,
          onValueChange = { priceInput = it },
          label = { Text("السعر المرجعي (د.ج / كغ)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val trimmedCrop = cropNameInput.trim()
          val matchedPresetCode =
            CropVarietyType.entries
              .firstOrNull {
                it.titleAr == trimmedCrop || it.code.equals(trimmedCrop, ignoreCase = true)
              }
              ?.code
          val resolvedCropToSave = matchedPresetCode ?: trimmedCrop
          val tons = tonsInput.replace(",", ".").toDoubleOrNull() ?: 10.0
          val price = priceInput.replace(",", ".").toDoubleOrNull() ?: defaultPriceKg
          onConfirmPublish(operatorName, tons, price, resolvedCropToSave)
        },
        enabled = cropNameInput.trim().isNotEmpty(),
        modifier = Modifier.testTag("confirm_market_publish_button"),
      ) {
        Text("نشر في السوق العام")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("إلغاء") }
    },
  )
}
