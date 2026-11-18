package com.example.notifications

import android.util.Log
import com.example.data.remote.FirestoreUserProfileRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HirafiFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("HirafiFCM", "Refreshed FCM token received")
        if (Firebase.auth.currentUser == null || token.isBlank()) return
        serviceScope.launch {
            runCatching {
                val repo = FirestoreUserProfileRepository(applicationContext)
                repo.updateFcmToken(token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "تطبيق حرفي"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return
        val channel = message.data["channel"] ?: HirafiNotificationHelper.CHANNEL_ORDERS
        val notifId = (message.messageId?.hashCode() ?: System.currentTimeMillis().toInt())
        HirafiNotificationHelper.showNotification(
            context = applicationContext,
            channelId = channel,
            notificationId = notifId,
            title = title,
            body = body
        )
    }
}
