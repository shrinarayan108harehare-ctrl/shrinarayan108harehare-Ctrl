const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const crypto = require("node:crypto");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CUSTOMER_A = "cust_alice";
const CUSTOMER_B = "cust_bob";
const MANAGER_FATHER_UID = "uid_father";
const OWNER_UID = "uid_owner";
const DELIVERY_UID = "uid_delivery_rahul";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function sha256(str) {
  return crypto.createHash("sha256").update(str).digest("hex");
}

async function seedRoles() {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = new Date(Date.now() - 5000);
    const fatherSalt = "salt_father_123";
    const fatherHash = sha256(`${fatherSalt}:passFather123`);
    const ownerSalt = "salt_owner_123";
    const ownerHash = sha256(`${ownerSalt}:1234`);
    const dpSalt = "salt_dp_123";
    const dpHash = sha256(`${dpSalt}:1234`);

    await db.collection("managers").doc("owner").set({
      name: "Owner",
      username: "owner",
      mobile: "9876543210",
      roleType: "owner",
      passwordHash: ownerHash,
      passwordSalt: ownerSalt,
      active: true,
      linkedUid: OWNER_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("managers").doc("father").set({
      name: "Father",
      username: "father",
      mobile: "9876543211",
      roleType: "family_manager",
      passwordHash: fatherHash,
      passwordSalt: fatherSalt,
      active: true,
      linkedUid: MANAGER_FATHER_UID,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("managerDirectory").doc("father").set({
      accountId: "father",
      username: "father",
      salt: fatherSalt,
      roleType: "family_manager",
      active: true,
    });
    await db.collection("deliveryPersonnel").doc("dp_rahul").set({
      name: "Rahul Kumar",
      mobile: "9111222333",
      username: "rahul_dp",
      passwordHash: dpHash,
      passwordSalt: dpSalt,
      active: true,
      linkedUid: DELIVERY_UID,
      onShift: true,
      trackingConsentGranted: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(OWNER_UID).set({
      userId: OWNER_UID,
      role: "owner",
      displayName: "Owner",
      linkedManagerId: "owner",
      active: true,
      language: "en",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(MANAGER_FATHER_UID).set({
      userId: MANAGER_FATHER_UID,
      role: "manager",
      displayName: "Father",
      linkedManagerId: "father",
      active: true,
      language: "en",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(DELIVERY_UID).set({
      userId: DELIVERY_UID,
      role: "delivery",
      displayName: "Rahul Kumar",
      linkedDeliveryPersonId: "dp_rahul",
      active: true,
      language: "en",
      createdAt: now,
      updatedAt: now,
    });
  });
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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated requests are rejected across all collections", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("orders").get());
  await assertFails(unauth.collection("orderFinancials").get());
  await assertFails(unauth.collection("auditLogs").get());
  await assertFails(unauth.collection("conversations").get());
});

test("Customer isolation: can create/read own profile, chat, and orders; cannot read other customers or financials", async () => {
  await seedRoles();
  const aliceDb = testEnv.authenticatedContext(CUSTOMER_A).firestore();
  const bobDb = testEnv.authenticatedContext(CUSTOMER_B).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(CUSTOMER_A).set({
      userId: CUSTOMER_A,
      role: "customer",
      displayName: "Alice",
      phone: "9999900001",
      active: true,
      language: "en",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer cannot self-assign manager role
  await assertFails(
    aliceDb.collection("users").doc(CUSTOMER_A).set({
      userId: CUSTOMER_A,
      role: "manager",
      displayName: "Alice Hacker",
      linkedManagerId: "father",
      active: true,
      language: "en",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc(CUSTOMER_A).set({
      customerId: CUSTOMER_A,
      customerName: "Alice",
      lastMessage: "Need Paracetamol 650",
      lastMessageType: "text",
      lastMessageAt: now,
      unreadByManager: 1,
      unreadByCustomer: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("conversations").doc(CUSTOMER_A).collection("messages").doc("m1").set({
      conversationId: CUSTOMER_A,
      customerId: CUSTOMER_A,
      senderUid: CUSTOMER_A,
      senderRole: "customer",
      senderName: "Alice",
      messageType: "text",
      text: "Need Paracetamol 650",
      deliveryStatus: "sent",
      createdAt: now,
    })
  );

  // Bob cannot read Alice's private conversation or messages
  await assertFails(bobDb.collection("conversations").doc(CUSTOMER_A).get());
  await assertFails(bobDb.collection("conversations").doc(CUSTOMER_A).collection("messages").get());

  // Alice creates order
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_1001").set({
      orderNumber: "PDS-1001",
      customerId: CUSTOMER_A,
      customerName: "Alice",
      customerPhone: "9999900001",
      deliveryAddress: "12 Main Bazar",
      deliveryArea: "Sadar Bazar",
      deliveryLat: 26.8467,
      deliveryLng: 80.9462,
      status: "NEW",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice filtered query works; unfiltered list fails
  await assertSucceeds(aliceDb.collection("orders").where("customerId", "==", CUSTOMER_A).get());
  await assertFails(aliceDb.collection("orders").get());
  await assertFails(aliceDb.collection("orderFinancials").get());
});

test("Manager & Delivery isolation: Delivery sees only assigned orders, never financials or chats; AuditLogs are append-only", async () => {
  await seedRoles();
  const fatherDb = testEnv.authenticatedContext(MANAGER_FATHER_UID).firestore();
  const ownerDb = testEnv.authenticatedContext(OWNER_UID).firestore();
  const dpDb = testEnv.authenticatedContext(DELIVERY_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("orders").doc("ord_2001").set({
      orderNumber: "PDS-2001",
      customerId: CUSTOMER_A,
      customerName: "Alice",
      customerPhone: "9999900001",
      deliveryAddress: "12 Main Bazar",
      deliveryArea: "Sadar Bazar",
      deliveryLat: 26.8467,
      deliveryLng: 80.9462,
      status: "ASSIGNED",
      assignedDeliveryPersonId: "dp_rahul",
      assignedDeliveryUid: DELIVERY_UID,
      assignedDeliveryPersonName: "Rahul Kumar",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("orders").doc("ord_2002").set({
      orderNumber: "PDS-2002",
      customerId: CUSTOMER_B,
      customerName: "Bob",
      customerPhone: "9999900002",
      deliveryAddress: "99 Station Rd",
      deliveryArea: "Civil Lines",
      deliveryLat: 26.85,
      deliveryLng: 80.95,
      status: "NEW",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("conversations").doc(CUSTOMER_A).set({
      customerId: CUSTOMER_A,
      customerName: "Alice",
      lastMessage: "Private Rx",
      lastMessageType: "text",
      lastMessageAt: now,
      createdAt: now,
      updatedAt: now,
    });
  });

  // Manager enters confidential financial record
  await assertSucceeds(
    fatherDb.collection("orderFinancials").doc("ord_2001").set({
      orderId: "ord_2001",
      orderNumber: "PDS-2001",
      customerId: CUSTOMER_A,
      customerName: "Alice",
      deliveryArea: "Sadar Bazar",
      orderStatus: "ASSIGNED",
      billValue: 500,
      discountAmount: 20,
      refundAmount: 0,
      netProductSales: 480,
      costValue: 320,
      productGrossProfit: 160,
      deliveryDistanceKm: 3.5,
      deliveryChargeCollected: 30,
      deliveryExpensePaid: 20,
      netBusinessProfit: 170,
      recordedByManagerId: "father",
      recordedByManagerName: "Father",
      orderDateKey: "2026-11-17",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Delivery Person CANNOT read financials or private chat or unassigned order
  await assertFails(dpDb.collection("orderFinancials").doc("ord_2001").get());
  await assertFails(dpDb.collection("orderFinancials").get());
  await assertFails(dpDb.collection("conversations").doc(CUSTOMER_A).get());
  await assertFails(dpDb.collection("orders").doc("ord_2002").get());

  // Delivery Person CAN read & update status of assigned order
  await assertSucceeds(dpDb.collection("orders").where("assignedDeliveryUid", "==", DELIVERY_UID).get());
  await assertSucceeds(
    dpDb.collection("orders").doc("ord_2001").update({
      status: "PICKED_UP",
      pickedUpAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Append-only Audit Log test
  const entryHash = sha256("GENESIS|father|ORDER_FINANCIAL_UPDATE|ord_2001");
  await assertSucceeds(
    fatherDb.collection("auditLogs").doc("log_1").set({
      managerId: "father",
      managerName: "Father",
      actorUid: MANAGER_FATHER_UID,
      actionPerformed: "ORDER_FINANCIAL_UPDATE",
      recordType: "orderFinancials",
      recordId: "ord_2001",
      fieldChanged: "costValue",
      previousValue: "0",
      newValue: "320",
      dateKey: "2026-11-17",
      timeDisplay: "10:45 AM",
      actionStatus: "SUCCESS",
      modificationReason: "Initial billing",
      prevHash: "GENESIS",
      entryHash,
      createdAt: now,
    })
  );

  // Nobody (not even Owner) can edit or delete auditLogs!
  await assertFails(fatherDb.collection("auditLogs").doc("log_1").update({ newValue: "0" }));
  await assertFails(ownerDb.collection("auditLogs").doc("log_1").update({ newValue: "0" }));
  await assertFails(ownerDb.collection("auditLogs").doc("log_1").delete());

  // Normal family manager cannot deactivate another manager (Owner only)
  await assertFails(
    fatherDb.collection("managers").doc("owner").update({
      active: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Owner CAN deactivate Father -> Father immediately loses access!
  await assertSucceeds(
    ownerDb.collection("managers").doc("father").update({
      active: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(fatherDb.collection("orderFinancials").get());
});
