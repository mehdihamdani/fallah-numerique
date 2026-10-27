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
  // Ghost field attack
  await assertFails(
    aliceDb.collection("parcels").doc("alice_parcel_1").update({
      linkedToMarket: true,
      isVerifiedAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  // Value poisoning on soilPh out of bounds
  await assertFails(
    aliceDb.collection("parcels").doc("alice_parcel_1").update({
      soilPh: 99.5,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  // Valid market link toggle succeeds
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

  // Orphaned market listing fails when parcel does not exist
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

  // Create Alice's parcel first
  await assertSucceeds(
    aliceDb.collection("parcels").doc("alice_parcel_1").set(buildValidParcel(ALICE_UID, now))
  );

  // Now creating market listing succeeds
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

  // Bob can query active public market listings with status == 'active'
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("market_listings").where("status", "==", "active").get()
  );

  // Mark listing as sold (terminal state)
  await assertSucceeds(
    aliceDb.collection("market_listings").doc("listing_1").update({
      status: "sold",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Any subsequent update after 'sold' must fail (Terminal State Locking)
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

  // Cross-user read rejected
  await assertFails(bobDb.collection("transport_requests").doc("tr_1").get());

  // Advance status to delivered
  await assertSucceeds(
    aliceDb.collection("transport_requests").doc("tr_1").update({
      status: "delivered",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Subsequent update after terminal 'delivered' rejected
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
      serviceCategory: "drone_ndvi_scan",
      serviceTitle: "مسح طيفي بطائرة الدرون NDVI",
      parcelReference: "Ilot N 108 / P-24",
      wilaya: "07 - بسكرة (Biskra)",
      preferredDate: "2026-11-05",
      notes: "فحص حيوية نخيل دقلة نور",
      status: "requested",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("service_bookings").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(bobDb.collection("service_bookings").doc("srv_1").get());
});

