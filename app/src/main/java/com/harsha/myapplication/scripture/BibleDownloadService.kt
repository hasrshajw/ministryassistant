package com.harsha.myapplication.scripture

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat

/**
 * Keeps the process alive (and shows the ongoing notification) while a Bible is downloading or
 * importing, including when the user leaves the screen or swipes the app into the recents list.
 * The actual work lives in [BibleDownloads]; this service only holds the foreground state.
 * [BibleDownloads] stops it when the last download finishes.
 */
class BibleDownloadService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else 0

        ServiceCompat.startForeground(
            this,
            BibleDownloads.PROGRESS_NOTIF_ID,
            BibleDownloads.progressNotification(this),
            type,
        )
        if (!BibleDownloads.hasActive()) stopSelf()
        return START_NOT_STICKY
    }
}
