package com.smartcontractai.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

object FCMUtils {
    private const val TAG = "FCMUtils"

    /**
     * Retrieve the current FCM token asynchronously.
     */
    fun fetchFcmToken(onTokenReceived: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "FCM Token: $token")
            onTokenReceived(token)
        }
    }
}

/**
     * Jetpack Compose helper to request notification permissions on Android 13 (Tiramisu)+
     */
@Composable
fun RequestNotificationPermissionIfNeeded(
    context: Context,
    onPermissionGranted: () -> Unit = {}
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    Log.d("FCMUtils", "Notification permission granted")
                    onPermissionGranted()
                } else {
                    Log.w("FCMUtils", "Notification permission denied")
                }
            }
        )

        LaunchedEffect(Unit) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onPermissionGranted()
            }
        }
    } else {
        LaunchedEffect(Unit) {
            onPermissionGranted()
        }
    }
}
