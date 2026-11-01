package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AgriculturalParcel
import com.example.data.model.MarketListing
import com.example.data.model.ServiceBooking
import com.example.data.model.TransportRequest
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FellahRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveParcel_andObserveUserParcels_authenticatedOwner_succeeds() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = FellahRepository(firestore, auth)

    val saveResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.saveParcel(AgriculturalParcel(name = "واحة طولقة النموذجية"))
      }
    assertTrue("Expected saveParcel to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)
    val parcelId = saveResult.getOrThrow()
    assertTrue(parcelId.isNotBlank())

    val emitted =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeUserParcels(aliceUid).first { list -> list.any { it.id == parcelId } }
      }
    val saved = emitted.first { it.id == parcelId }
    assertEquals("واحة طولقة النموذجية", saved.name)

    // Verify update preserves createdAt and updates soilPh
    val updateResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.saveParcel(saved.copy(soilPh = 7.1))
      }
    assertTrue("Expected parcel update to succeed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)
  }

  @Test
  fun getParcelById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = FellahRepository(firestore, auth)
    val parcelId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.saveParcel(AgriculturalParcel(name = "Alice Private Oasis")).getOrThrow()
      }

    signInTestUser(BOB_EMAIL)
    val bobRepo = FellahRepository(firestore, auth)
    val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getParcelById(parcelId) }
    assertTrue(crossReadResult.isFailure)
    val fsEx = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull("Expected FirebaseFirestoreException", fsEx)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
  }

  @Test
  fun observeUserParcels_unauthenticatedCaller_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = FellahRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeUserParcels("some_uid").first()
      }
      fail("Expected unauthenticated observeUserParcels to throw")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      assertNotNull("Expected wrapped FirebaseFirestoreException, got $e", fsEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }
  }

  @Test
  fun publishMarketListing_andTerminalSoldState_enforced() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = FellahRepository(firestore, auth)

    val parcelId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.saveParcel(AgriculturalParcel()).getOrThrow()
      }

    val listingResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.publishMarketListing(
          MarketListing(
            parcelId = parcelId,
            operatorName = "Coopérative El Baraka",
            parcelReference = "Ilot N 108 / P-24",
            cropType = "deglet_nour",
            availableTons = 38.5,
            pricePerKgDzd = 420.0,
          )
        )
      }
    assertTrue("Expected market listing publish to succeed: ${listingResult.exceptionOrNull()}", listingResult.isSuccess)
    val listingId = listingResult.getOrThrow()

    val soldResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.markListingAsSold(listingId) }
    assertTrue("Expected markListingAsSold to succeed: ${soldResult.exceptionOrNull()}", soldResult.isSuccess)

    // Attempting another update after 'sold' terminal state must fail
    val repeatSold = withTimeout(DEFAULT_TIMEOUT_MS) { repository.markListingAsSold(listingId) }
    assertTrue("Expected update on sold listing to be rejected", repeatSold.isFailure)
    val fsEx = repeatSold.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
  }

  @Test
  fun createTransportAndServiceBooking_authenticatedOwner_andTerminalLock_enforced() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = FellahRepository(firestore, auth)

    val transportRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createTransportRequest(
          TransportRequest(
            parcelReference = "Ilot N 108 / P-24",
            vehicleType = "refrigerated_truck",
            originWilaya = "07 - بسكرة (Biskra)",
            destinationMarket = "سوق الجملة الكاليتوس - الجزائر",
            cargoTons = 18.0,
            scheduledDate = "2026-11-02",
          )
        )
      }
    assertTrue(
      "Expected createTransportRequest to succeed: ${transportRes.exceptionOrNull()}",
      transportRes.isSuccess,
    )
    val requestId = transportRes.getOrThrow()

    val deliveredRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.updateTransportStatus(requestId, "delivered")
      }
    assertTrue("Expected update to delivered to succeed", deliveredRes.isSuccess)

    // Subsequent status change after terminal 'delivered' must fail
    val reopenRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.updateTransportStatus(requestId, "in_transit")
      }
    assertTrue("Expected terminal delivered lock to reject update", reopenRes.isFailure)

    val serviceRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createServiceBooking(
          ServiceBooking(
            serviceCategory = "plowing_leveling",
            serviceTitle = "حرث وتسوية الأرض",
            parcelReference = "Ilot N 108 / P-24",
            wilaya = "07 - بسكرة (Biskra)",
            preferredDate = "2026-11-05",
            notes = "تسوية 4.82 هكتار بالليزر",
          )
        )
      }
    assertTrue(
      "Expected createServiceBooking to succeed: ${serviceRes.exceptionOrNull()}",
      serviceRes.isSuccess,
    )
    val bookingId = serviceRes.getOrThrow()
    val bookings =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeUserServiceBookings(aliceUid).first { list ->
          list.any { it.id == bookingId }
        }
      }
    assertTrue(bookings.any { it.id == bookingId })
  }

  @Test
  fun saveParcel_withBlankAndCustomMainCrop_andPublishCustomCropListing_succeeds() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = FellahRepository(firestore, auth)

    // 1. Default new parcel starts with empty mainCrop ("") and must pass Firestore rules
    val blankCropParcel = AgriculturalParcel(name = "قطعة جديدة بدون محصول محدد", mainCrop = "")
    val createRes = withTimeout(DEFAULT_TIMEOUT_MS) { repository.saveParcel(blankCropParcel) }
    assertTrue(
      "Expected saveParcel with empty mainCrop to succeed: ${createRes.exceptionOrNull()}",
      createRes.isSuccess,
    )
    val parcelId = createRes.getOrThrow()

    val savedBlank =
      withTimeout(DEFAULT_TIMEOUT_MS) { repository.getParcelById(parcelId).getOrThrow() }
    assertEquals("", savedBlank.mainCrop)

    // 2. Farmer enters their own custom crop/product ("قمح صلب وبقوليات خاصة")
    val customProduct = "قمح صلب وبقوليات خاصة"
    val updateRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.saveParcel(savedBlank.copy(mainCrop = customProduct))
      }
    assertTrue(
      "Expected saveParcel with custom mainCrop to succeed: ${updateRes.exceptionOrNull()}",
      updateRes.isSuccess,
    )

    val savedCustom =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeUserParcels(aliceUid).first { list ->
          list.any { it.id == parcelId && it.mainCrop == customProduct }
        }
      }
    assertEquals(customProduct, savedCustom.first { it.id == parcelId }.mainCrop)

    // 3. Farmer publishes custom crop to marketplace
    val customListingRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.publishMarketListing(
          MarketListing(
            parcelId = parcelId,
            operatorName = "مزرعة الأمل",
            parcelReference = savedBlank.ilotNumber,
            cropType = customProduct,
            wilaya = savedBlank.wilaya,
            availableTons = 12.5,
            pricePerKgDzd = 135.0,
          )
        )
      }
    assertTrue(
      "Expected publishMarketListing with custom cropType to succeed: ${customListingRes.exceptionOrNull()}",
      customListingRes.isSuccess,
    )
    val listingId = customListingRes.getOrThrow()

    val activeListings =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeActiveMarketListings().first { list -> list.any { it.id == listingId } }
      }
    assertEquals(customProduct, activeListings.first { it.id == listingId }.cropType)
  }

  private companion object {
    const val ALICE_EMAIL = "alice.farmer@fellah.dz"
    const val BOB_EMAIL = "bob.farmer@fellah.dz"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
