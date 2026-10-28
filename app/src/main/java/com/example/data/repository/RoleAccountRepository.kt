package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.AppLanguage
import com.example.data.model.MarketplaceOrder
import com.example.data.model.PartnerCatalogItem
import com.example.data.model.PlatformMessage
import com.example.data.model.RoleSpecificProfile
import com.example.data.model.UserAccountProfile
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Repository managing multi-role user accounts (/me, /me/roles, /me/default-role, /me/onboarding),
 * progressive role-specific profiles, RBAC partner catalog items, B2B marketplace orders,
 * and shared cross-role messages.
 */
class RoleAccountRepository(
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
      ?: throw IllegalStateException(
        "User must be signed in with Google before accessing Firestore."
      )
  }

  /**
   * Observes the current authenticated user's multi-role account profile (`/users/{userId}`).
   * Emits `null` when the user has not yet initialized their role profile (e.g., new or pre-migration user).
   */
  fun observeUserAccount(userId: String): Flow<UserAccountProfile?> {
    val docRef = db.collection(USERS_COLLECTION).document(userId)
    return docRef
      .snapshots()
      .map { snapshot ->
        if (!snapshot.exists()) {
          null
        } else {
          snapshot.toObject(
            UserAccountProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
      }
      .catch { error ->
        if (error is Exception) {
          handleFirestoreError(error, OperationType.GET, docRef.path)
        }
        throw error
      }
  }

  suspend fun getUserAccount(userId: String): Result<UserAccountProfile?> {
    val docRef = db.collection(USERS_COLLECTION).document(userId)
    return try {
      val snapshot = docRef.get().await()
      val profile =
        if (snapshot.exists()) {
          snapshot.toObject(
            UserAccountProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }
      Result.success(profile)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Saves resumable onboarding progress (`/me/onboarding`) or completes initial multi-role setup.
   * Rejects invalid role identifiers, prevents duplicates, and ensures `defaultRole` belongs to `assignedRoles`.
   */
  suspend fun saveOnboardingProgress(
    displayName: String,
    email: String,
    preferredLanguage: AppLanguage,
    selectedRoleCodes: List<String>,
    chosenDefaultRole: String,
    step: Int,
    completed: Boolean,
  ): Result<UserAccountProfile> {
    val uid = requireUserId()
    val rawTrimmed = selectedRoleCodes.map { it.trim().lowercase() }.filter { it.isNotBlank() }
    val invalidCodes = rawTrimmed.filter { it !in UserRole.validCodes }
    if (invalidCodes.isNotEmpty()) {
      return Result.failure(
        IllegalArgumentException("Invalid role identifier(s): ${invalidCodes.joinToString()}")
      )
    }

    val normalizedRoles = UserRole.normalizeRoleCodes(rawTrimmed)
    if (completed && normalizedRoles.isEmpty()) {
      return Result.failure(
        IllegalArgumentException("At least one professional activity must be selected.")
      )
    }

    val resolvedDefault =
      when {
        normalizedRoles.isEmpty() -> ""
        chosenDefaultRole.isNotBlank() -> {
          val candidate = chosenDefaultRole.trim().lowercase()
          if (candidate !in normalizedRoles) {
            return Result.failure(
              IllegalArgumentException("Default role ($candidate) must be one of the assigned roles.")
            )
          }
          candidate
        }
        else -> normalizedRoles.first()
      }

    val safeStep = step.coerceIn(1, 5)
    val safeName = displayName.trim().ifBlank { "Opérateur Fellah Numérique" }.take(115)
    val safeEmail = email.trim().take(155)
    val docRef = db.collection(USERS_COLLECTION).document(uid)

    return try {
      val existingSnap = docRef.get().await()
      if (!existingSnap.exists()) {
        val payload =
          mapOf(
            "userId" to uid,
            "displayName" to safeName,
            "email" to safeEmail,
            "preferredLanguage" to preferredLanguage.code,
            "assignedRoles" to normalizedRoles,
            "defaultRole" to resolvedDefault,
            "onboardingCompleted" to completed,
            "onboardingStep" to safeStep,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
      } else {
        val updates =
          mapOf(
            "displayName" to safeName,
            "email" to safeEmail,
            "preferredLanguage" to preferredLanguage.code,
            "assignedRoles" to normalizedRoles,
            "defaultRole" to resolvedDefault,
            "onboardingCompleted" to completed,
            "onboardingStep" to safeStep,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.update(updates).await()
      }

      val saved =
        docRef
          .get()
          .await()
          .toObject(
            UserAccountProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          ?: UserAccountProfile(
            id = uid,
            userId = uid,
            displayName = safeName,
            email = safeEmail,
            preferredLanguage = preferredLanguage.code,
            assignedRoles = normalizedRoles,
            defaultRole = resolvedDefault,
            onboardingCompleted = completed,
            onboardingStep = safeStep,
          )
      Result.success(saved)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Switches and persists the user's active/default role (`/me/default-role`).
   * Rejects switching into any role not currently assigned to the user.
   */
  suspend fun updateDefaultRole(roleCode: String): Result<Unit> {
    val uid = requireUserId()
    val cleanRole = roleCode.trim().lowercase()
    if (cleanRole !in UserRole.validCodes) {
      return Result.failure(IllegalArgumentException("Invalid role identifier: $cleanRole"))
    }
    val docRef = db.collection(USERS_COLLECTION).document(uid)
    return try {
      val current =
        docRef
          .get()
          .await()
          .toObject(
            UserAccountProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          ?: return Result.failure(IllegalStateException("User profile not initialized."))

      if (cleanRole !in current.assignedRoles) {
        return Result.failure(
          IllegalArgumentException("Cannot switch to unassigned role: $cleanRole")
        )
      }

      docRef
        .update(
          mapOf(
            "defaultRole" to cleanRole,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updatePreferredLanguage(language: AppLanguage): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection(USERS_COLLECTION).document(uid)
    return try {
      val snap = docRef.get().await()
      if (snap.exists()) {
        docRef
          .update(
            mapOf(
              "preferredLanguage" to language.code,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Adds one or more roles to an existing account (`/me/roles`) without duplicating roles.
   */
  suspend fun addRolesToAccount(
    newRoleCodes: List<String>,
    makeFirstActive: Boolean = true,
  ): Result<UserAccountProfile> {
    val uid = requireUserId()
    val invalid = newRoleCodes.map { it.trim().lowercase() }.filter { it !in UserRole.validCodes }
    if (invalid.isNotEmpty()) {
      return Result.failure(IllegalArgumentException("Invalid role(s): ${invalid.joinToString()}"))
    }
    val docRef = db.collection(USERS_COLLECTION).document(uid)
    return try {
      val current =
        docRef
          .get()
          .await()
          .toObject(
            UserAccountProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          ?: return Result.failure(IllegalStateException("User profile not found."))

      val merged = UserRole.normalizeRoleCodes(current.assignedRoles + newRoleCodes)
      val nextDefault =
        if (makeFirstActive && newRoleCodes.isNotEmpty()) {
          newRoleCodes.first().trim().lowercase()
        } else if (current.defaultRole in merged) {
          current.defaultRole
        } else {
          merged.first()
        }

      docRef
        .update(
          mapOf(
            "assignedRoles" to merged,
            "defaultRole" to nextDefault,
            "onboardingCompleted" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      val updated =
        docRef
          .get()
          .await()
          .toObject(
            UserAccountProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          ) ?: current.copy(assignedRoles = merged, defaultRole = nextDefault)
      Result.success(updated)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Removes an assigned role (`DELETE /me/roles/:role`) safely:
   * - Prevents removing the user's sole remaining role.
   * - Checks for active catalog items or pending/accepted orders tied to that role unless `forceAfterWarning` is false (blocks when active obligations exist).
   * - Automatically updates `defaultRole` to a remaining assigned role if the active role was removed.
   * - Never deletes historical business records or role profiles.
   */
  suspend fun removeRoleFromAccount(
    roleCodeToRemove: String,
    blockIfActiveObligations: Boolean = true,
  ): Result<UserAccountProfile> {
    val uid = requireUserId()
    val cleanRole = roleCodeToRemove.trim().lowercase()
    val docRef = db.collection(USERS_COLLECTION).document(uid)
    return try {
      val current =
        docRef
          .get()
          .await()
          .toObject(
            UserAccountProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          ?: return Result.failure(IllegalStateException("User profile not found."))

      if (cleanRole !in current.assignedRoles) {
        return Result.failure(IllegalArgumentException("Role is not assigned: $cleanRole"))
      }
      if (current.assignedRoles.size <= 1) {
        return Result.failure(
          IllegalStateException("Cannot remove your only active role. Add another role first.")
        )
      }

      if (blockIfActiveObligations) {
        val activeCatalogCount =
          if (cleanRole != UserRole.TRADER.code) {
            db.collection(PARTNER_CATALOG_COLLECTION)
              .whereEqualTo("userId", uid)
              .whereEqualTo("providerRole", cleanRole)
              .whereEqualTo("status", "active")
              .get()
              .await()
              .size()
          } else {
            0
          }
        val activeProviderOrders =
          db.collection(ORDERS_COLLECTION)
            .whereEqualTo("providerId", uid)
            .whereEqualTo("targetRole", cleanRole)
            .get()
            .await()
            .documents
            .count { snap ->
              val st = snap.getString("status") ?: ""
              st in listOf("pending", "quoted", "accepted")
            }
        val activeRequesterOrders =
          db.collection(ORDERS_COLLECTION)
            .whereEqualTo("requesterId", uid)
            .whereEqualTo("requesterRole", cleanRole)
            .get()
            .await()
            .documents
            .count { snap ->
              val st = snap.getString("status") ?: ""
              st in listOf("pending", "quoted", "accepted")
            }

        val activeMarketListings =
          if (cleanRole == UserRole.FARMER.code) {
            db.collection(FellahRepository.MARKET_COLLECTION)
              .whereEqualTo("userId", uid)
              .whereEqualTo("status", "active")
              .get()
              .await()
              .size()
          } else {
            0
          }

        val totalObligations =
          activeCatalogCount + activeProviderOrders + activeRequesterOrders + activeMarketListings
        if (totalObligations > 0) {
          return Result.failure(
            IllegalStateException(
              "ACTIVE_OBLIGATIONS:$totalObligations:Pause or complete active listings/orders under this role before removing it."
            )
          )
        }
      }

      val remaining = current.assignedRoles.filterNot { it == cleanRole }
      val nextDefault =
        if (current.defaultRole == cleanRole || current.defaultRole !in remaining) {
          remaining.first()
        } else {
          current.defaultRole
        }

      docRef
        .update(
          mapOf(
            "assignedRoles" to remaining,
            "defaultRole" to nextDefault,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      Result.success(current.copy(assignedRoles = remaining, defaultRole = nextDefault))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Observes all role-specific profiles belonging to `userId`.
   */
  fun observeRoleProfiles(userId: String): Flow<Map<String, RoleSpecificProfile>> {
    val path = ROLE_PROFILES_COLLECTION
    return db
      .collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            RoleSpecificProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .associateBy { it.roleCode }
      }
      .catch { error ->
        if (error is Exception) {
          handleFirestoreError(error, OperationType.LIST, path)
        }
        throw error
      }
  }

  suspend fun saveRoleSpecificProfile(profile: RoleSpecificProfile): Result<Unit> {
    val uid = requireUserId()
    val cleanRole = profile.roleCode.trim().lowercase()
    if (cleanRole !in UserRole.validCodes) {
      return Result.failure(IllegalArgumentException("Invalid role code: $cleanRole"))
    }
    val docId = "${uid}_$cleanRole"
    val docRef = db.collection(ROLE_PROFILES_COLLECTION).document(docId)
    val complete =
      profile.businessName.isNotBlank() &&
        (profile.wilaya.isNotBlank() || profile.activityType.isNotBlank())

    return try {
      val snap = docRef.get().await()
      if (!snap.exists()) {
        val createMap =
          mapOf(
            "userId" to uid,
            "roleCode" to cleanRole,
            "businessName" to profile.businessName.trim().take(115),
            "wilaya" to profile.wilaya.trim().take(75),
            "commune" to profile.commune.trim().take(75),
            "activityType" to profile.activityType.trim().take(115),
            "capacityOrArea" to profile.capacityOrArea.trim().take(115),
            "qualificationsOrDetails" to profile.qualificationsOrDetails.trim().take(290),
            "isProfileComplete" to complete,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(createMap).await()
      } else {
        val updateMap =
          mapOf(
            "businessName" to profile.businessName.trim().take(115),
            "wilaya" to profile.wilaya.trim().take(75),
            "commune" to profile.commune.trim().take(75),
            "activityType" to profile.activityType.trim().take(115),
            "capacityOrArea" to profile.capacityOrArea.trim().take(115),
            "qualificationsOrDetails" to profile.qualificationsOrDetails.trim().take(290),
            "isProfileComplete" to complete,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.update(updateMap).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Observes active Partner Catalog items across the marketplace (supplies, vehicles, services, storage).
   */
  fun observeActivePartnerCatalog(): Flow<List<PartnerCatalogItem>> {
    val path = PARTNER_CATALOG_COLLECTION
    return db
      .collection(path)
      .whereEqualTo("status", "active")
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            PartnerCatalogItem::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) {
          handleFirestoreError(error, OperationType.LIST, path)
        }
        throw error
      }
  }

  fun observeUserPartnerCatalog(userId: String): Flow<List<PartnerCatalogItem>> {
    val path = PARTNER_CATALOG_COLLECTION
    return db
      .collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            PartnerCatalogItem::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) {
          handleFirestoreError(error, OperationType.LIST, path)
        }
        throw error
      }
  }

  suspend fun publishPartnerCatalogItem(item: PartnerCatalogItem): Result<String> {
    val uid = requireUserId()
    val itemId = UUID.randomUUID().toString()
    val docRef = db.collection(PARTNER_CATALOG_COLLECTION).document(itemId)
    return try {
      val payload =
        mapOf(
          "userId" to uid,
          "providerRole" to item.providerRole.trim().lowercase(),
          "providerName" to item.providerName.trim().ifBlank { "Opérateur Agréé" }.take(115),
          "title" to item.title.trim().take(115),
          "category" to item.category.trim().ifBlank { "general" }.take(55),
          "wilaya" to item.wilaya.trim().ifBlank { "07 - بسكرة (Biskra)" }.take(75),
          "priceDzd" to item.priceDzd.coerceIn(0.0, 10_000_000.0),
          "unitLabel" to item.unitLabel.trim().ifBlank { "د.ج / وحدة" }.take(38),
          "availableQuantityOrCapacity" to
            item.availableQuantityOrCapacity.coerceIn(0.0, 1_000_000.0),
          "description" to item.description.trim().take(290),
          "status" to "active",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(itemId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updatePartnerCatalogStatus(itemId: String, status: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection(PARTNER_CATALOG_COLLECTION).document(itemId)
    return try {
      docRef
        .update(
          mapOf(
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deletePartnerCatalogItem(itemId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection(PARTNER_CATALOG_COLLECTION).document(itemId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Observes orders created by `userId` combined with orders directly addressed to `userId`.
   */
  fun observeUserOrders(userId: String): Flow<List<MarketplaceOrder>> {
    val path = ORDERS_COLLECTION
    val outgoingFlow =
      db.collection(path)
        .whereEqualTo("requesterId", userId)
        .snapshots()
        .map { snap ->
          snap.toObjects(
            MarketplaceOrder::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
    val incomingDirectFlow =
      db.collection(path)
        .whereEqualTo("providerId", userId)
        .snapshots()
        .map { snap ->
          snap.toObjects(
            MarketplaceOrder::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }

    return combine(outgoingFlow, incomingDirectFlow) { outgoing, incoming ->
        (outgoing + incoming)
          .distinctBy { it.id }
          .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) {
          handleFirestoreError(error, OperationType.LIST, path)
        }
        throw error
      }
  }

  /**
   * Observes open broadcast orders (`providerId == ""`, `status == "pending"`) targeting a specific role
   * that the authenticated user holds.
   */
  fun observeOpenOrdersForRole(roleCode: String): Flow<List<MarketplaceOrder>> {
    val path = ORDERS_COLLECTION
    return db
      .collection(path)
      .whereEqualTo("providerId", "")
      .whereEqualTo("status", "pending")
      .whereEqualTo("targetRole", roleCode)
      .snapshots()
      .map { snap ->
        snap
          .toObjects(
            MarketplaceOrder::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
          .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) {
          handleFirestoreError(error, OperationType.LIST, path)
        }
        throw error
      }
  }

  suspend fun createMarketplaceOrder(order: MarketplaceOrder): Result<String> {
    val uid = requireUserId()
    val orderId = UUID.randomUUID().toString()
    val docRef = db.collection(ORDERS_COLLECTION).document(orderId)
    return try {
      val payload =
        mapOf(
          "requesterId" to uid,
          "requesterName" to order.requesterName.trim().ifBlank { "Opérateur" }.take(115),
          "requesterRole" to order.requesterRole.trim().lowercase(),
          "providerId" to order.providerId.trim().take(128),
          "targetRole" to order.targetRole.trim().lowercase(),
          "targetItemId" to order.targetItemId.trim().take(128),
          "orderType" to order.orderType.trim(),
          "itemTitle" to order.itemTitle.trim().take(115),
          "wilaya" to order.wilaya.trim().ifBlank { "07 - بسكرة (Biskra)" }.take(75),
          "quantity" to order.quantity.coerceIn(0.1, 1_000_000.0),
          "unitLabel" to order.unitLabel.trim().ifBlank { "طن" }.take(38),
          "proposedPriceDzd" to order.proposedPriceDzd.coerceIn(0.0, 100_000_000.0),
          "notes" to order.notes.trim().take(290),
          "status" to "pending",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(orderId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Allows a provider holding the target role to submit a quotation on an open or assigned order.
   */
  suspend fun submitOrderQuotation(
    orderId: String,
    isBroadcastClaim: Boolean,
    quotedPriceDzd: Double,
    quoteNotes: String,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection(ORDERS_COLLECTION).document(orderId)
    return try {
      val updates =
        mutableMapOf<String, Any>(
          "proposedPriceDzd" to quotedPriceDzd.coerceIn(0.0, 100_000_000.0),
          "notes" to quoteNotes.trim().take(290),
          "status" to "quoted",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (isBroadcastClaim) {
        updates["providerId"] = uid
      }
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateOrderStatus(
    orderId: String,
    nextStatus: String,
    notes: String? = null,
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection(ORDERS_COLLECTION).document(orderId)
    return try {
      val updates =
        mutableMapOf<String, Any>(
          "status" to nextStatus,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (notes != null) {
        updates["notes"] = notes.trim().take(290)
      }
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  /**
   * Observes shared account messages (both sent and received) across all assigned roles.
   */
  fun observeAccountMessages(userId: String): Flow<List<PlatformMessage>> {
    val path = MESSAGES_COLLECTION
    val sentFlow =
      db.collection(path)
        .whereEqualTo("senderId", userId)
        .snapshots()
        .map { snap ->
          snap.toObjects(
            PlatformMessage::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
    val receivedFlow =
      db.collection(path)
        .whereEqualTo("recipientId", userId)
        .snapshots()
        .map { snap ->
          snap.toObjects(
            PlatformMessage::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }

    return combine(sentFlow, receivedFlow) { sent, received ->
        (sent + received)
          .distinctBy { it.id }
          .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
      }
      .catch { error ->
        if (error is Exception) {
          handleFirestoreError(error, OperationType.LIST, path)
        }
        throw error
      }
  }

  suspend fun sendMessage(
    senderName: String,
    senderRole: String,
    recipientId: String,
    recipientName: String,
    subject: String,
    body: String,
  ): Result<String> {
    val uid = requireUserId()
    val msgId = UUID.randomUUID().toString()
    val docRef = db.collection(MESSAGES_COLLECTION).document(msgId)
    return try {
      val payload =
        mapOf(
          "senderId" to uid,
          "senderName" to senderName.trim().ifBlank { "Opérateur" }.take(115),
          "senderRole" to senderRole.trim().lowercase(),
          "recipientId" to recipientId.trim().take(128),
          "recipientName" to recipientName.trim().ifBlank { "Destinataire" }.take(115),
          "subject" to subject.trim().ifBlank { "استفسار تجاري" }.take(115),
          "body" to body.trim().take(490),
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(msgId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun markMessageAsRead(messageId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection(MESSAGES_COLLECTION).document(messageId)
    return try {
      docRef
        .update(
          mapOf(
            "isRead" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  companion object {
    const val USERS_COLLECTION = "users"
    const val ROLE_PROFILES_COLLECTION = "role_profiles"
    const val PARTNER_CATALOG_COLLECTION = "partner_catalog"
    const val ORDERS_COLLECTION = "marketplace_orders"
    const val MESSAGES_COLLECTION = "messages"
  }
}
