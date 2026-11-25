const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_1";
const BOB_UID = "bob_uid_2";
const EVE_UID = "eve_outsider_3";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

const past = () => new Date(Date.now() - 2000);
const future = () => new Date(Date.now() + 60000);

async function seedFamilyWithAlice(db, familyId = "fam_1", inviteCode = "FAM123") {
  const now = past();
  const batch = db.batch();
  batch.set(db.collection("families").doc(familyId), {
    name: "Familia García",
    inviteCode,
    ownerUid: ALICE_UID,
    memberUids: [ALICE_UID],
    createdAt: now,
    updatedAt: now,
  });
  batch.set(db.collection("inviteCodes").doc(inviteCode), {
    familyId,
    familyName: "Familia García",
    ownerUid: ALICE_UID,
    createdAt: now,
  });
  batch.set(db.collection("families").doc(familyId).collection("members").doc(ALICE_UID), {
    familyId,
    linkedUid: ALICE_UID,
    name: "Alice",
    role: "Adulto",
    avatarEmoji: "👩",
    colorHex: "#1B4D3E",
    memberUids: [ALICE_UID],
    createdAt: now,
    updatedAt: now,
  });
  batch.set(db.collection("users").doc(ALICE_UID), {
    uid: ALICE_UID,
    displayName: "Alice",
    email: "alice@example.com",
    familyId,
    avatarEmoji: "👩",
    colorHex: "#1B4D3E",
    createdAt: now,
    updatedAt: now,
  });
  await batch.commit();
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("families").doc("fam_1").get());
  await assertFails(unauthDb.collection("inviteCodes").doc("FAM123").get());
  await assertFails(
    unauthDb.collection("families").doc("fam_1").collection("tasks").where("memberUids", "array-contains", ALICE_UID).get()
  );
});

test("User profile PII isolation and immutable uid/createdAt", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = past();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      avatarEmoji: "👩",
      colorHex: "#1B4D3E",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob cannot read or write Alice's PII user profile
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hacked", updatedAt: past() }));

  // Alice cannot mutate createdAt or uid or inject future timestamps
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({ createdAt: past(), updatedAt: past() })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({ displayName: "Alice 2", updatedAt: future() })
  );
});

test("Invite code lookup works by exact ID, listing inviteCodes is forbidden, partner can join", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();

  await assertSucceeds(seedFamilyWithAlice(aliceDb));

  // Listing all inviteCodes is strictly denied
  await assertFails(bobDb.collection("inviteCodes").get());

  // Exact lookup by code succeeds for signed-in Bob
  await assertSucceeds(bobDb.collection("inviteCodes").doc("FAM123").get());

  // Bob cannot read families/fam_1 before joining
  await assertFails(bobDb.collection("families").doc("fam_1").get());

  // Bob cannot hijack ownerUid or tamper memberUids while joining
  await assertFails(
    bobDb.collection("families").doc("fam_1").update({
      memberUids: [BOB_UID], // removes Alice!
      updatedAt: past(),
    })
  );

  // Bob joins atomically: updates families/fam_1, creates members/BOB_UID, updates users/BOB_UID
  const now = past();
  const joinBatch = bobDb.batch();
  joinBatch.update(bobDb.collection("families").doc("fam_1"), {
    memberUids: [ALICE_UID, BOB_UID],
    updatedAt: now,
  });
  joinBatch.set(bobDb.collection("families").doc("fam_1").collection("members").doc(BOB_UID), {
    familyId: "fam_1",
    linkedUid: BOB_UID,
    name: "Bob",
    role: "Adulto",
    avatarEmoji: "👨",
    colorHex: "#C25934",
    memberUids: [ALICE_UID, BOB_UID],
    createdAt: now,
    updatedAt: now,
  });
  joinBatch.set(bobDb.collection("users").doc(BOB_UID), {
    uid: BOB_UID,
    displayName: "Bob",
    familyId: "fam_1",
    avatarEmoji: "👨",
    colorHex: "#C25934",
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(joinBatch.commit());

  // Now Bob can read families/fam_1, while outsider Eve still cannot
  await assertSucceeds(bobDb.collection("families").doc("fam_1").get());
  await assertFails(eveDb.collection("families").doc("fam_1").get());

  // Idempotent re-join (e.g., if Bob is already in memberUids and re-submits invite code)
  await assertSucceeds(
    bobDb.collection("families").doc("fam_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: past(),
    })
  );

  // Re-join updating existing members/BOB_UID doc with profile fields + memberUids
  await assertSucceeds(
    bobDb.collection("families").doc("fam_1").collection("members").doc(BOB_UID).update({
      name: "Bob Updated",
      avatarEmoji: "🧑",
      colorHex: "#2847D6",
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: past(),
    })
  );

  // Bob leaves family and later re-joins cleanly
  await assertSucceeds(
    bobDb.collection("families").doc("fam_1").update({
      memberUids: [ALICE_UID],
      updatedAt: past(),
    })
  );
  await assertSucceeds(
    bobDb.collection("families").doc("fam_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: past(),
    })
  );
  // Outsider Eve cannot use the idempotent branch or sneak into memberUids without adding only herself
  await assertFails(
    eveDb.collection("families").doc("fam_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: past(),
    })
  );
});

