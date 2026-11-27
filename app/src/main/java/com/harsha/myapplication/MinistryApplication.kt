package com.harsha.myapplication

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

class MinistryApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Ensure Firebase is initialized (google-services plugin also auto-inits).
        FirebaseApp.initializeApp(this)

        // Crashlytics: enabled by default in release; keep collection on.
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // Analytics is automatic once the dependency is present.
        FirebaseAnalytics.getInstance(this)

        // Remote Config: sensible defaults + fetch on startup.
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 // 1 hour in production
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate()

        // FCM: request token so the device is registered (token available via MessagingService).
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { /* token ready when successful */ }
    }
}
