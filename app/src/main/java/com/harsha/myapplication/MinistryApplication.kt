package com.harsha.myapplication

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.harsha.myapplication.telemetry.CrashReporter

class MinistryApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Ensure Firebase is initialized (google-services plugin also auto-inits).
        FirebaseApp.initializeApp(this)

        // Crashlytics: enabled by default in release; keep collection on.
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // Every handled ("minor") error in the app is routed through this to Crashlytics non-fatals.
        CrashReporter.init(this)

        // Analytics is automatic once the dependency is present.
        FirebaseAnalytics.getInstance(this)

        // Remote Config: sensible defaults + fetch on startup.
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 // 1 hour in production
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.fetchAndActivate()
            .addOnFailureListener { CrashReporter.record("RemoteConfig", it, "fetchAndActivate failed") }

        // FCM: request token so the device is registered (token available via MessagingService).
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                // Token is ready when successful; a failure means push messages won't arrive.
                task.exception?.let { CrashReporter.record("FCM", it, "Could not get FCM token") }
            }
    }
}
