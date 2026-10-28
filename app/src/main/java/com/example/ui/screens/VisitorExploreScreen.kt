package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.ai.FellahPlatformDataSnapshot
import com.example.data.model.AgriServiceCategory
import com.example.data.model.AgriServiceDomain
import com.example.data.model.AppLanguage
import com.example.data.model.CropVarietyType
import com.example.data.model.UserRole
import com.example.data.model.WorkspaceSection
import com.example.data.news.FellahMatinStrings
import com.example.ui.viewmodel.FellahAiViewModel
import com.example.ui.viewmodel.FellahMatinViewModel
import com.example.util.RoleStrings

private data class PublicMarketPreviewItem(
  val id: String,
  val titleAr: String,
  val titleFr: String,
  val titleEn: String,
  val wilaya: String,
  val referencePriceLabel: String,
  val targetSection: WorkspaceSection,
  val suggestedRole: UserRole,
)

private val publicVisitorMarketDirectory =
  listOf(
    PublicMarketPreviewItem(
      id = "pub_deglet",
      titleAr = "تمور دقلة نور — واحات طولقة",
      titleFr = "Dattes Deglet Nour — Oasis de Tolga",
      titleEn = "Deglet Nour Dates — Tolga Oasis",
      wilaya = "07 - Biskra",
      referencePriceLabel = "390 - 450 DZD / kg",
      targetSection = WorkspaceSection.PUBLIC_MARKET,
      suggestedRole = UserRole.TRADER,
    ),
    PublicMarketPreviewItem(
      id = "pub_potato",
      titleAr = "بطاطا موسمية سبونتا — وادي سوف وعين الدفلى",
      titleFr = "Pomme de terre Spunta — El Oued & Aïn Defla",
      titleEn = "Spunta Seasonal Potatoes — El Oued & Ain Defla",
      wilaya = "39 - El Oued",
      referencePriceLabel = "68 - 82 DZD / kg",
      targetSection = WorkspaceSection.PUBLIC_MARKET,
      suggestedRole = UserRole.TRADER,
    ),
    PublicMarketPreviewItem(
      id = "pub_olive",
      titleAr = "زيتون السيقوا والشملال للعصر والتصبير",
      titleFr = "Olives Sigoise & Chemlal",
      titleEn = "Sigoise & Chemlal Olives",
      wilaya = "29 - Mascara",
      referencePriceLabel = "175 - 210 DZD / kg",
      targetSection = WorkspaceSection.PUBLIC_MARKET,
      suggestedRole = UserRole.TRADER,
    ),
    PublicMarketPreviewItem(
      id = "pub_cold_storage",
      titleAr = "غرف تبريد موجبة وسالبة (+4°C / -18°C) مع الفرز والتوضيب",
      titleFr = "Chambres froides positives/négatives & conditionnement",
      titleEn = "Positive/Negative Cold Storage & Post-Harvest Grading",
      wilaya = "09 - Blida / 07 - Biskra",
      referencePriceLabel = "4,500 DZD / Tonne / Mois",
      targetSection = WorkspaceSection.SERVICES,
      suggestedRole = UserRole.FARMER,
    ),
    PublicMarketPreviewItem(
      id = "pub_frigo_transport",
      titleAr = "خطوط شاحنات مبردة ومسّطحة بين الجنوب وأسواق الجملة",
      titleFr = "Lignes de transport frigorifique Sud → Marchés de gros",
      titleEn = "Refrigerated Freight Corridors South → Wholesale Hubs",
      wilaya = "National (58 Wilayas)",
      referencePriceLabel = "Devis immédiat / طن",
      targetSection = WorkspaceSection.TRANSPORT,
      suggestedRole = UserRole.TRANSPORTER,
    ),
  )

