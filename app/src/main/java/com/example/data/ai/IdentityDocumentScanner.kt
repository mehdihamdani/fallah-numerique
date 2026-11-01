package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class NationalIdScanResult(
  val isDocumentDetected: Boolean,
  val fullName: String,
  val nationalIdNumber: String,
  val wilaya: String,
  val expiryOrBirthDate: String = "",
)

data class FarmerCardScanResult(
  val isDocumentDetected: Boolean,
  val farmerCardNumber: String,
  val holderName: String,
  val chamberWilaya: String,
  val farmOrExploitationName: String = "",
  val agriculturalSector: String = "",
)

/**
 * Abstraction for multimodal OCR extraction of Algerian National Identity Cards (CNI)
 * and optional Chamber of Agriculture Farmer Cards (Carte d'Agriculteur / بطاقة الفلاح).
 */
interface DocumentOcrScanner {
  suspend fun scanNationalIdCard(bitmap: Bitmap): Result<NationalIdScanResult>

  suspend fun scanFarmerCard(bitmap: Bitmap): Result<FarmerCardScanResult>
}

/**
 * Production multimodal document scanner backed by **Firebase AI Logic** (`gemini-3.8-flash`)
 * with structured JSON output (`Schema.obj`) and App Check attestation.
 */
object FirebaseAiDocumentScanner : DocumentOcrScanner {
  private const val TAG = "DocumentOcrScanner"
  private const val MODEL_NAME = "gemini-3.8-flash"
  private const val TIMEOUT_MS = 60_000L
  private const val MAX_IMAGE_DIMENSION_PX = 1280

  private val nationalIdSchema by lazy {
    Schema.obj(
      properties =
        mapOf(
          "isDocumentDetected" to
            Schema.boolean(
              description = "True if the image shows an identity card or official ID document"
            ),
          "fullName" to
            Schema.string(
              description = "Full name of the card holder in Arabic or Latin script, or empty string if unreadable"
            ),
          "nationalIdNumber" to
            Schema.string(
              description = "National identification number (NIN) or card number digits, or empty string if unreadable"
            ),
          "wilaya" to
            Schema.string(
              description = "Issuing Wilaya or place of residence if visible, otherwise empty string"
            ),
          "expiryOrBirthDate" to
            Schema.string(
              description = "Expiry date or birth date on the card if visible, otherwise empty string"
            ),
        ),
      optionalProperties = listOf("wilaya", "expiryOrBirthDate"),
    )
  }

  private val farmerCardSchema by lazy {
    Schema.obj(
      properties =
        mapOf(
          "isDocumentDetected" to
            Schema.boolean(
              description = "True if the image shows a Farmer Card (Carte d'Agriculteur / بطاقة الفلاح) or Chamber of Agriculture document"
            ),
          "farmerCardNumber" to
            Schema.string(
              description = "Registration or card number on the Farmer Card, or empty string if unreadable"
            ),
          "holderName" to
            Schema.string(
              description = "Full name of the farmer or cooperative on the card, or empty string"
            ),
          "chamberWilaya" to
            Schema.string(
              description = "Chamber of Agriculture Wilaya (الغرفة الفلاحية لولاية ...) if visible, otherwise empty string"
            ),
          "farmOrExploitationName" to
            Schema.string(
              description = "Name of the farm or agricultural exploitation if visible, otherwise empty string"
            ),
          "agriculturalSector" to
            Schema.string(
              description = "Agricultural sector or main crop/livestock activity if visible, otherwise empty string"
            ),
        ),
      optionalProperties = listOf("farmOrExploitationName", "agriculturalSector"),
    )
  }

