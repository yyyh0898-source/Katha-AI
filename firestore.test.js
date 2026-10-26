const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function validTimestamp() {
  return new Date(Date.now() - 1000);
}

function validCharacter(uid) {
  const ts = validTimestamp();
  return {
    userId: uid,
    name: "May Thu",
    tagline: "Yangon Rainy Night Poet",
    gender: "female",
    age: 24,
    ageGroup: "18_24",
    personality: "Warm, poetic, deeply empathetic, speaks natural Burmese.",
    backstory: "Runs a quiet vintage bookstore downtown.",
    storyScenario: "You step into her bookshop during a sudden monsoon downpour.",
    greetingMessage: "မိုးတွေအရမ်းရွာနေတယ်နော်... အထဲဝင်ပြီး ကော်ဖီပူပူလေး သောက်ပါဦးလား?",
    currentMood: "Romantic",
    isMature18Plus: false,
    avatarPreset: "velvet_poet",
    voicePitch: 1.05,
    voiceRate: 1.0,
    lastMessagePreview: "",
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated user cannot read or write user settings or characters", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("characters")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Authenticated owner can create and update valid UserSettings, while ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = validTimestamp();
  const validSettings = {
    userId: ALICE_UID,
    displayName: "Aung",
    languageCode: "my",
    themeMode: "dark",
    accentTheme: "velvet_rose",
    matureContentEnabled: true,
    biometricLockEnabled: false,
    autoVoiceEnabled: true,
    createdAt: ts,
    updatedAt: ts,
  };

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validSettings));

  // Shadow update with ghost field must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Aung Updated",
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Valid update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Aung Ko",
      themeMode: "light",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's characters", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("characters")
      .doc("char_1")
      .set(validCharacter(ALICE_UID))
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("characters").doc("char_1").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("characters")
      .where("userId", "==", ALICE_UID)
      .get()
  );
});

test("Character under 18 years old is strictly rejected by schema rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const underageChar = {
    ...validCharacter(ALICE_UID),
    age: 16,
  };
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("characters")
      .doc("char_underage")
      .set(underageChar)
  );
});

test("Client query alignment: character list requires userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("characters")
      .doc("char_1")
      .set(validCharacter(ALICE_UID))
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("characters")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("characters").get()
  );
});

test("Messages require existing parent character and enforce immutability on update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = validTimestamp();

  // Orphaned message without parent character must fail
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("characters")
      .doc("missing_char")
      .collection("messages")
      .doc("msg_1")
      .set({
        userId: ALICE_UID,
        characterId: "missing_char",
        sender: "user",
        text: "Hello",
        mood: "Romantic",
        imageType: "none",
        createdAt: ts,
      })
  );

  // Create parent character first
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("characters")
      .doc("char_1")
      .set(validCharacter(ALICE_UID))
  );

  // Valid message creation succeeds
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("characters")
      .doc("char_1")
      .collection("messages")
      .doc("msg_1")
      .set({
        userId: ALICE_UID,
        characterId: "char_1",
        sender: "user",
        text: "မင်္ဂလာပါ မေသူ",
        mood: "Romantic",
        imageType: "none",
        createdAt: ts,
      })
  );

  // Message update is forbidden
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("characters")
      .doc("char_1")
      .collection("messages")
      .doc("msg_1")
      .update({ text: "Tampered" })
  );
});
