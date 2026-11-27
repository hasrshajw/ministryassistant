package com.harsha.myapplication.scripture

import com.harsha.myapplication.telemetry.CrashReporter
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads a Bible (.jwpub) from the online catalogue and imports it automatically.
 *
 * Everything runs in this object's OWN process-wide scope, not in a composable, so it keeps going
 * when the user leaves the Import screen. A foreground service ([BibleDownloadService]) keeps the
 * process alive while the app is in the recents screen / background, and a notification shows the
 * progress and the final "imported" message.
 *
 * The UI only reads [states] (Compose state) and [events] (one-shot messages for a snackbar).
 */
object BibleDownloads {

    private const val TAG = "BibleDownloads"
    private const val CHANNEL_ID = "bible_downloads"
    internal const val PROGRESS_NOTIF_ID = 7101
    private const val DONE_NOTIF_BASE = 7200

    /** One downloadable Bible. [language] is the name it is stored under in [BibleData]. */
    class Source(
        val id: String,
        val language: String,
        val nativeName: String?,
        val subtitle: String,
        val url: String,
    )

    val catalog: List<Source> = listOf(
        Source(
            id = "en",
            language = "English",
            nativeName = null,
            subtitle = "New World Translation",
            url = "https://jihtqbzrqymdkypicajz.supabase.co/storage/v1/object/public/jw%20test/nwt_E.jwpub",
        ),
        Source(
            id = "te",
            language = "Telugu",
            nativeName = "తెలుగు",
            subtitle = "New World Translation",
            url = "https://jihtqbzrqymdkypicajz.supabase.co/storage/v1/object/public/jw%20test/nwt_TU.jwpub",
        ),
    )

    sealed interface State {
        /** [progress] is 0..1, or a negative number when the server did not send a file size. */
        class Downloading(val progress: Float) : State
        object Importing : State
        class Failed(val message: String) : State
    }