/**
 * Unauthenticated Visitor Mode (`VisitorExploreScreen`).
 * Allows visitors to browse public agricultural product categories, the 14 agricultural services,
 * and the 6 professional platform roles without querying protected Firestore collections.
 * Any protected action (buy, publish, book, quote, message) opens a clean sign-in prompt that
 * preserves the visitor's intended destination section and pre-selected role.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VisitorExploreScreen(
  language: AppLanguage,
  onLanguageSelected: (AppLanguage) -> Unit,
  onBackToLogin: () -> Unit,
  onRequireSignInForAction: (WorkspaceSection, UserRole?) -> Unit,
  customAiViewModel: FellahAiViewModel? = null,
  customMatinViewModel: FellahMatinViewModel? = null,
) {
  BackHandler(onBack = onBackToLogin)

  val layoutDirection =
    if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr
  var activeTab by rememberSaveable { mutableIntStateOf(0) }
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var pendingGatePrompt by remember { mutableStateOf<Pair<WorkspaceSection, UserRole?>?>(null) }
  var showVisitorAiSheet by rememberSaveable { mutableStateOf(false) }

  val visitorAiViewModel: FellahAiViewModel =
    customAiViewModel
      ?: viewModel(
        key = "visitor_fellah_ai",
        factory = FellahAiViewModel.provideFactory(authenticatedUserId = null),
      )
  val visitorMatinViewModel: FellahMatinViewModel =
    customMatinViewModel
      ?: viewModel(
        key = "visitor_fellah_matin",
        factory = FellahMatinViewModel.provideFactory(authenticatedUserId = null),
      )
  val visitorSnapshot = remember { FellahPlatformDataSnapshot(isAuthenticated = false) }

  CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
    Scaffold(
      modifier = Modifier.fillMaxSize().testTag("visitor_explore_root"),
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      floatingActionButton = {
        ExtendedFloatingActionButton(
          onClick = { showVisitorAiSheet = true },
          icon = {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
            )
          },
          text = {
            Text(
              text = RoleStrings.tr(language, "اسأل Fellah AI", "Demander à Fellah AI", "Ask Fellah AI"),
              fontWeight = FontWeight.Bold,
            )
          },
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          modifier = Modifier.testTag("visitor_open_fellah_ai_button"),
        )
      },
      topBar = {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shadowElevation = 2.dp,
          modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f),
            ) {
              IconButton(
                onClick = onBackToLogin,
                modifier = Modifier.size(48.dp).testTag("visitor_back_to_login_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription =
                    RoleStrings.tr(language, "العودة لتسجيل الدخول", "Retour", "Back"),
                )
              }
              Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFEFEFE),
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
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = RoleStrings.visitorBannerTitle(language),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
              ) {
                Row(modifier = Modifier.padding(2.dp)) {
                  AppLanguage.entries.forEach { lang ->
                    val selected = lang == language
                    Box(
                      modifier =
                        Modifier.clip(RoundedCornerShape(6.dp))
                          .background(
                            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                          )
                          .clickable { onLanguageSelected(lang) }
                          .padding(horizontal = 8.dp, vertical = 5.dp)
                          .testTag("visitor_lang_${lang.code}"),
                    ) {
                      Text(
                        text = lang.code,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color =
                          if (selected) MaterialTheme.colorScheme.onPrimary
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
              }

              Button(
                onClick = { onRequireSignInForAction(WorkspaceSection.ROLE_DASHBOARD, null) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(44.dp).testTag("visitor_top_sign_in_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Login,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = RoleStrings.tr(language, "دخول", "Connexion", "Sign In"),
                  style = MaterialTheme.typography.labelLarge,
                )
              }
            }
          }
        }
      },
    ) { innerPadding ->
      Column(
        modifier =
          Modifier.fillMaxSize()
            .padding(innerPadding)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Visitor Hero Notice
        Card(
          shape = RoundedCornerShape(18.dp),
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
            ),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.LockOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = RoleStrings.visitorBannerTitle(language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
            Text(
              text =
                RoleStrings.tr(
                  language,
                  ar = "استكشف شعب الإنتاج، مؤشرات السوق المرجعية، والخدمات الزراعية الـ 14 والأنشطة المهنية الستة بحرية. عند رغبتك في الشراء أو النشر أو الحجز، سجل دخولك للمتابعة فورًا.",
                  fr = "Parcourez librement les filières agricoles, les 14 services techniques et les 6 rôles professionnels. Connectez-vous uniquement lorsque vous souhaitez publier, acheter ou réserver.",
                  en = "Freely browse agricultural sectors, all 14 field services, and the 6 professional roles. Sign in only when you are ready to buy, publish, quote, or book.",
                ),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }

        // Visible Fellah Matin Morning News Section on Visitor Home (when not in full News tab)
        if (activeTab != 3) {
          FellahMatinDashboardSection(
            matinViewModel = visitorMatinViewModel,
            language = language,
            activeRole = null,
            onViewAllNews = { activeTab = 3 },
          )
        }

        // Search + Section Tabs
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          singleLine = true,
          placeholder = {
            Text(
              RoleStrings.tr(
                language,
                ar = "ابحث في المنتجات أو الخدمات أو الأنشطة المهنية...",
                fr = "Rechercher produits, services ou rôles professionnels…",
                en = "Search products, agricultural services, or roles…",
              )
            )
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("visitor_search_input"),
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          val tabs =
            listOf(
              0 to RoleStrings.tr(language, "السوق والشعب الفلاحية", "Marché & Filières", "Marketplace"),
              1 to RoleStrings.tr(language, "الخدمات الفلاحية (14)", "Services Agricoles (14)", "Agri Services (14)"),
              2 to RoleStrings.tr(language, "الأنشطة المهنية (6)", "Activités & Rôles (6)", "Platform Roles (6)"),
              3 to FellahMatinStrings.sectionHeading(language),
            )
          tabs.forEach { (idx, label) ->
            FilterChip(
              selected = activeTab == idx,
              onClick = { activeTab = idx },
              label = { Text(label, fontWeight = FontWeight.SemiBold) },
              modifier = Modifier.testTag("visitor_tab_$idx"),
            )
          }
        }

        when (activeTab) {
          0 -> {
            // Public Marketplace & Reference Crops
            val items =
              publicVisitorMarketDirectory.filter { item ->
                searchQuery.isBlank() ||
                  item.titleAr.contains(searchQuery, ignoreCase = true) ||
                  item.titleFr.contains(searchQuery, ignoreCase = true) ||
                  item.titleEn.contains(searchQuery, ignoreCase = true) ||
                  item.wilaya.contains(searchQuery, ignoreCase = true)
              }

            items.forEach { item ->
              val itemTitle =
                RoleStrings.tr(language, item.titleAr, item.titleFr, item.titleEn)
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
                    .testTag("visitor_market_card_${item.id}"),
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
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                      )
                      Column {
                        Text(
                          text = itemTitle,
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                        )
                        Text(
                          text = item.wilaya,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                      Text(
                        text = item.referencePriceLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                      )
                    }
                  }

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Button(
                      onClick = {
                        pendingGatePrompt = item.targetSection to item.suggestedRole
                      },
                      shape = RoundedCornerShape(10.dp),
                      modifier =
                        Modifier.height(48.dp)
                          .weight(1f)
                          .testTag("visitor_order_action_${item.id}"),
                    ) {
                      Icon(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        RoleStrings.tr(
                          language,
                          ar = "طلب شراء / عرض سعر",
                          fr = "Commander / Demander devis",
                          en = "Order / Request Quote",
                        )
                      )
                    }
                    OutlinedButton(
                      onClick = {
                        pendingGatePrompt = WorkspaceSection.PUBLIC_MARKET to UserRole.FARMER
                      },
                      shape = RoundedCornerShape(10.dp),
                      modifier =
                        Modifier.height(48.dp)
                          .testTag("visitor_publish_action_${item.id}"),
                    ) {
                      Text(RoleStrings.tr(language, "نشر عرض مماثل", "Publier", "Publish"))
                    }
                  }
                }
              }
            }

            // Crop Sector Quick Reference
            Text(
              text =
                RoleStrings.tr(
                  language,
                  ar = "الشعب الفلاحية المرجعية المعتمدة",
                  fr = "Filières Agricoles de Référence",
                  en = "Certified Reference Crop Sectors",
                ),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              CropVarietyType.entries.forEach { crop ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.surfaceContainerLow,
                  modifier =
                    Modifier.border(
                      1.dp,
                      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                      RoundedCornerShape(10.dp),
                    ),
                ) {
                  Text(
                    text = "${crop.titleAr} • ${crop.subtitleFr}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  )
                }
              }
            }
          }

          1 -> {
            // Public 14 Agricultural Services Directory
            AgriServiceDomain.entries.forEach { domain ->
              val domainServices =
                AgriServiceCategory.byDomain(domain).filter { svc ->
                  searchQuery.isBlank() ||
                    svc.titleAr.contains(searchQuery, ignoreCase = true) ||
                    svc.subtitleFr.contains(searchQuery, ignoreCase = true) ||
                    svc.descriptionAr.contains(searchQuery, ignoreCase = true)
                }
              if (domainServices.isNotEmpty()) {
                Text(
                  text = "${domain.titleAr} • ${domain.subtitleFr}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.primary,
                )
                domainServices.forEach { svc ->
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
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(14.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Column(
                        modifier = Modifier.weight(1f).padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                      ) {
                        Text(
                          text = "${svc.titleAr} (${svc.subtitleFr})",
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.Bold,
                        )
                        Text(
                          text = svc.descriptionAr,
                          style = MaterialTheme.typography.bodySmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                      OutlinedButton(
                        onClick = {
                          pendingGatePrompt = WorkspaceSection.SERVICES to UserRole.FARMER
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier =
                          Modifier.height(48.dp)
                            .testTag("visitor_service_book_${svc.code}"),
                      ) {
                        Icon(
                          imageVector = Icons.Filled.Handyman,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(svc.actionLabelAr)
                      }
                    }
                  }
                }
              }
            }
          }

          2 -> {
            // Public 6 Professional Roles Showcase
            UserRole.entries.forEach { role ->
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
                    .testTag("visitor_role_card_${role.code}"),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(16.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = MaterialTheme.colorScheme.primaryContainer,
                      modifier = Modifier.size(48.dp),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = RoleStrings.iconForRole(role),
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                      }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Text(
                        text = role.localizedTitle(language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text = role.localizedDescription(language),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  Button(
                    onClick = {
                      pendingGatePrompt = WorkspaceSection.ROLE_DASHBOARD to role
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier =
                      Modifier.height(48.dp)
                        .testTag("visitor_join_as_${role.code}"),
                  ) {
                    Text(RoleStrings.tr(language, "انضم بهذا النشاط", "Rejoindre", "Join"))
                  }
                }
              }
            }
          }

          3 -> {
            FellahMatinFullScreen(
              matinViewModel = visitorMatinViewModel,
              language = language,
              activeRole = null,
              isAuthenticated = false,
              verifiedMarketListings = emptyList(),
              verifiedWeatherReport = null,
              isLoadingWeather = false,
              weatherError = null,
              onRefreshWeather = null,
              onBack = { activeTab = 0 },
            )
          }
        }
      }
    }

    val gateTarget = pendingGatePrompt
    if (gateTarget != null) {
      AlertDialog(
        onDismissRequest = { pendingGatePrompt = null },
        title = {
          Text(
            text =
              RoleStrings.tr(
                language,
                ar = "تسجيل الدخول مطلوب للمتابعة",
                fr = "Connexion requise pour continuer",
                en = "Sign In Required to Continue",
              ),
            fontWeight = FontWeight.Bold,
          )
        },
        text = {
          Text(
            text = RoleStrings.visitorSignInGateMessage(language),
            style = MaterialTheme.typography.bodyMedium,
          )
        },
        confirmButton = {
          Button(
            onClick = {
              val (destSection, suggestedRole) = gateTarget
              pendingGatePrompt = null
              onRequireSignInForAction(destSection, suggestedRole)
            },
            modifier = Modifier.height(48.dp).testTag("visitor_gate_confirm_signin_button"),
          ) {
            Text(
              RoleStrings.tr(
                language,
                ar = "تسجيل الدخول والمتابعة",
                fr = "Se connecter et continuer",
                en = "Sign In & Continue",
              )
            )
          }
        },
        dismissButton = {
          TextButton(
            onClick = { pendingGatePrompt = null },
            modifier = Modifier.height(48.dp).testTag("visitor_gate_cancel_button"),
          ) {
            Text(RoleStrings.tr(language, "متابعة التصفح كزائر", "Rester visiteur", "Keep Browsing"))
          }
        },
      )
    }

    if (showVisitorAiSheet) {
      FellahAiModalSheet(
        aiViewModel = visitorAiViewModel,
        language = language,
        activeRole = UserRole.FARMER,
        platformSnapshot = visitorSnapshot,
        onDismiss = { showVisitorAiSheet = false },
      )
    }
  }
}
