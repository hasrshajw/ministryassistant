package com.harsha.myapplication.scripture

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.harsha.myapplication.telemetry.CrashReporter

/**
 * Keeps the process alive (and shows the ongoing notification) while a Bible is downloading or
 * importing, including when the user leaves the screen or swipes the app into the recents list.
 * The actual work lives in [BibleDownloads]; this service only holds the foreground state.
 * [BibleDownloads] asks it to stop (see [requestStop]) when the last download finishes.
 *
 * Crash-proofing (these were real crashes on some devices):
 *  - startForeground() can throw (app already in the background on Android 12+, missing foreground
 *    service permission, ...). That used to escape from onStartCommand and kill the app; now the
 *    service just stops and the download carries on unprotected.
 *  - The service is never stopped from outside before it has called startForeground(); doing so
 *    crashes with "did not then call Service.startForeground()".
 *  - Android 15 limits dataSync services to ~6 hours and calls [onTimeout]; the service must stop.
 */
class BibleDownloadService : Service() {

    @Volatile private var lastStartId = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else 0

        try {
            ServiceCompat.startForeground(
                this,
                BibleDownloads.PROGRESS_NOTIF_ID,
                BibleDownloads.progressNotification(this),
                type,
            )
        } catch (e: Throwable) {
            CrashReporter.record("BibleDownloadService", e, "startForeground failed")
            stopSelf()
            return START_NOT_STICKY
        }
        if (!BibleDownloads.hasActive()) stopSelf()
        return START_NOT_STICKY
    }

    /** Android 15+: the system's time limit for data-sync foreground services was reached. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        stopSelf()
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile private var instance: BibleDownloadService? = null

        /**
         * Safe to call at any time, from any thread. If the service has not started yet it does
         * nothing: onStartCommand sees there is no active download and stops itself right after
         * calling startForeground(). Stopping by start id means a newer start is never cancelled.
         */
        fun requestStop() {
            val service = instance ?: return
            try {
                service.stopSelf(service.lastStartId)
            } catch (e: Throwable) {
                CrashReporter.record("BibleDownloadService", e, "stopSelf failed")
            }
        }
    }
}
