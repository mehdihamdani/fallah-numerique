package com.example.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppLanguage
import com.example.data.model.FavoriteTargetType
import com.example.data.model.UserFavoriteRecord
import com.example.data.model.UserRatingRecord
import com.example.data.model.UserReputationSummary
import com.example.data.model.UserRole
import com.example.util.RoleStrings

private val AmberStarColor = Color(0xFFD98200)
private val HeartRedColor = Color(0xFFD32F2F)

/**
 * Displays an operator's aggregated star average (★ 4.8), reputation trust points (96/100 نقطة),
 * review count, and trust tier pill (`نخبة موثوقة` / `موثوق جداً` / `متعامل جديد`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OperatorReputationBadge(
  summary: UserReputationSummary,
  language: AppLanguage,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  val safeTagId = UserFavoriteRecord.sanitizeKeySegment(summary.targetUserId.ifBlank { "unknown" })
  val baseModifier =
    modifier
      .testTag("reputation_badge_$safeTagId")
      .let { mod -> if (onClick != null) mod.clickable(onClick = onClick) else mod }

  FlowRow(
    modifier = baseModifier,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    // Stars & Review Count Pill
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Star,
          contentDescription = null,
          tint = AmberStarColor,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text =
            if (summary.hasRatings) {
              "${summary.formattedAverageStars()} (${summary.totalRatings})"
            } else {
              RoleStrings.tr(language, "بدون تقييمات بعد", "Aucun avis", "No reviews yet")
            },
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }

    // Reputation Trust Points Pill (0..100) + Tier Label
    Surface(
      shape = RoundedCornerShape(8.dp),
      color =
        if (summary.trustPointsOutOf100 >= 80) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.WorkspacePremium,
          contentDescription = null,
          tint =
            if (summary.trustPointsOutOf100 >= 80) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(15.dp),
        )
        Text(
          text =
            if (summary.hasRatings) {
              val ptsUnit = RoleStrings.tr(language, "نقطة", "pts", "pts")
              "${summary.trustPointsOutOf100}/100 $ptsUnit • ${summary.tierLabel(language)}"
            } else {
              summary.tierLabel(language)
            },
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color =
            if (summary.trustPointsOutOf100 >= 80) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

/**
 * Accessible 48.dp x 48.dp favorite toggle button supporting Operators, Market Harvest Listings,
 * and Partner Catalog Offers.
 */
@Composable
fun FavoriteToggleIconButton(
  isFavorited: Boolean,
  favoriteType: FavoriteTargetType,
  targetId: String,
  language: AppLanguage,
  onToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val safeTarget = UserFavoriteRecord.sanitizeKeySegment(targetId.ifBlank { "item" })
  val contentDesc =
    if (isFavorited) {
      RoleStrings.tr(language, "إزالة من المفضلة", "Retirer des favoris", "Remove from favorites")
    } else {
      RoleStrings.tr(language, "إضافة إلى المفضلة", "Ajouter aux favoris", "Add to favorites")
    }

  IconButton(
    onClick = onToggle,
    modifier =
      modifier
        .size(48.dp)
        .testTag("favorite_btn_${favoriteType.code}_$safeTarget"),
  ) {
    Icon(
      imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
      contentDescription = contentDesc,
      tint = if (isFavorited) HeartRedColor else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(22.dp),
    )
  }
}

/**
 * Compact 48.dp height action button for rating an operator (`★ تقييم المتعامل`).
 */
@Composable
fun RateOperatorActionButton(
  targetIdForTag: String,
  hasExistingRatingByMe: Boolean,
  language: AppLanguage,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val safeTag = UserFavoriteRecord.sanitizeKeySegment(targetIdForTag.ifBlank { "op" })
  OutlinedButton(
    onClick = onClick,
    shape = RoundedCornerShape(10.dp),
    modifier = modifier.height(48.dp).testTag("rate_operator_btn_$safeTag"),
  ) {
    Icon(
      imageVector = Icons.Filled.Star,
      contentDescription = null,
      tint = AmberStarColor,
      modifier = Modifier.size(18.dp),
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text =
        if (hasExistingRatingByMe) {
          RoleStrings.tr(language, "تعديل تقييمي", "Modifier ma note", "Update Rating")
        } else {
          RoleStrings.tr(language, "تقييم المتعامل", "Noter l'opérateur", "Rate Operator")
        },
      style = MaterialTheme.typography.labelLarge,
    )
  }
}