test("Empty and populated subcollection queries require memberUids array-contains filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  await seedFamilyWithAlice(aliceDb);

  // Filtered queries succeed even when subcollections are empty
  await assertSucceeds(
    aliceDb.collection("families").doc("fam_1").collection("shoppingItems").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("families").doc("fam_1").collection("tasks").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("families").doc("fam_1").collection("customLists").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("families").doc("fam_1").collection("customListItems").where("memberUids", "array-contains", ALICE_UID).get()
  );

  // Unfiltered list queries fail
  await assertFails(aliceDb.collection("families").doc("fam_1").collection("tasks").get());

  // Eve querying with her own UID fails to see Alice's docs or cannot create items in fam_1
  await assertFails(
    eveDb.collection("families").doc("fam_1").collection("tasks").doc("t1").set({
      familyId: "fam_1",
      title: "Hijack",
      description: "",
      assignedMemberName: "Eve",
      assignedMemberEmoji: "😈",
      dueDate: "Hoy",
      priority: "ALTA",
      completed: false,
      category: "Hogar",
      createdByUid: EVE_UID,
      memberUids: [EVE_UID],
      createdAt: past(),
      updatedAt: past(),
    })
  );
});

test("Shopping items, tasks, custom lists, calendar events, meal plans, recipes CRUD & validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await seedFamilyWithAlice(aliceDb);
  const now = past();

  // 1. Shopping item create & partial update (no @DocumentId `id` field!)
  const shopRef = aliceDb.collection("families").doc("fam_1").collection("shoppingItems").doc("item_1");
  await assertSucceeds(
    shopRef.set({
      familyId: "fam_1",
      name: "Leche entera",
      quantity: 2,
      unit: "L",
      supermarket: "Mercadona",
      priority: "URGENTE",
      hasOffer: true,
      offerDetails: "2x1",
      category: "Lácteos",
      checked: false,
      addedByMemberName: "Alice",
      assignedMemberName: "Alice",
      notes: "",
      createdByUid: ALICE_UID,
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(shopRef.update({ checked: true, updatedAt: past() }));
  await assertSucceeds(shopRef.update({ name: "Leche semidesnatada", quantity: 3, assignedMemberName: "Todos", updatedAt: past() }));
  // Reject ghost field
  await assertFails(shopRef.update({ ghost: true, updatedAt: past() }));

  // 2. Task create (with reminders) & edit & checkbox toggle
  const taskRef = aliceDb.collection("families").doc("fam_1").collection("tasks").doc("task_1");
  await assertSucceeds(
    taskRef.set({
      familyId: "fam_1",
      title: "Sacar basura",
      description: "Orgánica",
      assignedMemberName: "Todos",
      assignedMemberEmoji: "👨‍👩‍👧‍👦",
      dueDate: "Hoy",
      dueEpochMillis: Date.now(),
      dueTime: "20:30",
      reminderMinutesList: [60, 15, 0],
      priority: "ALTA",
      completed: false,
      category: "Hogar",
      createdByUid: ALICE_UID,
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(taskRef.update({ completed: true, updatedAt: past() }));
  await assertSucceeds(taskRef.update({ title: "Sacar basura y reciclaje", reminderMinutesList: [1440, 60], updatedAt: past() }));
  await assertFails(taskRef.update({ dueEpochMillis: -1, updatedAt: past() }));

  // 3. CustomList & CustomListItem
  const listRef = aliceDb.collection("families").doc("fam_1").collection("customLists").doc("list_1");
  await assertSucceeds(
    listRef.set({
      familyId: "fam_1",
      title: "Maleta fin de semana",
      emoji: "🧳",
      colorHex: "#1B4D3E",
      createdByUid: ALICE_UID,
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  const listItemRef = aliceDb.collection("families").doc("fam_1").collection("customListItems").doc("li_1");
  await assertSucceeds(
    listItemRef.set({
      familyId: "fam_1",
      listId: "list_1",
      title: "Cargador móvil",
      quantity: "2 ud",
      notes: "",
      assignedMemberName: "Todos",
      checked: false,
      createdByUid: ALICE_UID,
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(listItemRef.update({ checked: true, updatedAt: past() }));

  // 4. Calendar event (multi-day, allDay, multi-reminders), meal plan (2 weeks), recipe, categories, product history
  const evRef = aliceDb.collection("families").doc("fam_1").collection("calendarEvents").doc("ev_1");
  await assertSucceeds(
    evRef.set({
      familyId: "fam_1",
      title: "Escapada rural",
      description: "Revisión",
      location: "Sierra",
      dateEpochMillis: Date.now(),
      endDateEpochMillis: Date.now() + 2 * 86400000,
      extraDatesEpochMillis: [],
      allDay: true,
      startTime: "00:00",
      endTime: "23:59",
      assignedMemberName: "Todos",
      memberColorHex: "#1B4D3E",
      category: "Ocio familiar",
      reminderMinutesBefore: 1440,
      reminderMinutesList: [1440, 2880, 10080],
      notified: false,
      createdByUid: ALICE_UID,
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("families").doc("fam_1").collection("mealPlans").doc("slot_12_CENA").set({
      familyId: "fam_1",
      dayOfWeekIndex: 12,
      mealType: "CENA",
      dishName: "Salmón al horno",
      notes: "",
      createdByUid: ALICE_UID,
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("families").doc("fam_1").collection("recipes").doc("rec_1").set({
      familyId: "fam_1",
      title: "Tortilla de patatas",
      category: "Cenas Rápidas",
      prepTimeMinutes: 25,
      servings: 4,
      ingredients: "4 ud · Patatas\n5 ud · Huevos",
      instructions: "1. Freír.\n2. Cuajar.",
      emoji: "🍳",
      favorite: true,
      createdByUid: ALICE_UID,
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  const catRef = aliceDb.collection("families").doc("fam_1").collection("categories").doc("cat_1");
  await assertSucceeds(
    catRef.set({
      familyId: "fam_1",
      scope: "TASK",
      name: "Jardín",
      emoji: "🌱",
      colorHex: "#157A4B",
      createdByUid: ALICE_UID,
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(catRef.update({ name: "Terraza y jardín", updatedAt: past() }));
  await assertSucceeds(catRef.delete());

  const histRef = aliceDb.collection("families").doc("fam_1").collection("productHistory").doc("prod_leche_entera");
  await assertSucceeds(
    histRef.set({
      familyId: "fam_1",
      name: "Leche entera",
      nameLower: "leche entera",
      lastUnit: "L",
      lastCategory: "Lácteos",
      lastSupermarket: "Mercadona",
      useCount: 1,
      createdByUid: ALICE_UID,
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(histRef.update({ useCount: 2, updatedAt: past() }));

  // 5. Pet feeding tracker (Oliva)
  const petRef = aliceDb.collection("families").doc("fam_1").collection("petCare").doc("oliva_feeding");
  await assertSucceeds(
    petRef.set({
      familyId: "fam_1",
      petName: "Oliva",
      dateKey: "2026-11-24",
      fed: true,
      fedByMemberName: "Alice",
      fedTimeLabel: "09:15",
      createdByUid: ALICE_UID,
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    petRef.update({
      fed: false,
      fedByMemberName: "",
      fedTimeLabel: "",
      updatedAt: past(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("families").doc("fam_1").collection("petCare").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(petRef.update({ ghost: true, updatedAt: past() }));
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  await assertFails(
    eveDb.collection("families").doc("fam_1").collection("petCare").doc("oliva_feeding").update({
      fed: false,
      updatedAt: past(),
    })
  );

  // 6. MacroTargets & MacroIngredients CRUD, validation, and outsider denial
  const macroTargetRef = aliceDb.collection("families").doc("fam_1").collection("macroTargets").doc("default");
  await assertSucceeds(
    macroTargetRef.set({
      familyId: "fam_1",
      dailyCarbsG: 220,
      dailyProteinG: 140,
      dailyFatG: 65,
      breakfastCarbsG: 60,
      breakfastProteinG: 35,
      breakfastFatG: 18,
      lunchCarbsG: 90,
      lunchProteinG: 55,
      lunchFatG: 25,
      dinnerCarbsG: 70,
      dinnerProteinG: 50,
      dinnerFatG: 22,
      createdByUid: ALICE_UID,
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    macroTargetRef.update({
      lunchProteinG: 60,
      updatedAt: past(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("families").doc("fam_1").collection("macroTargets").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(macroTargetRef.update({ dailyCarbsG: -10, updatedAt: past() }));
  await assertFails(eveDb.collection("families").doc("fam_1").collection("macroTargets").doc("default").get());

  const macroIngRef = aliceDb.collection("families").doc("fam_1").collection("macroIngredients").doc("ing_salmon");
  await assertSucceeds(
    macroIngRef.set({
      familyId: "fam_1",
      name: "Salmón fresco",
      emoji: "🐟",
      carbsPer100g: 0,
      proteinPer100g: 20,
      fatPer100g: 13,
      createdByUid: ALICE_UID,
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    macroIngRef.update({
      fatPer100g: 12.5,
      updatedAt: past(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("families").doc("fam_1").collection("macroIngredients").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(macroIngRef.update({ proteinPer100g: 120, updatedAt: past() }));
  await assertFails(eveDb.collection("families").doc("fam_1").collection("macroIngredients").doc("ing_salmon").delete());
  await assertSucceeds(macroIngRef.delete());
});
