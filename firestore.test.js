const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_farmer_01";
const BOB_UID = "bob_farmer_02";
const CHARLIE_UID = "charlie_outsider_03";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function buildValidParcel(userId, serverTimestamp) {
  return {
    userId,
    name: "قطاع بسكرة - طولقة",
    wilaya: "07 - بسكرة (Biskra)",
    commune: "طولقة (Tolga Oasis)",
    ilotNumber: "Ilot N 108 / P-24",
    tenureType: "امتياز فلاحي (Concession)",
    areaHectares: 4.82,
    coordinatesLabel: "34°43'12.4\"N 5°22'48.9\"E",
    polygonPoints: ["0.20,0.23", "0.74,0.20", "0.85,0.75", "0.51,0.85", "0.24,0.71"],
    wellPoint: "0.40,0.44",
    wellDepthLabel: "بئر ارتوازي عميق 85m",
    ndviScore: 0.74,
    soilTexture: "sableux_limoneux",
    soilPh: 7.3,
    organicMatterPercent: 2.45,
    salinityEc: 1.8,
    drainageStatus: "عميق > 120 سم / تصريف ممتاز",
    labDocumentName: "analyse_inraa_biskra.pdf",
    mainCrop: "deglet_nour",
    irrigationSystem: "السقي بالتقطير الحديث (Goutte-à-goutte)",
    estimatedYieldTonsPerHa: 38.5,
    plantingYear: "2018 (أشجار منتجة)",
    intercropping: "فصة علفية + خضار تحت النخيل",
    linkedToMarket: false,
    createdAt: serverTimestamp,
    updatedAt: serverTimestamp,
  };
}

function buildUserAccount(userId, assignedRoles, defaultRole, onboardingCompleted, onboardingStep, now) {
  return {
    userId,
    displayName: "El Baraka Operator",
    email: `${userId}@fellah-numerique.dz`,
    preferredLanguage: "AR",
    assignedRoles,
    defaultRole,
    onboardingCompleted,
    onboardingStep,
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("1. Unauthenticated user cannot read or create parcels", async () => {
  const unauthCtx = testEnv.unauthenticatedContext();
  const unauthDb = unauthCtx.firestore();
  await assertFails(unauthDb.collection("parcels").doc("parcel_1").get());
  await assertFails(
    unauthDb
      .collection("parcels")
      .doc("parcel_1")
      .set(buildValidParcel(ALICE_UID, new Date()))
  );
});

test("2. Authenticated owner can create, get, and query own parcels with matching filter", async () => {
  const aliceCtx = testEnv.authenticatedContext(ALICE_UID);
  const aliceDb = aliceCtx.firestore();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("parcels").doc("alice_parcel_1").set(buildValidParcel(ALICE_UID, now))
  );
  await assertSucceeds(aliceDb.collection("parcels").doc("alice_parcel_1").get());
  await assertSucceeds(
    aliceDb.collection("parcels").where("userId", "==", ALICE_UID).get()
  );
});

test("3. Unfiltered list query on parcels fails (rules are not filters)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("parcels").get());
});

test("4. Cross-user access to another farmer's parcel is rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("parcels")
      .doc("bob_parcel_1")
      .set(buildValidParcel(BOB_UID, new Date(Date.now() - 1000)));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("parcels").doc("bob_parcel_1").get());
  await assertFails(aliceDb.collection("parcels").doc("bob_parcel_1").delete());
});