  private val nationalIdModel by lazy {
    Firebase.ai.generativeModel(
      modelName = MODEL_NAME,
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = nationalIdSchema
          temperature = 0.1f
        },
    )
  }

  private val farmerCardModel by lazy {
    Firebase.ai.generativeModel(
      modelName = MODEL_NAME,
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = farmerCardSchema
          temperature = 0.1f
        },
    )
  }

  override suspend fun scanNationalIdCard(bitmap: Bitmap): Result<NationalIdScanResult> {
    val prepared = downscaleIfNeeded(bitmap)
    return try {
      val response =
        withTimeout(TIMEOUT_MS) {
          nationalIdModel.generateContent(
            content {
              image(prepared)
              text(
                "Extract the holder's full name, national ID / card number (NIN), and issuing Wilaya " +
                  "from this Algerian National Identity Card (بطاقة التعريف الوطنية / Carte Nationale d'Identité). " +
                  "Never invent data; return empty strings for fields that are not legible."
              )
            }
          )
        }
      val jsonText =
        response.text?.takeIf { it.isNotBlank() }
          ?: return Result.failure(IllegalStateException("لم يُرجع نموذج المسح الضوئي أي بيانات."))

      val json = JSONObject(jsonText)
      val detected = json.optBoolean("isDocumentDetected", false)
      val fullName = json.optString("fullName", "").trim().take(120)
      val idNumber = json.optString("nationalIdNumber", "").trim().take(40)
      val wilaya = json.optString("wilaya", "").trim().take(80)
      val dateStr = json.optString("expiryOrBirthDate", "").trim().take(40)

      Result.success(
        NationalIdScanResult(
          isDocumentDetected = detected || idNumber.isNotEmpty() || fullName.isNotEmpty(),
          fullName = fullName,
          nationalIdNumber = idNumber,
          wilaya = wilaya,
          expiryOrBirthDate = dateStr,
        )
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "National ID scan timed out", e)
      Result.failure(
        IllegalStateException("انتهت مهلة مسح بطاقة التعريف (60 ثانية). يرجى المحاولة مجدداً أو إدخال الرقم يدوياً.")
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Failed to scan National ID card via Firebase AI", e)
      Result.failure(e)
    }
  }

  override suspend fun scanFarmerCard(bitmap: Bitmap): Result<FarmerCardScanResult> {
    val prepared = downscaleIfNeeded(bitmap)
    return try {
      val response =
        withTimeout(TIMEOUT_MS) {
          farmerCardModel.generateContent(
            content {
              image(prepared)
              text(
                "Extract the Farmer Card registration number, holder name, Chamber of Agriculture Wilaya, " +
                  "farm/exploitation name, and agricultural sector from this Algerian Farmer Card " +
                  "(بطاقة الفلاح / Carte d'Agriculteur - Chambre d'Agriculture). " +
                  "Never invent data; return empty strings for fields that are not legible."
              )
            }
          )
        }
      val jsonText =
        response.text?.takeIf { it.isNotBlank() }
          ?: return Result.failure(IllegalStateException("لم يُرجع نموذج مسح بطاقة الفلاح أي بيانات."))

      val json = JSONObject(jsonText)
      val detected = json.optBoolean("isDocumentDetected", false)
      val cardNumber = json.optString("farmerCardNumber", "").trim().take(60)
      val holder = json.optString("holderName", "").trim().take(120)
      val wilaya = json.optString("chamberWilaya", "").trim().take(80)
      val farmName = json.optString("farmOrExploitationName", "").trim().take(120)
      val sector = json.optString("agriculturalSector", "").trim().take(120)

      Result.success(
        FarmerCardScanResult(
          isDocumentDetected = detected || cardNumber.isNotEmpty() || holder.isNotEmpty(),
          farmerCardNumber = cardNumber,
          holderName = holder,
          chamberWilaya = wilaya,
          farmOrExploitationName = farmName,
          agriculturalSector = sector,
        )
      )
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Farmer Card scan timed out", e)
      Result.failure(
        IllegalStateException("انتهت مهلة مسح بطاقة الفلاح (60 ثانية). يرجى المحاولة مجدداً أو إدخال الرقم يدوياً.")
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Failed to scan Farmer Card via Firebase AI", e)
      Result.failure(e)
    }
  }

  private fun downscaleIfNeeded(source: Bitmap): Bitmap {
    val maxSide = max(source.width, source.height)
    if (maxSide <= MAX_IMAGE_DIMENSION_PX || maxSide <= 0) return source
    val ratio = MAX_IMAGE_DIMENSION_PX.toFloat() / maxSide.toFloat()
    val targetW = (source.width * ratio).roundToInt().coerceAtLeast(1)
    val targetH = (source.height * ratio).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, targetW, targetH, true)
  }
}