/**
 * Interactive 1–5 star & reputation points rating dialog (`20..100 نقطة`) with optional comment
 * and recent community reviews list.
 */
@Composable
fun RateOperatorDialog(
  targetUserId: String,
  targetName: String,
  targetRole: UserRole,
  language: AppLanguage,
  reputationSummary: UserReputationSummary,
  existingMyRating: UserRatingRecord?,
  onDismiss: () -> Unit,
  onSubmitRating: (stars: Int, comment: String) -> Unit,
) {
  var selectedStars by remember(existingMyRating?.stars) {
    mutableIntStateOf((existingMyRating?.stars ?: 5).coerceIn(1, 5))
  }
  var commentInput by remember(existingMyRating?.comment) {
    mutableStateOf(existingMyRating?.comment.orEmpty())
  }
  val computedPoints = selectedStars * 20

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("rate_operator_dialog"),
    title = {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text =
            RoleStrings.tr(
              language,
              ar = "تنقيط وتقييم المتعامل بالنجوم",
              fr = "Noter et évaluer l'opérateur",
              en = "Rate & Score Operator",
            ),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.ExtraBold,
        )
        Text(
          text = "$targetName • ${targetRole.localizedTitle(language)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold,
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OperatorReputationBadge(summary = reputationSummary, language = language)

        // Interactive 1..5 Star Selector (each 48dp touch target)
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              (1..5).forEach { starIndex ->
                val isFilled = starIndex <= selectedStars
                IconButton(
                  onClick = { selectedStars = starIndex },
                  modifier = Modifier.size(48.dp).testTag("rate_star_$starIndex"),
                ) {
                  Icon(
                    imageVector =
                      if (isFilled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription =
                      RoleStrings.tr(
                        language,
                        ar = "$starIndex نجوم",
                        fr = "$starIndex étoiles",
                        en = "$starIndex stars",
                      ),
                    tint =
                      if (isFilled) AmberStarColor
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                  )
                }
              }
            }

            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.primaryContainer,
            ) {
              Text(
                text =
                  RoleStrings.tr(
                    language,
                    ar = "★ $selectedStars / 5 نجوم  •  التنقيط الممنوح: $computedPoints / 100 نقطة",
                    fr = "★ $selectedStars / 5 étoiles  •  Score : $computedPoints / 100 pts",
                    en = "★ $selectedStars / 5 Stars  •  Score: $computedPoints / 100 pts",
                  ),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier =
                  Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("rate_live_points_preview"),
              )
            }
          }
        }

        OutlinedTextField(
          value = commentInput,
          onValueChange = { if (it.length <= 300) commentInput = it },
          label = {
            Text(
              RoleStrings.tr(
                language,
                ar = "ملاحظة حول جودة التعامل والالتزام (اختياري)",
                fr = "Avis sur la qualité et le sérieux (optionnel)",
                en = "Constructive review note (optional)",
              )
            )
          },
          supportingText = { Text("${commentInput.length} / 300") },
          modifier = Modifier.fillMaxWidth().testTag("rate_comment_input"),
        )

        if (reputationSummary.recentReviews.isNotEmpty()) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
          Text(
            text =
              RoleStrings.tr(
                language,
                ar = "آراء وتقييمات المتعاملين الأخيرة (${reputationSummary.totalRatings})",
                fr = "Derniers avis vérifiés (${reputationSummary.totalRatings})",
                en = "Recent Verified Reviews (${reputationSummary.totalRatings})",
              ),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          reputationSummary.recentReviews.take(4).forEach { review ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceContainerLowest,
              modifier =
                Modifier.fillMaxWidth()
                  .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(10.dp),
                  ),
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text =
                      "${review.reviewerName} (${UserRole.fromCode(review.reviewerRole).localizedTitle(language)})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "★ ${review.stars} • ${review.pointsAwarded} pts",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberStarColor,
                    fontWeight = FontWeight.Bold,
                  )
                }
                if (review.comment.isNotBlank()) {
                  Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onSubmitRating(selectedStars, commentInput.trim()) },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.height(48.dp).testTag("rate_submit_button"),
      ) {
        Text(
          RoleStrings.tr(
            language,
            ar = "اعتماد التقييم والتنقيط",
            fr = "Enregistrer la note",
            en = "Submit Rating",
          ),
          fontWeight = FontWeight.Bold,
        )
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.height(48.dp).testTag("rate_cancel_button"),
      ) {
        Text(RoleStrings.tr(language, "إلغاء", "Annuler", "Cancel"))
      }
    },
  )
}

