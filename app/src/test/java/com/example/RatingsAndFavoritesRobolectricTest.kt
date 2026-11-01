package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.example.data.model.AppLanguage
import com.example.data.model.FavoriteTargetType
import com.example.data.model.MarketListing
import com.example.data.model.PartnerCatalogItem
import com.example.data.model.UserFavoriteRecord
import com.example.data.model.UserRatingRecord
import com.example.data.model.UserReputationSummary
import com.example.data.model.UserRole
import com.example.ui.screens.PublicMarketScreen
import com.example.ui.screens.RoleDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RatingsAndFavoritesRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `userReputationSummary aggregates stars trustPointsOutOf100 and tiers accurately`() {
    val emptySummary = UserReputationSummary.fromRatings("uid_new", emptyList())
    assertFalse(emptySummary.hasRatings)
    assertEquals(0, emptySummary.trustPointsOutOf100)
    assertEquals("متعامل جديد", emptySummary.tierLabel(AppLanguage.AR))

    val ratings =
      listOf(
        UserRatingRecord(
          id = "r1_uid_farmer",
          reviewerId = "r1",
          reviewerName = "تاجر الواحة",
          reviewerRole = "trader",
          targetUserId = "uid_farmer",
          targetName = "تعاونية البركة",
          targetRole = "farmer",
          stars = 5,
          pointsAwarded = 100,
          comment = "جودة ممتازة والتزام بالوزن",
        ),
        UserRatingRecord(
          id = "r2_uid_farmer",
          reviewerId = "r2",
          reviewerName = "شركة النقل المبرد",
          reviewerRole = "transporter",
          targetUserId = "uid_farmer",
          targetName = "تعاونية البركة",
          targetRole = "farmer",
          stars = 4,
          pointsAwarded = 80,
          comment = "تحميل سريع ومنظم",
        ),
      )

    val map = UserReputationSummary.buildReputationMap(ratings)
    val summary = map.getValue("uid_farmer")
    assertTrue(summary.hasRatings)
    assertEquals(2, summary.totalRatings)
    assertEquals(4.5, summary.averageStars, 0.01)
    assertEquals(90, summary.trustPointsOutOf100)
    assertEquals(180, summary.totalPointsAccumulated)
    assertEquals("نخبة موثوقة", summary.tierLabel(AppLanguage.AR))
  }

  @Test
  fun `publicMarketScreen submits star rating toggles favorites and hides self-rating on own listing`() {
    val currentUid = "trader_me_01"
    val otherFarmerUid = "farmer_tolga_99"

    val listings =
      listOf(
        MarketListing(
          id = "listing_tolga_1",
          userId = otherFarmerUid,
          operatorName = "مزرعة النخيل الذهبي",
          cropType = "deglet_nour",
          wilaya = "07 - بسكرة (Biskra)",
          availableTons = 22.0,
          pricePerKgDzd = 430.0,
        ),
        MarketListing(
          id = "listing_mine_2",
          userId = currentUid,
          operatorName = "مزرعتي الخاصة",
          cropType = "olivier",
          wilaya = "29 - معسكر (Mascara)",
          availableTons = 12.0,
          pricePerKgDzd = 195.0,
        ),
      )

    var publishedRatings by
      mutableStateOf(
        listOf(
          UserRatingRecord(
            id = "other_reviewer_${otherFarmerUid}",
            reviewerId = "other_reviewer",
            reviewerName = "مؤسسة الأوراس",
            reviewerRole = "trader",
            targetUserId = otherFarmerUid,
            targetName = "مزرعة النخيل الذهبي",
            targetRole = "farmer",
            stars = 5,
            pointsAwarded = 100,
            comment = "تمور ممتازة",
          )
        )
      )
    var userFavorites by mutableStateOf<List<UserFavoriteRecord>>(emptyList())

    composeTestRule.setContent {
      MyApplicationTheme {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          PublicMarketScreen(
            listingsState = UiState.Success(listings),
            currentUserId = currentUid,
            searchQuery = "",
            onOpenPublishModal = {},
            onMarkSold = {},
            onBackToDelineation = {},
            partnerCatalogState =
              UiState.Success(
                listOf(
                  PartnerCatalogItem(
                    id = "partner_frigo_1",
                    userId = "storage_owner_77",
                    providerRole = "storage_provider",
                    providerName = "غرف تبريد الزيبان",
                    title = "غرفة تبريد +4°C",
                    wilaya = "07 - بسكرة",
                    priceDzd = 4500.0,
                  )
                )
              ),
            activeRole = UserRole.TRADER,
            language = AppLanguage.AR,
            requesterDisplayName = "تاجر الجملة",
            reputationByUser = UserReputationSummary.buildReputationMap(publishedRatings),
            publishedRatings = publishedRatings,
            userFavorites = userFavorites,
            onSubmitRating = { targetUid, targetName, targetRole, stars, comment, ctxId ->
              val updated =
                publishedRatings.filterNot {
                  it.reviewerId == currentUid && it.targetUserId == targetUid
                } +
                  UserRatingRecord(
                    id = "${currentUid}_$targetUid",
                    reviewerId = currentUid,
                    reviewerName = "تاجر الجملة",
                    reviewerRole = "trader",
                    targetUserId = targetUid,
                    targetName = targetName,
                    targetRole = targetRole,
                    stars = stars,
                    pointsAwarded = stars * 20,
                    comment = comment,
                    contextItemId = ctxId,
                  )
              publishedRatings = updated
            },
            onToggleFavorite = { favType, targetId, ownerId, title, subtitle, role ->
              val docId = UserFavoriteRecord.compositeDocumentId(currentUid, favType, targetId)
              userFavorites =
                if (userFavorites.any { it.id == docId }) {
                  userFavorites.filterNot { it.id == docId }
                } else {
                  userFavorites +
                    UserFavoriteRecord(
                      id = docId,
                      userId = currentUid,
                      favoriteType = favType.code,
                      targetId = targetId,
                      targetOwnerId = ownerId,
                      targetTitle = title,
                      targetSubtitle = subtitle,
                      targetRole = role,
                    )
                }
            },
            onRemoveFavorite = { favId ->
              userFavorites = userFavorites.filterNot { it.id == favId }
            },
          )
        }
      }
    }
    composeTestRule.waitForIdle()

    // 1. Verify reputation badge renders on farmer_tolga_99 (5.0 stars, 100/100 points)
    composeTestRule
      .onNodeWithTag("reputation_badge_$otherFarmerUid")
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule.onNodeWithText("100/100 نقطة • نخبة موثوقة").assertIsDisplayed()

    // 2. Verify anti-self-rating UI guard: own listing has NO rate_operator_btn_listing_mine_2
    composeTestRule.onAllNodesWithTag("rate_operator_btn_listing_mine_2").assertCountEquals(0)

    // 3. Open RateOperatorDialog for other farmer, select 4 stars (80 pts), submit comment
    composeTestRule
      .onNodeWithTag("rate_operator_btn_listing_tolga_1")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("rate_operator_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("rate_star_4").performClick()
    composeTestRule.waitForIdle()
    composeTestRule
      .onNodeWithText("80 / 100 نقطة", substring = true)
      .assertIsDisplayed()

    composeTestRule
      .onNodeWithTag("rate_comment_input")
      .performTextReplacement("معاملة احترافية في التمور")
    composeTestRule.onNodeWithTag("rate_submit_button").performClick()
    composeTestRule.waitForIdle()

    // Average of 5 and 4 stars -> 4.5 stars, 90/100 points
    assertEquals(2, publishedRatings.size)
    composeTestRule.onNodeWithText("4.5 (2)").assertIsDisplayed()
    composeTestRule.onNodeWithText("90/100 نقطة • نخبة موثوقة").assertIsDisplayed()

    // 4. Toggle Favorite on harvest listing AND operator, then inspect Favorites tab
    composeTestRule
      .onNodeWithTag("favorite_btn_market_listing_listing_tolga_1")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()

    composeTestRule
      .onNodeWithTag("favorite_operator_listing_listing_tolga_1")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()
    assertEquals(2, userFavorites.size)

    composeTestRule
      .onNodeWithTag("market_subtab_favorites")
      .performScrollTo()
      .performClick()
    composeTestRule.waitForIdle()

    val savedListingDocId =
      UserFavoriteRecord.compositeDocumentId(
        currentUid,
        FavoriteTargetType.MARKET_LISTING,
        "listing_tolga_1",
      )
    composeTestRule
      .onNodeWithTag("favorite_saved_card_$savedListingDocId")
      .performScrollTo()
      .assertIsDisplayed()

    // Remove one favorite from the Favorites subtab
    composeTestRule
      .onNodeWithTag("remove_favorite_$savedListingDocId")
      .performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, userFavorites.size)
  }

  @Test
  fun `roleDashboardScreen displays myReputationOverviewCard with stars points and favorites count`() {
    val myRep =
      UserReputationSummary(
        targetUserId = "farmer_me",
        averageStars = 4.8,
        totalRatings = 5,
        trustPointsOutOf100 = 96,
        totalPointsAccumulated = 480,
      )

    composeTestRule.setContent {
      MyApplicationTheme {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          RoleDashboardScreen(
            activeRole = UserRole.FARMER,
            assignedRoles = listOf(UserRole.FARMER),
            language = AppLanguage.AR,
            operatorDisplayName = "مستثمرة الهضاب",
            roleProfile = null,
            parcels = emptyList(),
            marketListings = emptyList(),
            partnerCatalog = emptyList(),
            myCatalogItems = emptyList(),
            myOrders = emptyList(),
            openRoleOrders = emptyList(),
            transportRequests = emptyList(),
            serviceBookings = emptyList(),
            messages = emptyList(),
            currentUserId = "farmer_me",
            onNavigateToSection = {},
            onSwitchRole = {},
            onOpenPublishHarvestDialog = {},
            myReputation = myRep,
            favoritesCount = 3,
          )
        }
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("my_reputation_overview_card").assertIsDisplayed()
    composeTestRule.onNodeWithText("96/100 نقطة • نخبة موثوقة").assertIsDisplayed()
    composeTestRule.onNodeWithText("المفضلة المحفوظة: 3").assertIsDisplayed()
  }
}
