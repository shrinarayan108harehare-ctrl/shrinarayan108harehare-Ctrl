package com.example.firebase

import android.util.Log
import com.example.R
import com.example.util.LocalNotifier
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PharmacyMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = Firebase.auth.currentUser?.uid ?: return
        runCatching {
            val dbId = applicationContext.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
                .collection("users")
                .document(uid)
                .update(
                    mapOf(
                        "fcmToken" to token,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
        }.onFailure {
            Log.w(TAG, "Deferred FCM token sync: ${it.message}")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        LocalNotifier.ensureChannels(applicationContext)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Prayag Deen & Sons"
        val body = message.notification?.body
            ?: message.data["message"]
            ?: message.data["body"]
            ?: ""
        val notifId = message.data["notificationId"] ?: "fcm_${System.currentTimeMillis()}"
        if (body.isNotBlank() || title.isNotBlank()) {
            LocalNotifier.showAnnouncementNotification(applicationContext, notifId, title, body)
        }
    }

    companion object {
        private const val TAG = "PharmacyFCM"
    }
}
