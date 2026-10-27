package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.AgriculturalParcel
import com.example.data.model.MarketListing
import com.example.data.model.ServiceBooking
import com.example.data.model.TransportRequest
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FellahRepository(
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

  fun observeUserParcels(userId: String): Flow<List<AgriculturalParcel>> {
    val path = PARCELS_COLLECTION
    return db
      .collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            AgriculturalParcel::class.java,
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

  suspend fun getParcelById(parcelId: String): Result<AgriculturalParcel> {
    val docRef = db.collection(PARCELS_COLLECTION).document(parcelId)
    return try {
      val snapshot = docRef.get().await()
      val parcel =
        snapshot.toObject(
          AgriculturalParcel::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ) ?: throw NoSuchElementException("Parcel not found: $parcelId")
      Result.success(parcel)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun saveParcel(parcel: AgriculturalParcel): Result<String> {
    val uid = requireUserId()
    val isCreate = parcel.id.isBlank()
    val targetId = if (isCreate) UUID.randomUUID().toString() else parcel.id
    val docRef = db.collection(PARCELS_COLLECTION).document(targetId)

    return try {
      if (isCreate) {
        val toCreate =
          parcel.copy(
            id = targetId,
            userId = uid,
            createdAt = null,
            updatedAt = null,
          )
        docRef.set(toCreate).await()
      } else {
        val existingCreatedAt =
          parcel.createdAt
            ?: docRef.get().await().getTimestamp("createdAt")
            ?: throw IllegalStateException("Existing parcel createdAt timestamp missing")
        val toUpdate =
          parcel.copy(
            id = targetId,
            userId = uid,
            createdAt = existingCreatedAt,
            updatedAt = null,
          )
        docRef.set(toUpdate).await()
      }
      Result.success(targetId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(
        e,
        if (isCreate) OperationType.CREATE else OperationType.UPDATE,
        docRef.path,
      )
      Result.failure(e)
    }
  }

  suspend fun deleteParcel(parcelId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection(PARCELS_COLLECTION).document(parcelId)
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

  fun observeActiveMarketListings(): Flow<List<MarketListing>> {
    val path = MARKET_COLLECTION
    return db
      .collection(path)
      .whereEqualTo("status", "active")
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            MarketListing::class.java,
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

  fun observeUserMarketListings(userId: String): Flow<List<MarketListing>> {
    val path = MARKET_COLLECTION
    return db
      .collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            MarketListing::class.java,
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

  suspend fun publishMarketListing(listing: MarketListing): Result<String> {
    val uid = requireUserId()
    require(listing.parcelId.isNotBlank()) { "Parcel must be saved before linking to market." }
    val listingId = UUID.randomUUID().toString()
    val listingRef = db.collection(MARKET_COLLECTION).document(listingId)
    val parcelRef = db.collection(PARCELS_COLLECTION).document(listing.parcelId)

    return try {
      val payload =
        listing.copy(
          id = listingId,
          userId = uid,
          status = "active",
          createdAt = null,
          updatedAt = null,
        )
      listingRef.set(payload).await()
      parcelRef
        .update(
          mapOf(
            "linkedToMarket" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(listingId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, listingRef.path)
      Result.failure(e)
    }
  }

  suspend fun markListingAsSold(listingId: String): Result<Unit> {
    requireUserId()
    val listingRef = db.collection(MARKET_COLLECTION).document(listingId)
    return try {
      listingRef
        .update(
          mapOf(
            "status" to "sold",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, listingRef.path)
      Result.failure(e)
    }
  }

  fun observeUserTransportRequests(userId: String): Flow<List<TransportRequest>> {
    val path = TRANSPORT_COLLECTION
    return db
      .collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            TransportRequest::class.java,
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

  suspend fun createTransportRequest(request: TransportRequest): Result<String> {
    val uid = requireUserId()
    val requestId = UUID.randomUUID().toString()
    val docRef = db.collection(TRANSPORT_COLLECTION).document(requestId)
    return try {
      val payload =
        request.copy(
          id = requestId,
          userId = uid,
          status = "pending",
          createdAt = null,
          updatedAt = null,
        )
      docRef.set(payload).await()
      Result.success(requestId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateTransportStatus(requestId: String, nextStatus: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection(TRANSPORT_COLLECTION).document(requestId)
    return try {
      docRef
        .update(
          mapOf(
            "status" to nextStatus,
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

  suspend fun deleteTransportRequest(requestId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection(TRANSPORT_COLLECTION).document(requestId)
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

  fun observeUserServiceBookings(userId: String): Flow<List<ServiceBooking>> {
    val path = SERVICES_COLLECTION
    return db
      .collection(path)
      .whereEqualTo("userId", userId)
      .snapshots()
      .map { snapshot ->
        snapshot
          .toObjects(
            ServiceBooking::class.java,
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

  suspend fun createServiceBooking(booking: ServiceBooking): Result<String> {
    val uid = requireUserId()
    val bookingId = UUID.randomUUID().toString()
    val docRef = db.collection(SERVICES_COLLECTION).document(bookingId)
    return try {
      val payload =
        booking.copy(
          id = bookingId,
          userId = uid,
          status = "requested",
          createdAt = null,
          updatedAt = null,
        )
      docRef.set(payload).await()
      Result.success(bookingId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteServiceBooking(bookingId: String): Result<Unit> {
    requireUserId()
    val docRef = db.collection(SERVICES_COLLECTION).document(bookingId)
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

  companion object {
    const val PARCELS_COLLECTION = "parcels"
    const val MARKET_COLLECTION = "market_listings"
    const val TRANSPORT_COLLECTION = "transport_requests"
    const val SERVICES_COLLECTION = "service_bookings"
  }
}
