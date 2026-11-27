package com.harsha.myapplication.telemetry

import android.app.Activity
import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/**
 * Single entry point for sending every handled ("minor") error to Firebase.
 *
 * Fatal crashes and ANRs are already captured by Crashlytics itself. This covers everything the
 * app catches and carries on from: failed file reads/writes, bad parses, missing activities,
 * Drive/sign-in failures, and so on. Each report appears in the Firebase console under
 * Crashlytics -> Non-fatals, and as an `app_error` Analytics event for counting.
 *
 * Rules:
 *  - Reporting must NEVER throw or crash the app, so everything is wrapped.
 *  - Coroutine cancellation is control flow, not an error, so it is never reported.
 *  - Crashlytics keeps only a small number of non-fatals per session, so identical errors are
 *    de-duplicated within [DEDUPE_WINDOW_MS] (the next report carries a "repeated N times" note).
 */
object CrashReporter {

    private const val TAG = "CrashReporter"
    private const val DEDUPE_WINDOW_MS = 30_000L
    private const val MAX_TRACKED_KEYS = 200

    private class Seen(var lastSentAt: Long, var suppressed: Int)

    private val seen = ConcurrentHashMap<String, Seen>()

    @Volatile
    private var analytics: FirebaseAnalytics? = null

    /** Call once from [Application.onCreate], after Firebase is initialised. */
    fun init(app: Application) {
        try {
            analytics = FirebaseAnalytics.getInstance(app)
            val crashlytics = FirebaseCrashlytics.getInstance()
            val debuggable = (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            crashlytics.setCustomKey("build_type", if (debuggable) "debug" else "release")
            crashlytics.setCustomKey("sdk_int", Build.VERSION.SDK_INT)
            crashlytics.setCustomKey("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            registerLifecycleBreadcrumbs(app)
        } catch (t: Throwable) {
            Log.e(TAG, "CrashReporter init failed", t)
        }
    }

    /**
     * Report a handled exception.
     * @param tag     short area name, usually the class or file ("NotesRepository").
     * @param message optional human context ("Failed to save notes").
     */
    fun record(tag: String, t: Throwable, message: String? = null) {
        if (t is CancellationException) return
        try {
            Log.e(tag, message ?: t.message ?: t::class.java.simpleName, t)

            val key = "$tag|${t::class.java.name}|${t.stackTrace.firstOrNull()}"
            val now = System.currentTimeMillis()
            if (seen.size > MAX_TRACKED_KEYS) seen.clear()
            val entry = seen.getOrPut(key) { Seen(0L, 0) }
            var repeats = 0
            synchronized(entry) {
                if (entry.lastSentAt != 0L && now - entry.lastSentAt < DEDUPE_WINDOW_MS) {
                    entry.suppressed++
                    return
                }
                repeats = entry.suppressed
                entry.suppressed = 0
                entry.lastSentAt = now
            }

            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("error_tag", tag)
            val note = buildString {
                append('[').append(tag).append(']')
                if (message != null) append(' ').append(message)
                if (repeats > 0) append(" (repeated ").append(repeats).append(" more times)")
            }
            crashlytics.log(note)
            crashlytics.recordException(t)

            analytics?.logEvent("app_error", Bundle().apply {
                putString("error_tag", tag.take(36))
                putString("error_type", t::class.java.simpleName.take(36))
            })
        } catch (inner: Throwable) {
            // The reporter must never take the app down.
            Log.e(TAG, "Failed to report error from $tag", inner)
        }
    }

    /** Report a failure that has no exception object (e.g. "save() called before init()"). */
    fun recordMessage(tag: String, message: String) =
        record(tag, NonFatalIssue(message), message)

    /** Add a breadcrumb that is attached to any later report or crash. */
    fun breadcrumb(message: String) {
        try {
            FirebaseCrashlytics.getInstance().log(message)
        } catch (_: Throwable) {
        }
    }

    /** Remember which screen the user is on; shows up as a key on every report. */
    fun setScreen(name: String) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey("screen", name)
            FirebaseCrashlytics.getInstance().log("screen: $name")
        } catch (_: Throwable) {
        }
    }

    /**
     * Drop-in replacement for [runCatching] that reports the failure before returning it.
     * Cancellation is rethrown so coroutines still cancel properly.
     */
    inline fun <T> attempt(tag: String, block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            record(tag, e)
            Result.failure(e)
        }

    private fun registerLifecycleBreadcrumbs(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(a: Activity, s: Bundle?) =
                breadcrumb("${a::class.java.simpleName} created")

            override fun onActivityStarted(a: Activity) = Unit
            override fun onActivityResumed(a: Activity) =
                breadcrumb("${a::class.java.simpleName} resumed")

            override fun onActivityPaused(a: Activity) =
                breadcrumb("${a::class.java.simpleName} paused")

            override fun onActivityStopped(a: Activity) = Unit
            override fun onActivitySaveInstanceState(a: Activity, o: Bundle) = Unit
            override fun onActivityDestroyed(a: Activity) =
                breadcrumb("${a::class.java.simpleName} destroyed")
        })
    }

    /** Wrapper used when a problem is detected without an exception being thrown. */
    class NonFatalIssue(message: String) : RuntimeException(message)
}
