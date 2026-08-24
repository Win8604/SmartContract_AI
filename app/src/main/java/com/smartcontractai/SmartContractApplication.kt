package com.smartcontractai

import android.app.Application
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger

class SmartContractApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            val clientToken = getString(R.string.facebook_client_token)
            if (clientToken.isNotBlank() && clientToken != "YOUR_FACEBOOK_CLIENT_TOKEN") {
                FacebookSdk.fullyInitialize()
                AppEventsLogger.activateApp(this)
            } else {
                Log.w("SmartContractApp", "Facebook client token not set. Skipping Facebook SDK auto-logging.")
            }
        } catch (e: Exception) {
            Log.e("SmartContractApp", "Error initializing Facebook SDK", e)
        }
    }
}

