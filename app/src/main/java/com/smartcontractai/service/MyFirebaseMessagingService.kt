package com.smartcontractai.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smartcontractai.MainActivity
import com.smartcontractai.R

import com.smartcontractai.data.NotificationRepository

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Thông báo mới"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: "Bạn vừa nhận được thông báo mới từ hệ thống."

        // Đẩy thông báo từ Firebase vào Notification Feed thời gian thực và lưu vào Database
        NotificationRepository.addNotification(applicationContext, title, body)

        showNotification(title, body)
    }

    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d(TAG, "Refreshed token: $token")
        println("FCM TOKEN: $token")

        // Send token to server if app has backend
        sendRegistrationToServer(token)
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(
        title: String,
        body: String
    ) {

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            this,
            "default_channel"
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this)
            .notify(
                System.currentTimeMillis().toInt(),
                notification
            )
    }

    /**
     * There are three scenarios when `onRegistered` is called:
     * 1) Every time a manual `register()` call finishes successfully
     * 2) Whenever the FID is changed and the app is re-registered with FCM via the new FID.
     * 3) Automatically on app startup or routine sync when auto-initialization is enabled.
     */
    fun onRegistered(installationId: String) {
        Log.d(TAG, "Registered installation ID: $installationId")

        // Send the Firebase Installation ID to your app server.
        sendRegistrationToServer(installationId)
    }

    private fun sendTokenToServer(token: String) {
        // Implement network call to upload the FCM token to backend
    }

    private fun sendRegistrationToServer(installationId: String) {
        // Implement network call to upload the Firebase Installation ID to backend
    }

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "default_channel"
        private const val CHANNEL_NAME = "Default Notifications"
    }
}
