package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.ai.AiChatMessageRecord
import com.example.data.ai.AiConversation
import com.example.data.ai.AiErrorCode
import com.example.data.ai.AiUsageQuota
import com.example.data.ai.FellahAiException
import com.example.data.handleFirestoreError
import com.example.data.model.AppLanguage
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Firestore repository implementing conversation persistence, ownership isolation,
 * deletion, and daily usage quota tracking for Fellah AI:
 * - `GET /api/v1/ai/conversations` -> [observeConversations] / [listConversations]
 * - `GET /api/v1/ai/conversations/{id}` -> [getConversationById] / [getConversationMessages]
 * - `DELETE /api/v1/ai/conversations/{id}` -> [deleteConversation]
 * - Daily usage quota enforcement -> [checkAndConsumeDailyQuota]
 */
class AiConversationRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(
    context: Context
  ) : this(
    db =
      FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
      ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw FellahAiException(
        code = AiErrorCode.UNAUTHORIZED_ACCESS,
        message = "يجب تسجيل الدخول لحفظ المحادثات أو استرجاعها.",
      )
  }

  fun observeConversations(userId: String): Flow<List<AiConversation>> {
    val query = db.collection(CONVERSATIONS_COLLECTION).whereEqualTo("userId", userId)
    return query
      .snapshots()
      .map { snap ->
        snap.documents
          .mapNotNull { doc ->
            doc.toObject(
              AiConversation::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          }
          .sortedByDescending { it.updatedAt?.seconds ?: 0L }
      }
      .catch { error ->
        if (error is Exception) {
          handleFirestoreError(error, OperationType.LIST, CONVERSATIONS_COLLECTION)
        }
        throw error
      }
  }

  suspend fun listConversations(): Result<List<AiConversation>> {
    val uid =
      auth.currentUser?.uid
        ?: return Result.failure(
          FellahAiException(AiErrorCode.UNAUTHORIZED_ACCESS, "Authentication required.")
        )
    return try {
      val snap =
        db.collection(CONVERSATIONS_COLLECTION).whereEqualTo("userId", uid).get().await()
      val items =
        snap.documents
          .mapNotNull {
            it.toObject(
              AiConversation::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          }
          .sortedByDescending { it.updatedAt?.seconds ?: 0L }
      Result.success(items)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, CONVERSATIONS_COLLECTION)
      Result.failure(e)
    }
  }

  suspend fun getConversationById(conversationId: String): Result<AiConversation> {
    val docRef = db.collection(CONVERSATIONS_COLLECTION).document(conversationId)
    return try {
      val snapshot = docRef.get().await()
      val conv =
        snapshot.toObject(
          AiConversation::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ) ?: throw NoSuchElementException("Conversation not found: $conversationId")
      Result.success(conv)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  fun observeConversationMessages(
    userId: String,
    conversationId: String,
  ): Flow<List<AiChatMessageRecord>> {
    val query =
      db.collection(MESSAGES_COLLECTION)
        .whereEqualTo("userId", userId)
        .whereEqualTo("conversationId", conversationId)
    return query
      .snapshots()
      .map { snap ->
        snap.documents
          .mapNotNull { doc ->
            doc.toObject(
              AiChatMessageRecord::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          }
          .sortedWith(
            compareBy<AiChatMessageRecord> { it.createdAt?.seconds ?: 0L }
              .thenBy { it.createdAt?.nanoseconds ?: 0 }
              .thenBy { if (it.sender == "user") 0 else 1 }
          )
      }
      .catch { error ->
        if (error is Exception) {
          handleFirestoreError(error, OperationType.LIST, MESSAGES_COLLECTION)
        }
        throw error
      }
  }

  suspend fun getConversationMessages(conversationId: String): Result<List<AiChatMessageRecord>> {
    val uid =
      auth.currentUser?.uid
        ?: return Result.failure(
          FellahAiException(AiErrorCode.UNAUTHORIZED_ACCESS, "Authentication required.")
        )
    return try {
      val snap =
        db.collection(MESSAGES_COLLECTION)
          .whereEqualTo("userId", uid)
          .whereEqualTo("conversationId", conversationId)
          .get()
          .await()
      val list =
        snap.documents
          .mapNotNull {
            it.toObject(
              AiChatMessageRecord::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          }
          .sortedWith(
            compareBy<AiChatMessageRecord> { it.createdAt?.seconds ?: 0L }
              .thenBy { it.createdAt?.nanoseconds ?: 0 }
              .thenBy { if (it.sender == "user") 0 else 1 }
          )
      Result.success(list)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, MESSAGES_COLLECTION)
      Result.failure(e)
    }
  }

  suspend fun createConversation(
    title: String,
    activeRole: UserRole,
    language: AppLanguage,
  ): Result<AiConversation> {
    return try {
      val uid = requireUserId()
      val convId = "conv_${UUID.randomUUID().toString().replace("-", "").take(20)}"
      val safeTitle = title.trim().ifBlank { "محادثة Fellah AI" }.take(120)
      val docRef = db.collection(CONVERSATIONS_COLLECTION).document(convId)
      val payload =
        mapOf(
          "userId" to uid,
          "title" to safeTitle,
          "activeRole" to activeRole.code,
          "language" to language.code,
          "messageCount" to 0,
          "lastMessagePreview" to "",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(
        AiConversation(
          id = convId,
          userId = uid,
          title = safeTitle,
          activeRole = activeRole.code,
          language = language.code,
          messageCount = 0,
          lastMessagePreview = "",
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, CONVERSATIONS_COLLECTION)
      Result.failure(e)
    }
  }

  suspend fun appendMessage(
    conversationId: String,
    sender: String,
    content: String,
    activeRole: UserRole,
    language: AppLanguage,
    verifiedDataSummary: String = "",
    hasVerifiedPlatformData: Boolean = false,
    promptTokensEstimate: Int = 0,
    completionTokensEstimate: Int = 0,
    updatedMessageCount: Int,
  ): Result<String> {
    val msgId = "aimsg_${UUID.randomUUID().toString().replace("-", "").take(20)}"
    val msgRef = db.collection(MESSAGES_COLLECTION).document(msgId)
    val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversationId)
    return try {
      val uid = requireUserId()
      val safeSender = if (sender == "assistant") "assistant" else "user"
      val safeContent = content.trim().ifBlank { "..." }.take(4000)
      val safeSummary = verifiedDataSummary.trim().take(1500)
      val msgPayload =
        mapOf(
          "userId" to uid,
          "conversationId" to conversationId,
          "sender" to safeSender,
          "content" to safeContent,
          "activeRole" to activeRole.code,
          "verifiedDataSummary" to safeSummary,
          "hasVerifiedPlatformData" to hasVerifiedPlatformData,
          "promptTokensEstimate" to promptTokensEstimate.coerceIn(0, 20000),
          "completionTokensEstimate" to completionTokensEstimate.coerceIn(0, 20000),
          "createdAt" to FieldValue.serverTimestamp(),
        )
      msgRef.set(msgPayload).await()

      val convUpdate =
        mapOf(
          "activeRole" to activeRole.code,
          "language" to language.code,
          "messageCount" to updatedMessageCount.coerceIn(0, 500),
          "lastMessagePreview" to safeContent.take(240),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      convRef.update(convUpdate).await()
      Result.success(msgId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, msgRef.path)
      Result.failure(e)
    }
  }

  /**
   * Deletes an owned conversation and all of its immutable message records (`DELETE /api/v1/ai/conversations/{id}`).
   */
  suspend fun deleteConversation(conversationId: String): Result<Unit> {
    val convRef = db.collection(CONVERSATIONS_COLLECTION).document(conversationId)
    return try {
      val uid = requireUserId()
      val msgSnap =
        db.collection(MESSAGES_COLLECTION)
          .whereEqualTo("userId", uid)
          .whereEqualTo("conversationId", conversationId)
          .get()
          .await()
      for (doc in msgSnap.documents) {
        doc.reference.delete().await()
      }
      convRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, convRef.path)
      Result.failure(e)
    }
  }

  /**
   * Enforces per-user daily usage quota and token budget in `/ai_usage_quotas/{userId}`.
   */
  suspend fun checkAndConsumeDailyQuota(
    dayKey: String,
    maxDailyRequests: Int = DEFAULT_DAILY_REQUEST_LIMIT,
    maxDailyTokens: Int = DEFAULT_DAILY_TOKEN_LIMIT,
    estimatedTokensDelta: Int = 250,
  ): Result<AiUsageQuota> {
    return try {
      val uid = requireUserId()
      val safeMaxReq = maxDailyRequests.coerceIn(1, 100)
      val safeMaxTokens = maxDailyTokens.coerceIn(500, 300000)
      val docRef = db.collection(QUOTAS_COLLECTION).document(uid)
      val snap = docRef.get().await()

      if (!snap.exists()) {
        val nextCount = 1
        val nextTokens = estimatedTokensDelta.coerceIn(1, safeMaxTokens)
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "dayKey" to dayKey,
              "dailyRequestCount" to nextCount,
              "dailyTokensEstimate" to nextTokens,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
        Result.success(
          AiUsageQuota(
            id = uid,
            userId = uid,
            dayKey = dayKey,
            dailyRequestCount = nextCount,
            dailyTokensEstimate = nextTokens,
          )
        )
      } else {
        val existing =
          snap.toObject(
            AiUsageQuota::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          ) ?: AiUsageQuota(id = uid, userId = uid, dayKey = dayKey)

        val isSameDay = existing.dayKey == dayKey
        val baseCount = if (isSameDay) existing.dailyRequestCount else 0
        val baseTokens = if (isSameDay) existing.dailyTokensEstimate else 0

        if (baseCount >= safeMaxReq || baseTokens + estimatedTokensDelta > safeMaxTokens) {
          return Result.failure(
            FellahAiException(
              code = AiErrorCode.DAILY_QUOTA_EXCEEDED,
              message = "تم بلوغ الحد اليومي المسموح به لاستخدام Fellah AI. يرجى المحاولة غداً.",
            )
          )
        }

        val nextCount = (baseCount + 1).coerceIn(1, 100)
        val nextTokens = (baseTokens + estimatedTokensDelta).coerceIn(1, 300000)
        docRef
          .update(
            mapOf(
              "dayKey" to dayKey,
              "dailyRequestCount" to nextCount,
              "dailyTokensEstimate" to nextTokens,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()

        Result.success(
          AiUsageQuota(
            id = uid,
            userId = uid,
            dayKey = dayKey,
            dailyRequestCount = nextCount,
            dailyTokensEstimate = nextTokens,
          )
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, QUOTAS_COLLECTION)
      Result.failure(e)
    }
  }

  companion object {
    const val CONVERSATIONS_COLLECTION = "ai_conversations"
    const val MESSAGES_COLLECTION = "ai_messages"
    const val QUOTAS_COLLECTION = "ai_usage_quotas"
    const val DEFAULT_DAILY_REQUEST_LIMIT = 40
    const val DEFAULT_DAILY_TOKEN_LIMIT = 120000
  }
}