/**
 * Overview card for the current operator's own reputation score, stars, accumulated points, and
 * saved favorites shortcut (displayed on `RoleDashboardScreen` & `OperatorSettingsScreen`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyReputationOverviewCard(
  myReputation: UserReputationSummary,
  favoritesCount: Int,
  language: AppLanguage,
  onOpenFavoritesInMarket: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier =
      modifier
        .fillMaxWidth()
        .border(
          1.dp,
          MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
          RoundedCornerShape(18.dp),
        )
        .testTag("my_reputation_overview_card"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
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
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.size(44.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = AmberStarColor,
                modifier = Modifier.size(24.dp),
              )
            }
          }
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text =
                RoleStrings.tr(
                  language,
                  ar = "رصيد السمعة المهنية والنجوم والمفضلة",
                  fr = "Réputation Professionnelle, Étoiles & Favoris",
                  en = "Reputation Score, Stars & Saved Favorites",
                ),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
            )
            Text(
              text =
                if (myReputation.hasRatings) {
                  RoleStrings.tr(
                    language,
                    ar = "معدل ★ ${myReputation.formattedAverageStars()} من ${myReputation.totalRatings} متعاملين • مجموع النقاط المكتسبة: ${myReputation.totalPointsAccumulated} نقطة",
                    fr = "Moyenne ★ ${myReputation.formattedAverageStars()} (${myReputation.totalRatings} avis) • Cumul : ${myReputation.totalPointsAccumulated} pts",
                    en = "Avg ★ ${myReputation.formattedAverageStars()} (${myReputation.totalRatings} reviews) • Total: ${myReputation.totalPointsAccumulated} pts",
                  )
                } else {
                  RoleStrings.tr(
                    language,
                    ar = "أنجز معاملاتك في السوق والطلبات لحصد النجوم ونقاط الثقة الموثقة",
                    fr = "Complétez vos transactions pour obtenir des étoiles et points de confiance",
                    en = "Complete marketplace deals to earn verified stars and trust points",
                  )
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OperatorReputationBadge(summary = myReputation, language = language)

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
          modifier =
            Modifier.let { mod ->
              if (onOpenFavoritesInMarket != null) {
                mod.clickable(onClick = onOpenFavoritesInMarket)
              } else {
                mod
              }
            }.testTag("my_favorites_count_pill"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Favorite,
              contentDescription = null,
              tint = HeartRedColor,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text =
                RoleStrings.tr(
                  language,
                  ar = "المفضلة المحفوظة: $favoritesCount",
                  fr = "Favoris : $favoritesCount",
                  en = "Favorites: $favoritesCount",
                ),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
      }

      if (myReputation.recentReviews.isNotEmpty()) {
        val latest = myReputation.recentReviews.first()
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
          ) {
            Text(
              text =
                RoleStrings.tr(
                  language,
                  ar = "آخر تقييم مستلم: ★ ${latest.stars}/5 (${latest.pointsAwarded} نقطة) من ${latest.reviewerName}",
                  fr = "Dernier avis reçu : ★ ${latest.stars}/5 (${latest.pointsAwarded} pts) par ${latest.reviewerName}",
                  en = "Latest review: ★ ${latest.stars}/5 (${latest.pointsAwarded} pts) from ${latest.reviewerName}",
                ),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
            )
            if (latest.comment.isNotBlank()) {
              Text(
                text = "«${latest.comment}»",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}