    /** Source id -> state. A source with no entry is idle (or already imported). */
    val states = mutableStateMapOf<String, State>()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** One-shot messages ("English Bible imported", "Download failed: ...") for the visible screen. */
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO +
            CoroutineExceptionHandler { _, t -> CrashReporter.record(TAG, t, "Unhandled error in a Bible download") }
    )
    private val lock = Any()
    private val jobs = HashMap<String, Job>()

    fun hasActive(): Boolean = synchronized(lock) { jobs.isNotEmpty() }

    // ------------------------------------------------------------------ start

    fun start(context: Context, source: Source) {
        val app = context.applicationContext
        val job: Job
        synchronized(lock) {
            if (jobs.containsKey(source.id)) return
            states[source.id] = State.Downloading(0f)
            job = scope.launch(start = CoroutineStart.LAZY) { run(app, source) }
            jobs[source.id] = job
            startService(app)
        }
        ensureChannel(app)
        job.start()
    }

    private fun startService(app: Context) {
        try {
            ContextCompat.startForegroundService(app, Intent(app, BibleDownloadService::class.java))
        } catch (e: Exception) {
            // Download still runs (process scoped); it just is not protected while in the background.
            CrashReporter.record(TAG, e, "Could not start foreground service")
        }
    }

    private fun finish(app: Context, id: String) {
        synchronized(lock) {
            jobs.remove(id)
            // Never stopService() here: if the download fails instantly (no network) the service may not
            // have reached startForeground() yet, and stopping it first crashes the app with
            // "Context.startForegroundService() did not then call Service.startForeground()".
            if (jobs.isEmpty()) BibleDownloadService.requestStop()
        }
    }

    // ------------------------------------------------------------------ work

    private suspend fun run(app: Context, source: Source) {
        val file = File(app.cacheDir, "dl_${source.id}.jwpub")
        try {
            download(source.url, file) { p ->
                states[source.id] = State.Downloading(p)
                updateProgressNotification(app)
            }

            states[source.id] = State.Importing
            updateProgressNotification(app)

            val result = JwpubImporter.importFromUri(app, Uri.fromFile(file))
            if (result.success) {
                BibleData.loadVerses(result.verses, source.language)
                states.remove(source.id)
                _events.tryEmit("${source.language} Bible imported")
                notifyDone(app, source, "${source.language} Bible imported")
            } else {
                fail(app, source, result.message.ifBlank { "Import failed" })
            }
        } catch (e: CancellationException) {
            states.remove(source.id)
            throw e
        } catch (e: Throwable) {
            // Throwable, not Exception: OutOfMemoryError on a low-RAM phone must end as a failed
            // download with a message, never as a crash.
            CrashReporter.record(TAG, e, "Download failed for ${source.id}")
            val reason = if (e is OutOfMemoryError) {
                "Not enough free memory. Close other apps and try again."
            } else {
                e.message ?: "Download failed"
            }
            fail(app, source, reason)
        } finally {
            file.delete()
            finish(app, source.id)
        }
    }

    private fun fail(app: Context, source: Source, message: String) {
        states[source.id] = State.Failed(message)
        _events.tryEmit("${source.language} Bible: $message")
        notifyDone(app, source, "${source.language} Bible failed: $message")
    }

    private fun download(url: String, out: File, onProgress: (Float) -> Unit) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("Server returned $code")
            val total = conn.contentLengthLong
            if (total <= 0) onProgress(-1f)

            conn.inputStream.use { input ->
                FileOutputStream(out).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = 0L
                    var lastPercent = -1
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (total > 0) {
                            val percent = (read * 100 / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(read.toFloat() / total)
                            }
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    // ------------------------------------------------------------------ notifications

    internal fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Bible downloads", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun openAppIntent(app: Context): PendingIntent? {
        val launch = app.packageManager.getLaunchIntentForPackage(app.packageName) ?: return null
        return PendingIntent.getActivity(
            app, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /** The ongoing notification shown by the foreground service. */
    internal fun progressNotification(app: Context): Notification {
        ensureChannel(app)
        val active = catalog.mapNotNull { s -> states[s.id]?.let { s to it } }
            .filter { it.second !is State.Failed }

        val builder = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent(app))

        if (active.size == 1) {
            val (source, state) = active[0]
            builder.setContentTitle("${source.language} Bible")
            when (state) {
                is State.Downloading -> {
                    if (state.progress >= 0f) {
                        val pct = (state.progress * 100).toInt()
                        builder.setContentText("Downloading… $pct%").setProgress(100, pct, false)
                    } else {
                        builder.setContentText("Downloading…").setProgress(0, 0, true)
                    }
                }
                else -> builder.setContentText("Importing…").setProgress(0, 0, true)
            }
        } else {
            builder.setContentTitle("Downloading Bibles")
                .setContentText(if (active.isEmpty()) "Finishing up…" else active.joinToString { it.first.language })
                .setProgress(0, 0, true)
        }
        return builder.build()
    }

    @SuppressLint("MissingPermission")
    private fun updateProgressNotification(app: Context) {
        try {
            NotificationManagerCompat.from(app).notify(PROGRESS_NOTIF_ID, progressNotification(app))
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS not granted (user choice): the download is unaffected.
            CrashReporter.breadcrumb("BibleDownloads: progress notification not allowed")
        }
    }

    @SuppressLint("MissingPermission")
    private fun notifyDone(app: Context, source: Source, text: String) {
        try {
            ensureChannel(app)
            val n = NotificationCompat.Builder(app, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Bible")
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(openAppIntent(app))
                .build()
            NotificationManagerCompat.from(app).notify(DONE_NOTIF_BASE + catalog.indexOf(source), n)
        } catch (e: SecurityException) {
            // notifications not allowed (user choice)
            CrashReporter.breadcrumb("BibleDownloads: completion notification not allowed")
        }
    }
}
