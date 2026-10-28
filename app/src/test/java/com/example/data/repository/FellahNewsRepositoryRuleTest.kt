package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.base.FirestoreEmulatorTestBase
import com.example.data.news.FellahNewsCategory
import com.example.data.news.FellahNewsRssIngestionService
import com.example.data.news.NewsFeedPageRequest
import com.example.data.news.NewsModerationStatus
import com.example.data.news.NewsSourceType
import com.example.data.news.NewsVerificationStatus
import com.example.data.offline.FellahOfflineDatabase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FellahNewsRepositoryRuleTest : FirestoreEmulatorTestBase() {

  private lateinit var roomDb: FellahOfflineDatabase

  @Before
  override fun setUpFirebase() {
    super.setUpFirebase()
    val context = ApplicationProvider.getApplicationContext<Context>()
    roomDb =
      Room.inMemoryDatabaseBuilder(context, FellahOfflineDatabase::class.java)
        .allowMainThreadQueries()
        .build()
  }

  @After
  override fun tearDownFirebase() {
    roomDb.close()
    super.tearDownFirebase()
  }

  @Test
  fun editorialAdminCanPublishApproveRejectAndManageSources_whileRegularUserReadsApprovedOnly() =
    runBlocking {
      // 1. Sign in as primary editorial admin (`mehdi.hamdani.net@gmail.com`)
      signInTestUser("mehdi.hamdani.net@gmail.com")
      val adminRepo =
        FellahNewsRepository(
          db = firestore,
          auth = auth,
          cachedNewsDao = roomDb.cachedNewsArticleDao(),
          rssIngestionService = FellahNewsRssIngestionService(httpFetcher = { "" }),
        )
      assertTrue(adminRepo.checkIsEditorialAdmin())

      val approvedId =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo
            .submitOrImportArticle(
              title = "توسيع مساحات الحبوب المروية في الجنوب الكبير",
              summary = "برنامج وطني لدعم اقتناء المرشات المحورية والبذور المعتمدة لموسم الحرث والبذر.",
              sourceName = "وكالة الأنباء الجزائرية (APS)",
              originalUrl = "https://www.aps.dz/economie/cereales-sud-101",
              publishedAtIso = "2026-10-28T07:00:00Z",
              category = FellahNewsCategory.CROP_PRODUCTION,
              language = "AR",
              wilaya = "01 - أدرار (Adrar)",
              moderationStatus = NewsModerationStatus.APPROVED,
              verificationStatus = NewsVerificationStatus.EDITORIAL_VERIFIED,
            )
            .getOrThrow()
        }

      val pendingId =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo
            .submitOrImportArticle(
              title = "مسودة خبر حول غرف التبريد قيد التدقيق",
              summary = "مراجعة طاقات التخزين المبردة للبطاطا والتمور قبل النشر الرسمي.",
              sourceName = "FAO Newsroom",
              originalUrl = "https://www.fao.org/newsroom/detail/cold-chain-draft-202",
              publishedAtIso = "2026-10-28T08:00:00Z",
              category = FellahNewsCategory.COLD_STORAGE_POSTHARVEST,
              language = "AR",
              moderationStatus = NewsModerationStatus.PENDING_REVIEW,
            )
            .getOrThrow()
        }

      // Admin registers and then disables a news source
      val srcId =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo
            .upsertNewsSource(
              sourceId = "src_rule_test_1",
              name = "FAO Newsroom RSS",
              baseUrl = "https://www.fao.org",
              feedUrl = "https://www.fao.org/feeds/fao-newsroom-rss",
              sourceType = NewsSourceType.OFFICIAL_INSTITUTION,
              language = "EN",
              isEnabled = true,
            )
            .getOrThrow()
        }
      assertEquals("src_rule_test_1", srcId)

      // 2. Switch to non-admin regular operator
      signInTestUser("regular_farmer_matin@fellah.dz")
      val userRepo =
        FellahNewsRepository(
          db = firestore,
          auth = auth,
          cachedNewsDao = roomDb.cachedNewsArticleDao(),
          rssIngestionService = FellahNewsRssIngestionService(httpFetcher = { "" }),
        )
      assertFalse(userRepo.checkIsEditorialAdmin())

      // Regular user sees ONLY approved article, never pending_review
      val page =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          userRepo.queryPublishedNewsPage(NewsFeedPageRequest(page = 1, pageSize = 10))
        }
      assertTrue(page.items.any { it.id == approvedId })
      assertFalse(page.items.any { it.id == pendingId })

      // Regular user attempting to fetch pending_review article by ID fails
      val pendingGet =
        withTimeout(DEFAULT_TIMEOUT_MS) { userRepo.getPublishedArticleById(pendingId) }
      assertTrue(pendingGet.isFailure)

      // Regular user attempting to publish or moderate fails with PERMISSION_DENIED
      val unauthorizedPublish =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          userRepo.submitOrImportArticle(
            title = "خبر غير مصرح به",
            summary = "محاولة نشر مقال من حساب فلاح غير مشرف تحريري.",
            sourceName = "Fake Source",
            originalUrl = "https://www.aps.dz/economie/unauthorized",
            publishedAtIso = "2026-10-28T09:00:00Z",
            category = FellahNewsCategory.GOV_ANNOUNCEMENTS,
            language = "AR",
            moderationStatus = NewsModerationStatus.APPROVED,
          )
        }
      assertTrue(unauthorizedPublish.isFailure)
      val pubEx = unauthorizedPublish.exceptionOrNull()?.unwrapFirestoreException()
      assertNotNull(pubEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, pubEx?.code)

      val unauthorizedModerate =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          userRepo.moderateAndEditArticle(
            articleId = pendingId,
            moderationStatus = NewsModerationStatus.APPROVED,
          )
        }
      assertTrue(unauthorizedModerate.isFailure)

      // 3. Switch back to editorial admin, approve pendingId, and disable source
      signInTestUser("mehdi.hamdani.net@gmail.com")
      val approveRes =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.moderateAndEditArticle(
            articleId = pendingId,
            moderationStatus = NewsModerationStatus.APPROVED,
            updatedSummary = "ملخص مدقق رسمياً حول سعات التخزين المبردة للتمور والبطاطا.",
          )
        }
      assertTrue(approveRes.isSuccess)

      val disableSrcRes =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          adminRepo.setNewsSourceEnabled(
            sourceId = srcId,
            isEnabled = false,
            lastError = "Paused during maintenance check",
          )
        }
      assertTrue(disableSrcRes.isSuccess)
    }

  @Test
  fun userNewsPreferences_enforceOwnerIsolationAndCategoryValidation() = runBlocking {
    val aliceUid = signInTestUser("alice_news_prefs@fellah.dz")
    val aliceRepo =
      FellahNewsRepository(
        db = firestore,
        auth = auth,
        cachedNewsDao = roomDb.cachedNewsArticleDao(),
      )

    val saveRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.saveUserNewsPreferences(
          preferredCategories =
            listOf(
              FellahNewsCategory.WEATHER_ALERTS.code,
              FellahNewsCategory.CROP_PRODUCTION.code,
              "ignored_invalid_code",
            ),
          preferredRegion = "07 - بسكرة (Biskra)",
          preferredLanguage = "AR",
        )
      }
    assertTrue(saveRes.isSuccess)

    val observed =
      withTimeout(FLOW_TIMEOUT_MS) {
        aliceRepo.observeUserNewsPreferences(aliceUid).first { it != null }
      }
    assertNotNull(observed)
    assertEquals(2, observed?.preferredCategories?.size)
    assertEquals("07 - بسكرة (Biskra)", observed?.preferredRegion)

    // Cross-user read via direct Firestore call is denied by security rules
    signInTestUser("bob_news_prefs@fellah.dz")
    val crossReadEx =
      runCatching {
          withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("user_news_preferences").document(aliceUid).get().await()
          }
        }
        .exceptionOrNull()
        ?.unwrapFirestoreException()
    assertNotNull(crossReadEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, crossReadEx?.code)
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
