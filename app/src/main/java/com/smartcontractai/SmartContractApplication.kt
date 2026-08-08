package com.smartcontractai

import android.app.Application
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger

class SmartContractApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Khởi tạo Facebook SDK và kích hoạt App Events Logger
        FacebookSdk.fullyInitialize()
        AppEventsLogger.activateApp(this)
    }
}
