package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AppLanguage
import com.example.data.model.MarketplaceOrder
import com.example.data.model.PartnerCatalogItem
import com.example.data.model.RoleSpecificProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RoleAccountRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveOnboardingProgress_multiRoleDeduplicationAndSwitchDefaultRole_succeeds() = runBlocking {
    val uid = signInTestUser("multi.role.farmer@fellah.dz")
    val repo = RoleAccountRepository(firestore, auth)

    // Step 1: Save resumable incomplete onboarding draft
    val draftRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.saveOnboardingProgress(
          displayName = "تعاونية الواحة",
          email = "multi.role.farmer@fellah.dz",
          preferredLanguage = AppLanguage.AR,
          selectedRoleCodes = emptyList(),
          chosenDefaultRole = "",
          step = 2,
          completed = false,
        )
      }
    assertTrue("Expected draft onboarding save to succeed: ${draftRes.exceptionOrNull()}", draftRes.isSuccess)
    assertFalse(draftRes.getOrThrow().onboardingCompleted)
    assertEquals(2, draftRes.getOrThrow().onboardingStep)

    // Step 2: Complete onboarding with duplicate roles passed — repository normalizes to unique list
    val completeRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.saveOnboardingProgress(
          displayName = "تعاونية الواحة",
          email = "multi.role.farmer@fellah.dz",
          preferredLanguage = AppLanguage.FR,
          selectedRoleCodes = listOf("farmer", "trader", "farmer", "storage_provider"),
          chosenDefaultRole = "farmer",
          step = 5,
          completed = true,
        )
      }
    assertTrue("Expected multi-role onboarding completion to succeed: ${completeRes.exceptionOrNull()}", completeRes.isSuccess)
    val saved = completeRes.getOrThrow()
    assertEquals(listOf("farmer", "trader", "storage_provider"), saved.assignedRoles)
    assertEquals("farmer", saved.defaultRole)
    assertTrue(saved.onboardingCompleted)

    // Reject switching default role to unassigned 'transporter'
    val invalidSwitch = withTimeout(DEFAULT_TIMEOUT_MS) { repo.updateDefaultRole("transporter") }
    assertTrue(invalidSwitch.isFailure)

    // Switch default role to assigned 'trader'
    val validSwitch = withTimeout(DEFAULT_TIMEOUT_MS) { repo.updateDefaultRole("trader") }
    assertTrue("Expected valid defaultRole switch to succeed", validSwitch.isSuccess)

    val observed =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeUserAccount(uid).first { it?.defaultRole == "trader" }
      }
    assertNotNull(observed)
    assertEquals("trader", observed?.defaultRole)
  }

  @Test
  fun saveRoleSpecificProfile_andCrossUserIsolation_enforced() = runBlocking {
    val aliceUid = signInTestUser("alice.profile@fellah.dz")
    val aliceRepo = RoleAccountRepository(firestore, auth)

    val saveProfileRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.saveRoleSpecificProfile(
          RoleSpecificProfile(
            roleCode = "storage_provider",
            businessName = "مركب التبريد بسكرة",
            wilaya = "07 - بسكرة (Biskra)",
            commune = "طولقة",
            activityType = "غرف تبريد موجبة +4°C",
            capacityOrArea = "500 طن",
            qualificationsOrDetails = "فرز وتوضيب التمور",
          )
        )
      }
    assertTrue("Expected saveRoleSpecificProfile to succeed: ${saveProfileRes.exceptionOrNull()}", saveProfileRes.isSuccess)

    val aliceProfiles =
      withTimeout(FLOW_TIMEOUT_MS) {
        aliceRepo.observeRoleProfiles(aliceUid).first { it.containsKey("storage_provider") }
      }
    assertTrue(aliceProfiles["storage_provider"]?.isProfileComplete == true)

    // Bob cannot read Alice's user account document
    signInTestUser("bob.intruder@fellah.dz")
    val bobRepo = RoleAccountRepository(firestore, auth)
    val crossRead = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getUserAccount(aliceUid) }
    assertTrue(crossRead.isFailure)
    val fsEx = crossRead.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
  }

  @Test
  fun publishPartnerCatalog_enforcesServerSideRbacAndRoleRemovalObligationCheck() = runBlocking {
    signInTestUser("rbac.operator@fellah.dz")
    val repo = RoleAccountRepository(firestore, auth)

    // Initialize user with ONLY 'farmer' role
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.saveOnboardingProgress(
        displayName = "Fellah Only",
        email = "rbac.operator@fellah.dz",
        preferredLanguage = AppLanguage.AR,
        selectedRoleCodes = listOf("farmer"),
        chosenDefaultRole = "farmer",
        step = 5,
        completed = true,
      ).getOrThrow()
    }

    // Attempt to publish a 'supplier' catalog item without holding 'supplier' role -> PERMISSION_DENIED
    val unauthorizedPublish =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.publishPartnerCatalogItem(
          PartnerCatalogItem(
            providerRole = "supplier",
            providerName = "Unauthorized Supplier",
            title = "بذور بطاطا سبونتا",
            category = "seeds_seedlings",
            wilaya = "07 - بسكرة (Biskra)",
            priceDzd = 14500.0,
            unitLabel = "د.ج / قنطار",
            availableQuantityOrCapacity = 80.0,
            description = "بذور معتمدة",
          )
        )
      }
    assertTrue("Expected server-side RBAC to reject unauthorized supplier publish", unauthorizedPublish.isFailure)
    assertEquals(
      FirebaseFirestoreException.Code.PERMISSION_DENIED,
      unauthorizedPublish.exceptionOrNull()?.unwrapFirestoreException()?.code,
    )

    // Add 'supplier' role to account
    val addRes = withTimeout(DEFAULT_TIMEOUT_MS) { repo.addRolesToAccount(listOf("supplier")) }
    assertTrue("Expected addRolesToAccount to succeed", addRes.isSuccess)
    assertEquals("supplier", addRes.getOrThrow().defaultRole)

    // Now publishing supplier catalog item succeeds!
    val authorizedPublish =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.publishPartnerCatalogItem(
          PartnerCatalogItem(
            providerRole = "supplier",
            providerName = "مشتلة وتموين الواحة",
            title = "بذور بطاطا سبونتا معتمدة",
            category = "seeds_seedlings",
            wilaya = "07 - بسكرة (Biskra)",
            priceDzd = 14500.0,
            unitLabel = "د.ج / قنطار",
            availableQuantityOrCapacity = 80.0,
            description = "بذور معتمدة عالية الإنتاجية",
          )
        )
      }
    assertTrue("Expected authorized supplier publish to succeed: ${authorizedPublish.exceptionOrNull()}", authorizedPublish.isSuccess)
    val itemId = authorizedPublish.getOrThrow()

    // Removing 'supplier' role while active catalog item exists is blocked safely
    val blockedRemove =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.removeRoleFromAccount("supplier", blockIfActiveObligations = true)
      }
    assertTrue("Expected active obligation check to block role removal", blockedRemove.isFailure)
    assertTrue(blockedRemove.exceptionOrNull()?.message?.startsWith("ACTIVE_OBLIGATIONS:") == true)

    // Pause the active item, then remove 'supplier' role -> succeeds and falls back defaultRole to 'farmer'
    withTimeout(DEFAULT_TIMEOUT_MS) { repo.updatePartnerCatalogStatus(itemId, "paused").getOrThrow() }
    val cleanRemove =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.removeRoleFromAccount("supplier", blockIfActiveObligations = true)
      }
    assertTrue("Expected role removal after pausing obligations to succeed: ${cleanRemove.exceptionOrNull()}", cleanRemove.isSuccess)
    assertEquals(listOf("farmer"), cleanRemove.getOrThrow().assignedRoles)
    assertEquals("farmer", cleanRemove.getOrThrow().defaultRole)
  }

  @Test
  fun marketplaceOrders_andSharedMessages_authenticatedAndUnauthenticatedChecks() = runBlocking {
    val traderUid = signInTestUser("trader.b2b@fellah.dz")
    val traderRepo = RoleAccountRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      traderRepo.saveOnboardingProgress(
        displayName = "مؤسسة الأمل للجملة",
        email = "trader.b2b@fellah.dz",
        preferredLanguage = AppLanguage.AR,
        selectedRoleCodes = listOf("trader"),
        chosenDefaultRole = "trader",
        step = 5,
        completed = true,
      ).getOrThrow()
    }

    val orderId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        traderRepo.createMarketplaceOrder(
          MarketplaceOrder(
            requesterName = "مؤسسة الأمل للجملة",
            requesterRole = "trader",
            providerId = "",
            targetRole = "transporter",
            orderType = "transport_quote",
            itemTitle = "شحن 15 طن تمور دقلة نور",
            wilaya = "07 - بسكرة (Biskra)",
            quantity = 15.0,
            unitLabel = "طن",
            proposedPriceDzd = 55000.0,
            notes = "نقل مبرد +4°C",
          )
        ).getOrThrow()
      }

    // Sign in as Transporter and quote on the open order + send shared message
    val transporterUid = signInTestUser("carrier.b2b@fellah.dz")
    val carrierRepo = RoleAccountRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      carrierRepo.saveOnboardingProgress(
        displayName = "أسطول الجنوب للنقل المبرد",
        email = "carrier.b2b@fellah.dz",
        preferredLanguage = AppLanguage.FR,
        selectedRoleCodes = listOf("transporter"),
        chosenDefaultRole = "transporter",
        step = 5,
        completed = true,
      ).getOrThrow()
    }

    val openOrders =
      withTimeout(FLOW_TIMEOUT_MS) {
        carrierRepo.observeOpenOrdersForRole("transporter").first { list ->
          list.any { it.id == orderId }
        }
      }
    assertTrue(openOrders.any { it.id == orderId })

    val quoteRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        carrierRepo.submitOrderQuotation(
          orderId = orderId,
          isBroadcastClaim = true,
          quotedPriceDzd = 52000.0,
          quoteNotes = "شاحنة مبردة متوفرة غداً 06:00 صباحاً",
        )
      }
    assertTrue("Expected submitOrderQuotation to succeed: ${quoteRes.exceptionOrNull()}", quoteRes.isSuccess)

    val msgRes =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        carrierRepo.sendMessage(
          senderName = "أسطول الجنوب",
          senderRole = "transporter",
          recipientId = traderUid,
          recipientName = "مؤسسة الأمل",
          subject = "عرض سعر نقل الشحنة",
          body = "تم إرسال عرض السعر بقيمة 52,000 د.ج مع ضمان التبريد.",
        )
      }
    assertTrue("Expected sendMessage to succeed: ${msgRes.exceptionOrNull()}", msgRes.isSuccess)
    assertTrue(transporterUid.isNotBlank())

    // Negative check: unauthenticated caller cannot observe user account
    auth.signOut()
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        carrierRepo.observeUserAccount(traderUid).first()
      }
      fail("Expected unauthenticated observeUserAccount to fail")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      assertNotNull(fsEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
