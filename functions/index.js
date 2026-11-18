/**
 * Prayag Deen & Sons — Firebase Cloud Functions (Reference Deployment Artifact)
 *
 * Deploy manually via Firebase CLI (`firebase deploy --only functions`) after enabling
 * Firebase Blaze plan and FCM / Storage in Firebase Console.
 *
 * Handles:
 * 1. Push notification dispatch via FCM whenever a manager creates a document in `notifications`.
 * 2. Automatic retry tracking and invalid FCM token cleanup.
 * 3. Server-side cleanup / expiry checks for temporary live location flags after order completion.
 */

const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const DATABASE_ID = "ai-studio-android-prayagde-ec0b0bad-1acf-4ba2-a015-f5baaeff18da";
const db = getFirestore(DATABASE_ID);

exports.dispatchAnnouncementNotification = onDocumentCreated(
  {
    document: "notifications/{notificationId}",
    database: DATABASE_ID,
    region: "asia-south1",
  },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const notif = snap.data();
    const recipientUid = notif.recipientUid;
    if (!recipientUid) return;

    const userDoc = await db.collection("users").doc(recipientUid).get();
    const fcmToken = userDoc.exists ? userDoc.data().fcmToken : null;

    if (!fcmToken) {
      await snap.ref.update({
        status: "FAILED",
        failureReason: "Recipient has no registered FCM device token.",
        updatedAt: FieldValue.serverTimestamp(),
      });
      return;
    }

    try {
      await getMessaging().send({
        token: fcmToken,
        notification: {
          title: notif.title || "Prayag Deen & Sons",
          body: notif.message || "",
        },
        data: {
          notificationId: event.params.notificationId,
          announcementId: notif.announcementId || "",
          category: notif.category || "GENERAL",
        },
        android: {
          priority: "high",
          notification: { channelId: "prayag_deen_announcements" },
        },
      });
      await snap.ref.update({
        status: "SENT_TO_FCM",
        updatedAt: FieldValue.serverTimestamp(),
      });
    } catch (err) {
      const code = err && err.code ? String(err.code) : "UNKNOWN";
      await snap.ref.update({
        status: "FAILED",
        failureReason: code,
        retryCount: FieldValue.increment(1),
        updatedAt: FieldValue.serverTimestamp(),
      });
      if (
        code === "messaging/registration-token-not-registered" ||
        code === "messaging/invalid-registration-token"
      ) {
        await userDoc.ref.update({ fcmToken: "" });
      }
    }
  }
);

exports.clearLiveLocationOnOrderClose = onDocumentUpdated(
  {
    document: "orders/{orderId}",
    database: DATABASE_ID,
    region: "asia-south1",
  },
  async (event) => {
    const after = event.data?.after?.data();
    if (!after) return;
    if ((after.status === "DELIVERED" || after.status === "CANCELLED") && after.liveLocationShared === true) {
      await event.data.after.ref.update({
        liveLocationShared: false,
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
  }
);
