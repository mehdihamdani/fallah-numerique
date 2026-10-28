package com.example.data.ai

import com.example.data.model.AppLanguage
import com.example.data.model.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class AiConversation(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val activeRole: String = "farmer",
  val language: String = "AR",
  val messageCount: Int = 0,
  val lastMessagePreview: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AiChatMessageRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val conversationId: String = "",
  val sender: String = "user",
  val content: String = "",
  val activeRole: String = "farmer",
  val verifiedDataSummary: String = "",
  val hasVerifiedPlatformData: Boolean = false,
  val promptTokensEstimate: Int = 0,
  val completionTokensEstimate: Int = 0,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AiUsageQuota(
  @DocumentId val id: String = "",
  val userId: String = "",
  val dayKey: String = "",
  val dailyRequestCount: Int = 0,
  val dailyTokensEstimate: Int = 0,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

/**
 * Validated input schema for agricultural production cost & break-even calculation.
 */
data class ProductionCostInput(
  val cropName: String,
  val areaHectares: Double,
  val seedsCostDzd: Double = 0.0,
  val fertilizerCostDzd: Double = 0.0,
  val irrigationWaterCostDzd: Double = 0.0,
  val laborAndMachineryCostDzd: Double = 0.0,
  val transportAndStorageCostDzd: Double = 0.0,
  val expectedYieldTonsPerHa: Double = 0.0,
  val targetSellingPriceKgDzd: Double? = null,
)

data class ProductionCostBreakdown(
  val cropName: String,
  val areaHectares: Double,
  val totalCostDzd: Double,
  val costPerHectareDzd: Double,
  val totalExpectedYieldKg: Double,
  val breakEvenPricePerKgDzd: Double?,
  val projectedRevenueDzd: Double?,
  val projectedNetMarginDzd: Double?,
  val formattedSummaryAr: String,
  val formattedSummaryFr: String,
  val formattedSummaryEn: String,
) {
  fun localizedSummary(language: AppLanguage): String =
    when (language) {
      AppLanguage.AR -> formattedSummaryAr
      AppLanguage.FR -> formattedSummaryFr
      AppLanguage.EN -> formattedSummaryEn
    }
}

/**
 * Equivalent to POST `/api/v1/ai/chat` request payload.
 */
data class AiChatRequest(
  val conversationId: String? = null,
  val message: String,
  val activeRole: UserRole,
  val language: AppLanguage,
  val productionCostInput: ProductionCostInput? = null,
)

/**
 * Equivalent to POST `/api/v1/ai/chat` structured response payload.
 */
data class AiChatResponse(
  val conversationId: String,
  val replyText: String,
  val verifiedDataSummary: String,
  val hasVerifiedPlatformData: Boolean,
  val suggestedFollowUps: List<String>,
  val remainingDailyQuota: Int,
  val providerId: String,
)

enum class AiErrorCode {
  VALIDATION_ERROR,
  RATE_LIMITED,
  DAILY_QUOTA_EXCEEDED,
  UNAUTHORIZED_ACCESS,
  PROVIDER_TIMEOUT,
  PROVIDER_UNAVAILABLE,
}

class FellahAiException(
  val code: AiErrorCode,
  override val message: String,
  cause: Throwable? = null,
) : Exception(message, cause)
