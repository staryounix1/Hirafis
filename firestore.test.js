const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_client_1";
const BOB_UID = "bob_craftsman_2";

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

test("Unauthenticated user cannot read or write user profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "Alice",
      email: "alice@example.com",
      city: "الدار البيضاء",
      role: "client",
      roleLocked: true,
      specialtyId: "plumbing",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner creates locked client profile and cannot switch role afterwards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fullName: "يونس العلمي",
      email: "youness@example.com",
      city: "الدار البيضاء",
      role: "client",
      roleLocked: true,
      specialtyId: "plumbing",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Updating city while preserving locked role succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      city: "الرباط",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Attempting to switch locked client role to craftsman fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "craftsman",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user access and invalid payloads are rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      fullName: "كمال السباك",
      email: "kamal@example.com",
      city: "الدار البيضاء",
      role: "craftsman",
      roleLocked: true,
      specialtyId: "plumbing",
      createdAt: new Date(Date.now() - 5000),
      updatedAt: new Date(Date.now() - 5000),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      id: ALICE_UID,
      userId: ALICE_UID,
      fullName: "A",
      email: "alice@example.com",
      city: "الدار البيضاء",
      role: "client",
      roleLocked: true,
      specialtyId: "plumbing",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