test("5. Shadow update with ghost field or invalid pH value poisoning fails", async () => {
  const now = new Date(Date.now() - 2000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("parcels")
      .doc("alice_parcel_1")
      .set(buildValidParcel(ALICE_UID, now));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("parcels").doc("alice_parcel_1").update({
      linkedToMarket: true,
      isVerifiedAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("parcels").doc("alice_parcel_1").update({
      soilPh: 99.5,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(
    aliceDb.collection("parcels").doc("alice_parcel_1").update({
      linkedToMarket: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("6. Market listing requires existing owned parcel and enforces terminal 'sold' state lock", async () => {
  const now = new Date(Date.now() - 2000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertFails(
    aliceDb.collection("market_listings").doc("listing_1").set({
      userId: ALICE_UID,
      parcelId: "missing_parcel",
      operatorName: "Coopérative El Baraka",
      parcelReference: "Ilot N 108 / P-24",
      cropType: "deglet_nour",
      wilaya: "07 - بسكرة (Biskra)",
      availableTons: 38.5,
      pricePerKgDzd: 420,
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("parcels").doc("alice_parcel_1").set(buildValidParcel(ALICE_UID, now))
  );

  await assertSucceeds(
    aliceDb.collection("market_listings").doc("listing_1").set({
      userId: ALICE_UID,
      parcelId: "alice_parcel_1",
      operatorName: "Coopérative El Baraka",
      parcelReference: "Ilot N 108 / P-24",
      cropType: "deglet_nour",
      wilaya: "07 - بسكرة (Biskra)",
      availableTons: 38.5,
      pricePerKgDzd: 420,
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("market_listings").where("status", "==", "active").get()
  );

  await assertSucceeds(
    aliceDb.collection("market_listings").doc("listing_1").update({
      status: "sold",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection("market_listings").doc("listing_1").update({
      pricePerKgDzd: 500,
      availableTons: 10,
      operatorName: "Coopérative El Baraka",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("7. Transport requests enforce user isolation and terminal 'delivered' lock", async () => {
  const now = new Date(Date.now() - 2000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("transport_requests").doc("tr_1").set({
      userId: ALICE_UID,
      parcelReference: "Ilot N 108 / P-24",
      vehicleType: "refrigerated_truck",
      originWilaya: "07 - بسكرة (Biskra)",
      destinationMarket: "سوق الجملة الكاليتوس - الجزائر",
      cargoTons: 18.5,
      scheduledDate: "2026-11-02",
      status: "pending",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(bobDb.collection("transport_requests").doc("tr_1").get());

  await assertSucceeds(
    aliceDb.collection("transport_requests").doc("tr_1").update({
      status: "delivered",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection("transport_requests").doc("tr_1").update({
      status: "in_transit",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("8. Service bookings enforce schema enums and owner isolation", async () => {
  const now = new Date(Date.now() - 2000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("service_bookings").doc("srv_1").set({
      userId: ALICE_UID,
      serviceCategory: "plowing_leveling",
      serviceTitle: "حرث وتسوية الأرض",
      parcelReference: "Ilot N 108 / P-24",
      wilaya: "07 - بسكرة (Biskra)",
      preferredDate: "2026-11-08",
      notes: "تسوية الأرض بالليزر قبل الغرس",
      status: "requested",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("service_bookings").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(bobDb.collection("service_bookings").doc("srv_1").get());
  await assertFails(
    aliceDb.collection("service_bookings").doc("srv_invalid").set({
      userId: ALICE_UID,
      serviceCategory: "unknown_category_xyz",
      serviceTitle: "خدمة غير معروفة",
      parcelReference: "Ilot N 108 / P-24",
      wilaya: "07 - بسكرة (Biskra)",
      preferredDate: "2026-11-08",
      notes: "",
      status: "requested",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("9. Multi-role user profile: supports resumable onboarding, multi-role selection, rejects duplicates, invalid roles, and unassigned defaultRole", async () => {
  const now = new Date(Date.now() - 2000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Resumable draft onboarding step 1 with 0 roles selected yet succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(buildUserAccount(ALICE_UID, [], "", false, 1, now))
  );

  // Reject completing onboarding with 0 roles
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      onboardingCompleted: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject duplicate role assignment ['farmer', 'farmer']
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      assignedRoles: ["farmer", "farmer"],
      defaultRole: "farmer",
      onboardingCompleted: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject invalid role code ['farmer', 'super_admin']
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      assignedRoles: ["farmer", "super_admin"],
      defaultRole: "farmer",
      onboardingCompleted: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject defaultRole not in assignedRoles
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      assignedRoles: ["farmer", "trader"],
      defaultRole: "transporter",
      onboardingCompleted: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Valid multi-role completion succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      assignedRoles: ["farmer", "trader", "storage_provider"],
      defaultRole: "farmer",
      onboardingStep: 5,
      onboardingCompleted: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Switching defaultRole to another assigned role succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      defaultRole: "trader",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Cross-user read/update of Alice's user account profile fails
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      defaultRole: "farmer",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("10. Role-specific profiles enforce ${uid}_${roleCode} key and cross-user isolation", async () => {
  const now = new Date(Date.now() - 2000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const validProfile = {
    userId: ALICE_UID,
    roleCode: "storage_provider",
    businessName: "Frigo Oasis Tolga",
    wilaya: "07 - بسكرة (Biskra)",
    commune: "طولقة",
    activityType: "Chambre Froide Positive +4°C",
    capacityOrArea: "450 طن",
    qualificationsOrDetails: "فرز وتوضيب تمور دقلة نور للتصدير",
    isProfileComplete: true,
    createdAt: now,
    updatedAt: now,
  };

  // Wrong document ID rejected
  await assertFails(
    aliceDb.collection("role_profiles").doc(`${ALICE_UID}_farmer`).set(validProfile)
  );

  // Matching document ID succeeds
  await assertSucceeds(
    aliceDb
      .collection("role_profiles")
      .doc(`${ALICE_UID}_storage_provider`)
      .set(validProfile)
  );

  // Cross-user read rejected
  await assertFails(
    bobDb.collection("role_profiles").doc(`${ALICE_UID}_storage_provider`).get()
  );
});

test("11. Server-side RBAC on partner_catalog blocks publishing under unassigned roles", async () => {
  const now = new Date(Date.now() - 2000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice only has ['farmer'] assigned
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(buildUserAccount(ALICE_UID, ["farmer"], "farmer", true, 5, now))
  );

  // Alice attempts to publish a supplier item without holding 'supplier' role -> REJECTED!
  await assertFails(
    aliceDb.collection("partner_catalog").doc("cat_unauthorized").set({
      userId: ALICE_UID,
      providerRole: "supplier",
      providerName: "Fake Supplier",
      title: "سماد NPK 15-15-15",
      category: "fertilizers_supply",
      wilaya: "07 - بسكرة (Biskra)",
      priceDzd: 8500,
      unitLabel: "د.ج / قنطار",
      availableQuantityOrCapacity: 120,
      description: "سماد مركب",
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice adds 'supplier' to her assignedRoles
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      assignedRoles: ["farmer", "supplier"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Now publishing supplier catalog item succeeds!
  await assertSucceeds(
    aliceDb.collection("partner_catalog").doc("cat_supplier_1").set({
      userId: ALICE_UID,
      providerRole: "supplier",
      providerName: "El Baraka Intrants",
      title: "سماد NPK 15-15-15 ذواب",
      category: "fertilizers_supply",
      wilaya: "07 - بسكرة (Biskra)",
      priceDzd: 8500,
      unitLabel: "د.ج / قنطار",
      availableQuantityOrCapacity: 120,
      description: "سماد معدني متوازن للنخيل والخضروات",
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob (any authenticated user) can browse active partner catalog items
  await assertSucceeds(
    bobDb.collection("partner_catalog").where("status", "==", "active").get()
  );
});

test("12. Marketplace orders & shared messages enforce role authorization, participant isolation, and terminal locks", async () => {
  const now = new Date(Date.now() - 2000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .set(buildUserAccount(ALICE_UID, ["trader"], "trader", true, 5, now))
  );
  await assertSucceeds(
    bobDb
      .collection("users")
      .doc(BOB_UID)
      .set(buildUserAccount(BOB_UID, ["transporter"], "transporter", true, 5, now))
  );
  await assertSucceeds(
    charlieDb
      .collection("users")
      .doc(CHARLIE_UID)
      .set(buildUserAccount(CHARLIE_UID, ["farmer"], "farmer", true, 5, now))
  );

  // Alice (trader) broadcasts an open transport request targeting 'transporter'
  await assertSucceeds(
    aliceDb.collection("marketplace_orders").doc("ord_1").set({
      requesterId: ALICE_UID,
      requesterName: "Société El Amel Gros",
      requesterRole: "trader",
      providerId: "",
      targetRole: "transporter",
      targetItemId: "",
      orderType: "transport_quote",
      itemTitle: "نقل 20 طن دقلة نور مبردة (بسكرة -> الجزائر)",
      wilaya: "07 - بسكرة (Biskra)",
      quantity: 20,
      unitLabel: "طن",
      proposedPriceDzd: 65000,
      notes: "درجة حرارة +4°C",
      status: "pending",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob (transporter) can query open pending orders for 'transporter' and submit a quote
  await assertSucceeds(
    bobDb
      .collection("marketplace_orders")
      .where("providerId", "==", "")
      .where("status", "==", "pending")
      .where("targetRole", "==", "transporter")
      .get()
  );

  // Charlie (farmer only, NOT transporter) cannot read or claim transporter open order
  await assertFails(charlieDb.collection("marketplace_orders").doc("ord_1").get());

  // Bob submits quotation & claims providerId
  await assertSucceeds(
    bobDb.collection("marketplace_orders").doc("ord_1").update({
      providerId: BOB_UID,
      proposedPriceDzd: 62000,
      notes: "شاحنة مبردة جاهزة غداً صباحاً",
      status: "quoted",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Alice completes the order (terminal state)
  await assertSucceeds(
    aliceDb.collection("marketplace_orders").doc("ord_1").update({
      status: "completed",
      notes: "تم التسليم بنجاح",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Terminal state lock prevents reopening completed order
  await assertFails(
    bobDb.collection("marketplace_orders").doc("ord_1").update({
      status: "accepted",
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Shared cross-role direct message from Alice to Bob
  await assertSucceeds(
    aliceDb.collection("messages").doc("msg_1").set({
      senderId: ALICE_UID,
      senderName: "Société El Amel Gros",
      senderRole: "trader",
      recipientId: BOB_UID,
      recipientName: "Bob Transport",
      subject: "بخصوص شحنة بسكرة ord_1",
      body: "شكراً على الالتزام بموعد الوصول إلى سوق الكاليتوس.",
      isRead: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Charlie (non-participant) cannot read Alice <-> Bob message
  await assertFails(charlieDb.collection("messages").doc("msg_1").get());

  // Bob (recipient) can read and mark message as read
  await assertSucceeds(bobDb.collection("messages").doc("msg_1").get());
  await assertSucceeds(
    bobDb.collection("messages").doc("msg_1").update({
      isRead: true,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("13. Fellah AI conversations & messages enforce strict ownership, relational integrity, immutability, and size bounds", async () => {
  const now = new Date(Date.now() - 2000);
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Unauthenticated caller cannot read or create AI conversations
  await assertFails(unauthDb.collection("ai_conversations").doc("conv_1").get());
  await assertFails(
    unauthDb.collection("ai_conversations").doc("conv_1").set({
      userId: ALICE_UID,
      title: "كيف أبيع محصولي؟",
      activeRole: "farmer",
      language: "AR",
      messageCount: 0,
      lastMessagePreview: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice creates her conversation
  await assertSucceeds(
    aliceDb.collection("ai_conversations").doc("conv_alice_1").set({
      userId: ALICE_UID,
      title: "كيف أبيع محصولي؟",
      activeRole: "farmer",
      language: "AR",
      messageCount: 0,
      lastMessagePreview: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Unfiltered list fails; filtered list succeeds
  await assertFails(aliceDb.collection("ai_conversations").get());
  await assertSucceeds(
    aliceDb.collection("ai_conversations").where("userId", "==", ALICE_UID).get()
  );

  // Orphaned message referencing non-existent conversation fails
  await assertFails(
    aliceDb.collection("ai_messages").doc("msg_orphan").set({
      userId: ALICE_UID,
      conversationId: "missing_conv_999",
      sender: "user",
      content: "كيف أبيع محصولي؟",
      activeRole: "farmer",
      verifiedDataSummary: "",
      hasVerifiedPlatformData: false,
      promptTokensEstimate: 12,
      completionTokensEstimate: 0,
      createdAt: now,
    })
  );

  // Valid user and assistant messages in owned conversation succeed
  await assertSucceeds(
    aliceDb.collection("ai_messages").doc("msg_u1").set({
      userId: ALICE_UID,
      conversationId: "conv_alice_1",
      sender: "user",
      content: "كيف أبيع محصولي؟",
      activeRole: "farmer",
      verifiedDataSummary: "",
      hasVerifiedPlatformData: false,
      promptTokensEstimate: 12,
      completionTokensEstimate: 0,
      createdAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("ai_messages").doc("msg_a1").set({
      userId: ALICE_UID,
      conversationId: "conv_alice_1",
      sender: "assistant",
      content: "يمكنك نشر محصولك من قسم تعيين القطعة أو السوق العام بعد ربط القطعة الفلاحية.",
      activeRole: "farmer",
      verifiedDataSummary: "2 إعلانات نشطة موثقة في بسكرة (420 د.ج/كغ)",
      hasVerifiedPlatformData: true,
      promptTokensEstimate: 45,
      completionTokensEstimate: 64,
      createdAt: now,
    })
  );

  // Messages are immutable (update rejected)
  await assertFails(
    aliceDb.collection("ai_messages").doc("msg_a1").update({
      content: "تم التعديل",
    })
  );

  // Cross-user read, message injection, and delete by Bob are rejected
  await assertFails(bobDb.collection("ai_conversations").doc("conv_alice_1").get());
  await assertFails(bobDb.collection("ai_conversations").doc("conv_alice_1").delete());
  await assertFails(bobDb.collection("ai_messages").doc("msg_a1").get());
  await assertFails(
    bobDb.collection("ai_messages").doc("msg_bob_hijack").set({
      userId: BOB_UID,
      conversationId: "conv_alice_1",
      sender: "user",
      content: "Attempt to inject into Alice conversation",
      activeRole: "trader",
      verifiedDataSummary: "",
      hasVerifiedPlatformData: false,
      promptTokensEstimate: 10,
      completionTokensEstimate: 0,
      createdAt: now,
    })
  );

  // Alice can query her conversation messages and delete her own conversation + messages
  await assertSucceeds(
    aliceDb
      .collection("ai_messages")
      .where("userId", "==", ALICE_UID)
      .where("conversationId", "==", "conv_alice_1")
      .get()
  );
  await assertSucceeds(aliceDb.collection("ai_messages").doc("msg_u1").delete());
  await assertSucceeds(aliceDb.collection("ai_messages").doc("msg_a1").delete());
  await assertSucceeds(aliceDb.collection("ai_conversations").doc("conv_alice_1").delete());
});

test("14. Fellah AI usage quotas enforce owner-only access, valid date format, and hard daily ceiling", async () => {
  const now = new Date(Date.now() - 1000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("ai_usage_quotas").doc(ALICE_UID).set({
      userId: ALICE_UID,
      dayKey: "2026-10-28",
      dailyRequestCount: 1,
      dailyTokensEstimate: 180,
      updatedAt: now,
    })
  );

  // Exceeding hard server ceiling (>100 requests/day) fails
  await assertFails(
    aliceDb.collection("ai_usage_quotas").doc(ALICE_UID).update({
      dailyRequestCount: 101,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Cross-user read and owner delete fail
  await assertFails(bobDb.collection("ai_usage_quotas").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("ai_usage_quotas").doc(ALICE_UID).delete());
});

test("15. Fellah Matin news_articles enforce editorial RBAC, approved-only public visibility, and strict HTTPS URL validation", async () => {
  const now = new Date(Date.now() - 2000);
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const adminUid = "editorial_admin_01";
  const adminDb = testEnv
    .authenticatedContext(adminUid, { email: "mehdi.hamdani.net@gmail.com" })
    .firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  const buildArticle = (overrides = {}) => ({
    authorId: adminUid,
    title: "انطلاق حملة الحرث والبذر للموسم الفلاحي",
    summary: "أعلنت المصالح الفلاحية عن توفير البذور المعتمدة والأسمدة الفوسفاتية عبر تعاونيات الحبوب والبقول الجافة.",
    sourceName: "وكالة الأنباء الجزائرية - اقتصاد",
    originalUrl: "https://www.aps.dz/economie/campagne-labour-semailles",
    publishedAtIso: "2026-10-28T07:30:00Z",
    retrievedAt: now,
    category: "gov_announcements",
    language: "AR",
    wilaya: "",
    imageUrl: "",
    verificationStatus: "verified_source",
    moderationStatus: "approved",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  });

  // Non-admin user cannot publish or import articles
  await assertFails(
    aliceDb.collection("news_articles").doc("art_unauth_create").set(
      buildArticle({ authorId: ALICE_UID })
    )
  );

  // Admin rejects http:// and javascript: URLs
  await assertFails(
    adminDb.collection("news_articles").doc("art_bad_http").set(
      buildArticle({ originalUrl: "http://insecure.example.com/news" })
    )
  );
  await assertFails(
    adminDb.collection("news_articles").doc("art_bad_js").set(
      buildArticle({ originalUrl: "javascript:alert(1)" })
    )
  );

  // Editorial admin creates an approved article AND a pending_review article AND a rejected article
  await assertSucceeds(
    adminDb.collection("news_articles").doc("art_approved_1").set(buildArticle())
  );
  await assertSucceeds(
    adminDb.collection("news_articles").doc("art_pending_1").set(
      buildArticle({
        title: "مسودة خبر قيد المراجعة التحريرية",
        originalUrl: "https://www.fao.org/newsroom/detail/pending-1",
        moderationStatus: "pending_review",
      })
    )
  );
  await assertSucceeds(
    adminDb.collection("news_articles").doc("art_rejected_1").set(
      buildArticle({
        title: "خبر مرفوض لعدم توثيق المصدر",
        originalUrl: "https://www.fao.org/newsroom/detail/rejected-1",
        moderationStatus: "rejected",
      })
    )
  );

  // Unauthenticated Firestore read is denied (visitors use Room public cache instead)
  await assertFails(unauthDb.collection("news_articles").doc("art_approved_1").get());

  // Signed-in non-admin user CAN read approved articles and list with moderationStatus == 'approved'
  await assertSucceeds(aliceDb.collection("news_articles").doc("art_approved_1").get());
  await assertSucceeds(
    aliceDb.collection("news_articles").where("moderationStatus", "==", "approved").get()
  );

  // Signed-in non-admin user CANNOT read pending_review or rejected articles, nor list unfiltered
  await assertFails(aliceDb.collection("news_articles").doc("art_pending_1").get());
  await assertFails(aliceDb.collection("news_articles").doc("art_rejected_1").get());
  await assertFails(aliceDb.collection("news_articles").get());

  // Non-admin cannot approve, edit, or delete articles
  await assertFails(
    aliceDb.collection("news_articles").doc("art_approved_1").update({
      summary: "تعديل غير مصرح به من مستخدم عادي",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(aliceDb.collection("news_articles").doc("art_approved_1").delete());

  // Editorial admin CAN approve pending article and read full editorial queue
  await assertSucceeds(adminDb.collection("news_articles").get());
  await assertSucceeds(
    adminDb.collection("news_articles").doc("art_pending_1").update({
      moderationStatus: "approved",
      verificationStatus: "editorial_verified",
      updatedAt: new Date(Date.now() - 300),
    })
  );
  await assertSucceeds(aliceDb.collection("news_articles").doc("art_pending_1").get());
});

test("16. Fellah Matin news_sources & editorial_admins enforce admin-only management and enabled-only visibility", async () => {
  const now = new Date(Date.now() - 2000);
  const delegatedAdminUid = "delegated_editor_02";
  const delegatedDb = testEnv.authenticatedContext(delegatedAdminUid).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Seed delegated editorial admin via server/security-disabled context (cannot be self-created by client)
  await assertFails(
    aliceDb.collection("editorial_admins").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      roleLabel: "editor",
      createdAt: now,
    })
  );

  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("editorial_admins").doc(delegatedAdminUid).set({
      userId: delegatedAdminUid,
      email: "editor@fellah-numerique.dz",
      roleLabel: "chief_editor",
      createdAt: now,
    });
  });

  // Delegated editor can read own editorial_admins record, while Alice cannot list or read it
  await assertSucceeds(delegatedDb.collection("editorial_admins").doc(delegatedAdminUid).get());
  await assertFails(aliceDb.collection("editorial_admins").doc(delegatedAdminUid).get());
  await assertFails(delegatedDb.collection("editorial_admins").get());

  // Delegated editor registers an enabled RSS source and a disabled broken source
  await assertSucceeds(
    delegatedDb.collection("news_sources").doc("src_fao_rss").set({
      name: "FAO Newsroom RSS",
      baseUrl: "https://www.fao.org",
      feedUrl: "https://www.fao.org/feeds/fao-newsroom-rss",
      sourceType: "official_institution",
      language: "FR",
      isEnabled: true,
      lastError: "",
      lastCheckedAt: now,
      updatedBy: delegatedAdminUid,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    delegatedDb.collection("news_sources").doc("src_disabled_rss").set({
      name: "Unreliable Feed",
      baseUrl: "https://example.org",
      feedUrl: "https://example.org/rss.xml",
      sourceType: "agri_publication",
      language: "AR",
      isEnabled: false,
      lastError: "HTTP 503 timeout during ingestion",
      lastCheckedAt: now,
      updatedBy: delegatedAdminUid,
      updatedAt: now,
    })
  );

  // Regular user can only read enabled sources
  await assertSucceeds(
    aliceDb.collection("news_sources").where("isEnabled", "==", true).get()
  );
  await assertFails(aliceDb.collection("news_sources").doc("src_disabled_rss").get());
  await assertFails(
    aliceDb.collection("news_sources").doc("src_fao_rss").update({
      isEnabled: false,
      updatedBy: ALICE_UID,
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Editorial admin can inspect all sources and disable unreliable sources
  await assertSucceeds(delegatedDb.collection("news_sources").get());
  await assertSucceeds(
    delegatedDb.collection("news_sources").doc("src_fao_rss").update({
      isEnabled: false,
      lastError: "Paused for maintenance",
      updatedBy: delegatedAdminUid,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("17. Fellah Matin user_news_preferences enforce owner isolation and valid category codes", async () => {
  const now = new Date(Date.now() - 1500);
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertFails(unauthDb.collection("user_news_preferences").doc(ALICE_UID).get());

  // Reject invalid news category code
  await assertFails(
    aliceDb.collection("user_news_preferences").doc(ALICE_UID).set({
      userId: ALICE_UID,
      preferredCategories: ["crop_production", "fake_crypto_news"],
      preferredRegion: "07 - بسكرة (Biskra)",
      preferredLanguage: "AR",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid preferences creation and update succeed for owner
  await assertSucceeds(
    aliceDb.collection("user_news_preferences").doc(ALICE_UID).set({
      userId: ALICE_UID,
      preferredCategories: ["crop_production", "weather_alerts", "inputs_and_machinery"],
      preferredRegion: "07 - بسكرة (Biskra)",
      preferredLanguage: "AR",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("user_news_preferences").doc(ALICE_UID).update({
      preferredCategories: ["market_availability", "cold_storage_postharvest"],
      preferredLanguage: "FR",
      updatedAt: new Date(Date.now() - 300),
    })
  );

  // Cross-user read & write rejected
  await assertFails(bobDb.collection("user_news_preferences").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("user_news_preferences").doc(ALICE_UID).update({
      preferredRegion: "16 - الجزائر (Alger)",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});

test("18. Registration National ID Card & optional Farmer Card PII fields enforce bounds and owner-only isolation", async () => {
  const now = new Date(Date.now() - 1500);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Owner creates account with scanned National ID Card and optional Farmer Card
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...buildUserAccount(ALICE_UID, ["farmer"], "farmer", true, 5, now),
      nationalIdScanned: true,
      nationalIdNumber: "109880712004560001",
      nationalIdHolderName: "أحمد بن صالح",
      farmerCardScanned: true,
      farmerCardNumber: "CAW-07-2025-8841",
      farmerCardWilaya: "07 - بسكرة (Biskra)",
    })
  );

  // Owner can read own PII and update optional Farmer Card if added later
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      farmerCardNumber: "CAW-07-2026-9910",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Cross-user read or tamper on Alice's scanned ID / Farmer Card PII is strictly rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      nationalIdNumber: "000000000",
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Oversized nationalIdNumber (> 40 chars) or wrong type rejected
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...buildUserAccount(BOB_UID, ["trader"], "trader", true, 5, now),
      nationalIdScanned: true,
      nationalIdNumber: "X".repeat(45),
    })
  );
});

test("19. User star ratings & points (/user_ratings) enforce anti-self-rating, 1..5 star bounds, pointsAwarded == stars * 20, and query enforcement", async () => {
  const now = new Date(Date.now() - 1500);
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const validRatingId = `${ALICE_UID}_${BOB_UID}`;
  const validPayload = {
    reviewerId: ALICE_UID,
    reviewerName: "أحمد الفلاح",
    reviewerRole: "farmer",
    targetUserId: BOB_UID,
    targetName: "شركة البركة للتجارة والتبريد",
    targetRole: "trader",
    stars: 5,
    pointsAwarded: 100,
    comment: "التزام ممتاز بالمواعيد وجودة التغليف والوزن الدقيق.",
    contextItemId: "order_biskra_01",
    status: "published",
    createdAt: now,
    updatedAt: now,
  };

  // Unauthenticated read/write rejected
  await assertFails(unauthDb.collection("user_ratings").doc(validRatingId).get());
  await assertFails(unauthDb.collection("user_ratings").doc(validRatingId).set(validPayload));

  // Self-rating is strictly rejected
  await assertFails(
    aliceDb.collection("user_ratings").doc(`${ALICE_UID}_${ALICE_UID}`).set({
      ...validPayload,
      targetUserId: ALICE_UID,
    })
  );

  // Out-of-bounds stars (6) or spoofed pointsAwarded (stars=2, pointsAwarded=100) rejected
  await assertFails(
    aliceDb.collection("user_ratings").doc(validRatingId).set({
      ...validPayload,
      stars: 6,
      pointsAwarded: 120,
    })
  );
  await assertFails(
    aliceDb.collection("user_ratings").doc(validRatingId).set({
      ...validPayload,
      stars: 2,
      pointsAwarded: 100,
    })
  );

  // Wrong document ID (not reviewerId_targetUserId) rejected
  await assertFails(
    aliceDb.collection("user_ratings").doc("random_rating_id").set(validPayload)
  );

  // Valid 5-star (100 points) rating creation succeeds
  await assertSucceeds(
    aliceDb.collection("user_ratings").doc(validRatingId).set(validPayload)
  );

  // Query enforcement: unfiltered list fails, filtered by status == 'published' succeeds for any signed-in user
  await assertFails(bobDb.collection("user_ratings").get());
  await assertSucceeds(
    bobDb.collection("user_ratings").where("status", "==", "published").get()
  );

  // Reviewer can update own rating (e.g., to 4 stars / 80 points), while target Bob cannot tamper or delete it
  await assertSucceeds(
    aliceDb.collection("user_ratings").doc(validRatingId).update({
      stars: 4,
      pointsAwarded: 80,
      comment: "خدمة جيدة جداً مع تأخر طفيف في الشحن.",
      updatedAt: new Date(Date.now() - 200),
    })
  );
  await assertFails(
    bobDb.collection("user_ratings").doc(validRatingId).update({
      stars: 5,
      pointsAwarded: 100,
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(bobDb.collection("user_ratings").doc(validRatingId).delete());
  await assertSucceeds(aliceDb.collection("user_ratings").doc(validRatingId).delete());
});

test("20. User favorites (/user_favorites) enforce strict owner-only isolation and composite ID integrity", async () => {
  const now = new Date(Date.now() - 1500);
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const favId = `${ALICE_UID}_operator_${BOB_UID}`;
  const validFavorite = {
    userId: ALICE_UID,
    favoriteType: "operator",
    targetId: BOB_UID,
    targetOwnerId: BOB_UID,
    targetTitle: "مؤسسة الواحة للنقل المبرد",
    targetSubtitle: "07 - بسكرة (Biskra) • ناقل فلاحي",
    targetRole: "transporter",
    createdAt: now,
    updatedAt: now,
  };

  // Unauthenticated & mismatched composite ID rejected
  await assertFails(unauthDb.collection("user_favorites").doc(favId).set(validFavorite));
  await assertFails(aliceDb.collection("user_favorites").doc("wrong_fav_id").set(validFavorite));

  // Invalid favoriteType rejected
  await assertFails(
    aliceDb.collection("user_favorites").doc(`${ALICE_UID}_unknown_${BOB_UID}`).set({
      ...validFavorite,
      favoriteType: "unknown",
    })
  );

  // Owner creates favorite, queries own favorites, and updates subtitle
  await assertSucceeds(aliceDb.collection("user_favorites").doc(favId).set(validFavorite));
  await assertSucceeds(
    aliceDb.collection("user_favorites").where("userId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("user_favorites").doc(favId).update({
      targetSubtitle: "07 - بسكرة • ★ 4.9 (98 نقطة)",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Unfiltered list and cross-user read/write/delete by Bob strictly rejected
  await assertFails(aliceDb.collection("user_favorites").get());
  await assertFails(bobDb.collection("user_favorites").doc(favId).get());
  await assertFails(
    bobDb.collection("user_favorites").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(bobDb.collection("user_favorites").doc(favId).delete());

  // Owner can remove favorite
  await assertSucceeds(aliceDb.collection("user_favorites").doc(favId).delete());
});

test("phone registration profile stores verified phoneNumber and allows post-OTP email completion", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  const phoneDraft = {
    ...buildUserAccount(ALICE_UID, [], "", false, 1, now),
    email: "",
    phoneNumber: "+213551234567",
    phoneVerified: true,
  };
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(phoneDraft));

  // Oversized phoneNumber (> 32 chars) rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      phoneNumber: "+" + "1".repeat(40),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Post-OTP completion with email, displayName, and role succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "مهدي فلاح البليدة",
      email: "mehdi.farmer@fellah.dz",
      assignedRoles: ["farmer"],
      defaultRole: "farmer",
      onboardingCompleted: true,
      onboardingStep: 4,
      updatedAt: new Date(Date.now() - 200),
    })
  );
});

test("parcels allow empty mainCrop and custom farmer product names while rejecting oversized strings", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 2000);

  // 1. Empty mainCrop ("") succeeds
  const blankCropParcel = {
    ...buildValidParcel(ALICE_UID, now),
    mainCrop: "",
  };
  await assertSucceeds(aliceDb.collection("parcels").doc("parcel_blank_crop").set(blankCropParcel));

  // 2. Updating to a custom farmer product succeeds
  await assertSucceeds(
    aliceDb.collection("parcels").doc("parcel_blank_crop").update({
      mainCrop: "بطيخ أحمر - المنيعة (منتوج خاص)",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // 3. Value poisoning (> 80 chars or non-string) rejected
  await assertFails(
    aliceDb.collection("parcels").doc("parcel_blank_crop").update({
      mainCrop: "م".repeat(85),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("parcels").doc("parcel_blank_crop").update({
      mainCrop: 12345,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // 4. Market listing accepts custom cropType (1..80 chars) and optional photoBase64
  await assertSucceeds(
    aliceDb.collection("market_listings").doc("listing_custom_crop").set({
      userId: ALICE_UID,
      parcelId: "parcel_blank_crop",
      operatorName: "مستثمرة المنيعة الحديثة",
      parcelReference: "Ilot N 108 / P-24",
      cropType: "بطيخ أحمر - المنيعة (منتوج خاص)",
      wilaya: "58 - المنيعة (El Meniaa)",
      availableTons: 24.0,
      pricePerKgDzd: 65,
      status: "active",
      photoBase64: "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    aliceDb.collection("market_listings").doc("listing_blank_crop").set({
      userId: ALICE_UID,
      parcelId: "parcel_blank_crop",
      operatorName: "مستثمرة المنيعة الحديثة",
      parcelReference: "Ilot N 108 / P-24",
      cropType: "",
      wilaya: "58 - المنيعة (El Meniaa)",
      availableTons: 24.0,
      pricePerKgDzd: 65,
      status: "active",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("21. Farmer service_quote lifecycle: farmer with parcel creates open request, provider reads & accepts, second provider cannot overwrite, and only assigned provider can complete", async () => {
  const now = new Date(Date.now() - 3000);
  const PROVIDER_1_UID = "provider_biskra_01";
  const PROVIDER_2_UID = "provider_biskra_02";
  const TRADER_ONLY_UID = "trader_outsider_04";

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb
      .collection("users")
      .doc(ALICE_UID)
      .set(buildUserAccount(ALICE_UID, ["farmer"], "farmer", true, 5, now));
    await adminDb
      .collection("parcels")
      .doc("alice_parcel_tolga")
      .set(buildValidParcel(ALICE_UID, now));
    await adminDb
      .collection("users")
      .doc(PROVIDER_1_UID)
      .set(buildUserAccount(PROVIDER_1_UID, ["service_provider"], "service_provider", true, 5, now));
    await adminDb
      .collection("users")
      .doc(PROVIDER_2_UID)
      .set(buildUserAccount(PROVIDER_2_UID, ["service_provider"], "service_provider", true, 5, now));
    await adminDb
      .collection("users")
      .doc(TRADER_ONLY_UID)
      .set(buildUserAccount(TRADER_ONLY_UID, ["trader"], "trader", true, 5, now));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const provider1Db = testEnv.authenticatedContext(PROVIDER_1_UID).firestore();
  const provider2Db = testEnv.authenticatedContext(PROVIDER_2_UID).firestore();
  const traderDb = testEnv.authenticatedContext(TRADER_ONLY_UID).firestore();

  const orderId = "srv_req_tolga_01";
  const validServiceQuote = {
    requesterId: ALICE_UID,
    requesterName: "أحمد الفلاح",
    requesterRole: "farmer",
    providerId: "",
    targetRole: "service_provider",
    targetItemId: "cat_tractor_07",
    orderType: "service_quote",
    itemTitle: "كراء الجرارات والآلات",
    wilaya: "07 - بسكرة (Biskra)",
    commune: "طولقة (Tolga Oasis)",
    parcelId: "alice_parcel_tolga",
    serviceCategory: "tractor_machinery_rental",
    preferredDate: "2026-11-05",
    centroidLat: 34.7201,
    centroidLng: 5.3802,
    quantity: 4.82,
    unitLabel: "هكتار",
    proposedPriceDzd: 6500,
    notes: "قطاع بسكرة - طولقة",
    status: "pending",
    createdAt: now,
    updatedAt: now,
  };

  // 1. Farmer with owned parcel creates open service_quote (providerId: "", targetRole: "service_provider")
  await assertSucceeds(
    aliceDb.collection("marketplace_orders").doc(orderId).set(validServiceQuote)
  );

  // Non-service_provider (trader) cannot list or get open service_quote requests
  await assertFails(traderDb.collection("marketplace_orders").doc(orderId).get());

  // 2. Service provider holding service_provider role reads open requests in their wilaya and accepts one
  await assertSucceeds(
    provider1Db
      .collection("marketplace_orders")
      .where("providerId", "==", "")
      .where("status", "==", "pending")
      .where("targetRole", "==", "service_provider")
      .where("wilaya", "==", "07 - بسكرة (Biskra)")
      .get()
  );

  // Unassigned provider cannot jump straight from pending to completed without accepting first
  await assertFails(
    provider1Db.collection("marketplace_orders").doc(orderId).update({
      providerId: PROVIDER_1_UID,
      status: "completed",
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Provider 1 accepts the request and adjusts proposedPriceDzd
  await assertSucceeds(
    provider1Db.collection("marketplace_orders").doc(orderId).update({
      providerId: PROVIDER_1_UID,
      status: "accepted",
      proposedPriceDzd: 6200,
      notes: "متوفر غداً صباحاً مع جرار رباعي الدفع",
      updatedAt: new Date(Date.now() - 1800),
    })
  );

  // 3. Another service_provider tries to accept or overwrite providerId on an accepted request -> MUST FAIL
  await assertFails(
    provider2Db.collection("marketplace_orders").doc(orderId).update({
      providerId: PROVIDER_2_UID,
      status: "accepted",
      proposedPriceDzd: 5900,
      updatedAt: new Date(Date.now() - 1500),
    })
  );

  // 4. Completion by a non-assigned provider (Provider 2) OR by the farmer requester -> MUST FAIL
  await assertFails(
    provider2Db.collection("marketplace_orders").doc(orderId).update({
      status: "completed",
      updatedAt: new Date(Date.now() - 1200),
    })
  );
  await assertFails(
    aliceDb.collection("marketplace_orders").doc(orderId).update({
      status: "completed",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // 5. Only the assigned provider (Provider 1) who accepted can mark the request "completed" -> SUCCEEDS
  await assertSucceeds(
    provider1Db.collection("marketplace_orders").doc(orderId).update({
      status: "completed",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("22. Farmer WITHOUT a registered parcel (or referencing another farmer's parcel) cannot create a service_quote request", async () => {
  const now = new Date(Date.now() - 2000);
  const LANDLESS_FARMER_UID = "farmer_no_parcel_09";

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb
      .collection("users")
      .doc(LANDLESS_FARMER_UID)
      .set(buildUserAccount(LANDLESS_FARMER_UID, ["farmer"], "farmer", true, 5, now));
    // Bob owns a parcel, but LANDLESS_FARMER_UID owns ZERO parcels
    await adminDb
      .collection("parcels")
      .doc("bob_owned_parcel")
      .set(buildValidParcel(BOB_UID, now));
  });

  const landlessDb = testEnv.authenticatedContext(LANDLESS_FARMER_UID).firestore();

  const baseOrder = {
    requesterId: LANDLESS_FARMER_UID,
    requesterName: "فلاح بدون قطعة مسجلة",
    requesterRole: "farmer",
    providerId: "",
    targetRole: "service_provider",
    targetItemId: "",
    orderType: "service_quote",
    itemTitle: "توفير العمالة",
    wilaya: "07 - بسكرة (Biskra)",
    quantity: 1,
    unitLabel: "يوم",
    proposedPriceDzd: 3500,
    notes: "",
    status: "pending",
    createdAt: now,
    updatedAt: now,
  };

  // Case A: Missing parcelId completely -> MUST FAIL
  await assertFails(
    landlessDb.collection("marketplace_orders").doc("srv_no_parcel_a").set(baseOrder)
  );

  // Case B: Non-existent parcelId -> MUST FAIL
  await assertFails(
    landlessDb.collection("marketplace_orders").doc("srv_no_parcel_b").set({
      ...baseOrder,
      parcelId: "non_existent_parcel_999",
    })
  );

  // Case C: Referencing another farmer's parcel (Bob's parcel) -> MUST FAIL
  await assertFails(
    landlessDb.collection("marketplace_orders").doc("srv_no_parcel_c").set({
      ...baseOrder,
      parcelId: "bob_owned_parcel",
    })
  );
});



