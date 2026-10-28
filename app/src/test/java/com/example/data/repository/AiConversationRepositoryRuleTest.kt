package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.ai.AiErrorCode
import com.example.data.ai.FellahAiException
import com.example.data.model.AppLanguage
import com.example.data.model.UserRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiConversationRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createAppendObserveAndDeleteConversation_authenticatedOwner_succeeds() = runBlocking {
    val aliceUid = signInTestUser("alice_ai_rule@fellah.dz")
    val repo = AiConversationRepository(firestore, auth)

    val conv =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.createConversation("كيف أبيع محصولي؟", UserRole.FARMER, AppLanguage.AR).getOrThrow()
      }
    assertTrue(conv.id.startsWith("conv_"))

    val msg1Id =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo
          .appendMessage(
            conversationId = conv.id,
            sender = "user",
            content = "كيف أبيع محصولي؟",
            activeRole = UserRole.FARMER,
            language = AppLanguage.AR,
            updatedMessageCount = 1,
          )
          .getOrThrow()
      }
    assertTrue(msg1Id.isNotEmpty())

    val msg2Id =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo
          .appendMessage(
            conversationId = conv.id,
            sender = "assistant",
            content = "يمكنك نشر إعلان المحصول عبر ربط القطعة الفلاحية بالسوق العام.",
            activeRole = UserRole.FARMER,
            language = AppLanguage.AR,
            verifiedDataSummary = "1 إعلان نشط موثق في بسكرة (420 د.ج/كغ)",
            hasVerifiedPlatformData = true,
            promptTokensEstimate = 30,
            completionTokensEstimate = 52,
            updatedMessageCount = 2,
          )
          .getOrThrow()
      }
    assertTrue(msg2Id.isNotEmpty())

    val emittedMessages =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeConversationMessages(aliceUid, conv.id).first { it.size == 2 }
      }
    assertEquals(2, emittedMessages.size)
    assertTrue(emittedMessages.any { it.hasVerifiedPlatformData })

    // Owner deletes conversation + messages
    val delRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.deleteConversation(conv.id) }
    assertTrue(delRes.isSuccess)
  }

  @Test
  fun crossUserReadAndDeleteConversation_rejectedWithPermissionDenied() = runBlocking {
    signInTestUser("alice_ai_iso@fellah.dz")
    val aliceRepo = AiConversationRepository(firestore, auth)
    val aliceConv =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo
          .createConversation("أبحث عن ناقل لمحصولي.", UserRole.FARMER, AppLanguage.AR)
          .getOrThrow()
      }

    signInTestUser("bob_ai_intruder@fellah.dz")
    val bobRepo = AiConversationRepository(firestore, auth)

    val readResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getConversationById(aliceConv.id) }
    assertTrue(readResult.isFailure)
    val readEx = readResult.exceptionOrNull() as? FirebaseFirestoreException
    assertNotNull(readEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, readEx?.code)

    val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.deleteConversation(aliceConv.id) }
    assertTrue(deleteResult.isFailure)
    val delEx = deleteResult.exceptionOrNull() as? FirebaseFirestoreException
    assertNotNull(delEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, delEx?.code)
  }

  @Test
  fun unauthenticatedObserveConversations_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = AiConversationRepository(firestore, auth)

    val thrown =
      runCatching {
          withTimeout(FLOW_TIMEOUT_MS) { repo.observeConversations("some_uid").first() }
        }
        .exceptionOrNull()
    assertNotNull(thrown)
    val fsEx = thrown?.unwrapFirestoreException()
    assertNotNull("Expected wrapped FirebaseFirestoreException, got $thrown", fsEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
  }

  @Test
  fun dailyQuotaEnforcement_blocksWhenLimitReachedAndResetsOnNewDay() = runBlocking {
    val uniqueTag = java.util.UUID.randomUUID().toString().take(8)
    signInTestUser("quota_$uniqueTag@fellah.dz")
    val repo = AiConversationRepository(firestore, auth)

    val q1 =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo
          .checkAndConsumeDailyQuota(dayKey = "2026-10-28", maxDailyRequests = 2)
          .getOrThrow()
      }
    assertEquals(1, q1.dailyRequestCount)

    val q2 =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo
          .checkAndConsumeDailyQuota(dayKey = "2026-10-28", maxDailyRequests = 2)
          .getOrThrow()
      }
    assertEquals(2, q2.dailyRequestCount)

    val q3 =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.checkAndConsumeDailyQuota(dayKey = "2026-10-28", maxDailyRequests = 2)
      }
    assertTrue(q3.isFailure)
    val quotaEx = q3.exceptionOrNull() as? FellahAiException
    assertEquals(AiErrorCode.DAILY_QUOTA_EXCEEDED, quotaEx?.code)

    // Next UTC day resets count to 1
    val nextDay =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo
          .checkAndConsumeDailyQuota(dayKey = "2026-10-29", maxDailyRequests = 2)
          .getOrThrow()
      }
    assertEquals(1, nextDay.dailyRequestCount)
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
