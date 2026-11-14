const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-caixasma-136882da-ab5d-4a85-8be1-a74d3b4ca56b";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      databaseId: DATABASE_ID,
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

function getDb(context) {
  return context.firestore();
}

test("Unauthenticated user cannot list or write transactions, goals, or debts", async () => {
  const unauthDb = getDb(testEnv.unauthenticatedContext());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/transactions`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/goals`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/debts`).get());
  await assertFails(
    unauthDb.collection(`users/${ALICE_UID}/transactions`).doc("tx_1").set({
      userId: ALICE_UID,
      type: "income",
      amount: 100,
      description: "Salario",
      category: "Renda",
      dateMillis: Date.now(),
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

test("Authenticated user can create, list, update, and delete own transaction", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const ref = aliceDb.collection(`users/${ALICE_UID}/transactions`).doc("tx_1");
  await assertSucceeds(
    ref.set({
      userId: ALICE_UID,
      type: "income",
      amount: 3500.5,
      description: "Salario mensal",
      category: "Renda",
      dateMillis: 1763100000000,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/transactions`).get());
  await assertSucceeds(
    ref.update({
      amount: 3800,
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(ref.delete());
});

test("Authenticated user cannot read, write, or update another user's records", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection(`users/${BOB_UID}/transactions`).doc("tx_bob").set({
      userId: BOB_UID,
      type: "expense",
      amount: 200,
      description: "Mercado",
      category: "Essencial",
      dateMillis: 1763100000000,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    });
  });

  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  await assertFails(aliceDb.collection(`users/${BOB_UID}/transactions`).get());
  await assertFails(aliceDb.collection(`users/${BOB_UID}/transactions`).doc("tx_bob").get());
  await assertFails(
    aliceDb.collection(`users/${BOB_UID}/transactions`).doc("tx_bob").update({
      amount: 1,
      updatedAt: pastTimestamp(),
    })
  );
});

test("Rejects shadow ghost field and invalid amount on transaction", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/transactions`).doc("tx_bad_1").set({
      userId: ALICE_UID,
      type: "expense",
      amount: 100,
      description: "Teste",
      category: "Essencial",
      dateMillis: 1763100000000,
      ghostField: true,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/transactions`).doc("tx_bad_2").set({
      userId: ALICE_UID,
      type: "expense",
      amount: -50,
      description: "Negativo",
      category: "Essencial",
      dateMillis: 1763100000000,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

test("Authenticated user can manage own goals and debts, rejects paidAmount > totalAmount", async () => {
  const aliceDb = getDb(testEnv.authenticatedContext(ALICE_UID));
  const goalRef = aliceDb.collection(`users/${ALICE_UID}/goals`).doc("goal_1");
  await assertSucceeds(
    goalRef.set({
      userId: ALICE_UID,
      title: "Reserva de Emergencia",
      targetAmount: 10000,
      notes: "6 meses de custo",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  const debtRef = aliceDb.collection(`users/${ALICE_UID}/debts`).doc("debt_1");
  await assertSucceeds(
    debtRef.set({
      userId: ALICE_UID,
      title: "Cartao de Credito",
      creditor: "Banco",
      totalAmount: 2400,
      paidAmount: 400,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertFails(
    aliceDb.collection(`users/${ALICE_UID}/debts`).doc("debt_invalid").set({
      userId: ALICE_UID,
      title: "Invalida",
      totalAmount: 500,
      paidAmount: 600,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});
