package com.harsha.myapplication.data

import com.harsha.myapplication.telemetry.CrashReporter
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// ───────────────────────── Models ─────────────────────────

enum class BackupFrequency(val label: String, val days: Long) {
    DAILY("Daily", 1),
    WEEKLY("Weekly", 7),
    MONTHLY("Monthly", 30),
}

data class BackupPrefs(
    val autoBackup: Boolean = false,
    val frequency: BackupFrequency = BackupFrequency.WEEKLY,
    val wifiOnly: Boolean = true,
    val driveConnected: Boolean = false,
    val driveAccount: String? = null,
    val drivePhoto: String? = null,
    val lastBackupMillis: Long = 0L,
    val lastBackupBytes: Long = 0L,
    /** Where the last backup went: "Google Drive", "This device" or "A file". */
    val lastBackupTarget: String? = null,
) {
    /** "2 days ago", "5 minutes ago"… */
    fun lastBackupLabel(): String =
        if (lastBackupMillis == 0L) "Never"
        else DateUtils.getRelativeTimeSpanString(
            lastBackupMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        ).toString()

    /** Shown under the "Back up automatically" switch: every change is synced, not a timetable. */
    fun autoSyncLabel(): String {
        val where = if (driveConnected) "Google Drive" else "this device"
        return "Updates $where after every change · Last: ${lastBackupLabel()}"
    }

    fun nextBackupLabel(): String =
        if (lastBackupMillis == 0L) "Runs the next time you open the app"
        else SimpleDateFormat("EEE, d MMM", Locale.getDefault())
            .format(Date(lastBackupMillis + frequency.days * BACKUP_DAY_MS))
            .let { "Next backup: $it" }
}

enum class BackupKind(val title: String) {
    Backup("Backing up"),
    Restore("Restoring"),
}

/** The one backup file stored on this device. */
data class BackupSnapshot(val file: File, val time: Long, val bytes: Long)

sealed interface BackupTask {
    data object Idle : BackupTask

    /** [progress] == null means "indeterminate" (shows the expressive LoadingIndicator). */
    data class Running(
        val kind: BackupKind,
        val target: String,
        val steps: List<String>,
        val stepIndex: Int,
        val progress: Float?,
    ) : BackupTask

    data class Done(val kind: BackupKind, val message: String, val needsRestart: Boolean = false) : BackupTask
    data class Failed(val kind: BackupKind, val message: String) : BackupTask
}

private class BackupTaskResult(val message: String, val needsRestart: Boolean = false)
private typealias BackupReport = (stepIndex: Int, progress: Float?) -> Unit

private const val BACKUP_DAY_MS = 86_400_000L
private val BACKUP_DATA_DIRS = listOf("shared_prefs", "files", "databases")

// ───────────────────────── Repository ─────────────────────────

/**
 * Backup / restore state + logic. Same pattern as AppearanceRepository: call [init] once from
 * MainActivity.onCreate. [prefs], [task] and [localBackup] are Compose state.
 *
 * What a backup contains: every file in the app's private `shared_prefs`, `files` and `databases`
 * folders (notes, people, settings, progress…), zipped with a manifest. Restoring overwrites those
 * files, so the app must be restarted afterwards (the UI offers that).
 */
object BackupRepository {
    private const val FILE = "backup_prefs"
    private const val K_AUTO = "auto"
    private const val K_FREQ = "freq"
    private const val K_WIFI = "wifi_only"
    private const val K_PHOTO = "drive_photo"
    private const val K_DRIVE = "drive_connected"
    private const val K_ACCOUNT = "drive_account"
    private const val K_LAST = "last_backup"
    private const val K_LAST_BYTES = "last_bytes"
    private const val K_LAST_TARGET = "last_target"
    private const val K_PENDING = "pending_sync"

    /** Wait this long after the last change before syncing, so a burst of edits is one upload. */
    private const val SYNC_DEBOUNCE_MS = 8_000L
    private const val SYNC_RETRY_MS = 120_000L
    private const val SYNC_MAX_RETRIES = 3

    /**
     * Every preference file that holds user data or settings. A change to any of these (people,
     * visit logs, hours, folders, appearance, notification and home-layout settings…) triggers a
     * sync. Left out on purpose: `backup_prefs` (the sync's own state), `app_lock_prefs` (never
     * leaves the phone) and the alarm / message-pool bookkeeping files that change on their own.
     */
    private val WATCHED_PREFS = listOf(
        "FlutterSharedPreferences", // people + visit logs
        "hours_prefs",
        "note_folders",
        "setup_prefs",
        "appearance_prefs",
        "notification_prefs",
        "home_layout_prefs",
        "app_icon_prefs",
        "genz_prefs",
        "genz_profile_memory",
        "feedback_prefs",
    )

    private var store: SharedPreferences? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    // ── change-triggered sync state (all touched on the main thread only) ──
    private var appContext: Context? = null
    private var syncJob: Job? = null
    private var syncing = false
    private var changedWhileSyncing = false
    private var syncFailures = 0
    /** After a restore the files on disk are newer than the app's memory: never upload until restart. */
    private var restorePending = false
    /** SharedPreferences keeps listeners weakly, so this must stay referenced. */
    private val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> notifyDataChanged() }

    var prefs by mutableStateOf(BackupPrefs())
        private set
    var task by mutableStateOf<BackupTask>(BackupTask.Idle)
        private set
    /** The single on-device backup file (null when none exists yet). */
    var localBackup by mutableStateOf<BackupSnapshot?>(null)
        private set
    var driveConnecting by mutableStateOf(false)
        private set

    fun init(context: Context) {
        if (store != null) return
        val sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        store = sp
        prefs = BackupPrefs(
            autoBackup = sp.getBoolean(K_AUTO, false),
            frequency = sp.getString(K_FREQ, null)
                ?.let { CrashReporter.attempt("BackupRepository") { BackupFrequency.valueOf(it) }.getOrNull() }
                ?: BackupFrequency.WEEKLY,
            wifiOnly = sp.getBoolean(K_WIFI, true),
            driveConnected = sp.getBoolean(K_DRIVE, false),
            driveAccount = sp.getString(K_ACCOUNT, null),
            drivePhoto = sp.getString(K_PHOTO, null),
            lastBackupMillis = sp.getLong(K_LAST, 0L),
            lastBackupBytes = sp.getLong(K_LAST_BYTES, 0L),
            lastBackupTarget = sp.getString(K_LAST_TARGET, null),
        )
        refreshLocalBackup(context)

        val app = context.applicationContext
        appContext = app
        WATCHED_PREFS.forEach { name ->
            CrashReporter.attempt("BackupRepository") {
                app.getSharedPreferences(name, Context.MODE_PRIVATE)
                    .registerOnSharedPreferenceChangeListener(changeListener)
            }
        }
        watchNetwork(app)
    }

    // ── settings ──
    fun setAutoBackup(on: Boolean) {
        update { it.copy(autoBackup = on) }
        if (on) scheduleSync() // first sync right away, then after every change
    }
    fun setFrequency(f: BackupFrequency) = update { it.copy(frequency = f) }
    fun setWifiOnly(on: Boolean) = update { it.copy(wifiOnly = on) }

    private fun update(transform: (BackupPrefs) -> BackupPrefs) {
        val next = transform(prefs)
        prefs = next
        store?.edit()
            ?.putBoolean(K_AUTO, next.autoBackup)
            ?.putString(K_FREQ, next.frequency.name)
            ?.putBoolean(K_WIFI, next.wifiOnly)
            ?.putString(K_PHOTO, next.drivePhoto)
            ?.putBoolean(K_DRIVE, next.driveConnected)
            ?.putString(K_ACCOUNT, next.driveAccount)
            ?.putLong(K_LAST, next.lastBackupMillis)
            ?.putLong(K_LAST_BYTES, next.lastBackupBytes)
            ?.putString(K_LAST_TARGET, next.lastBackupTarget)
            ?.apply()
    }

    private fun markBackedUp(bytes: Long, target: String) =
        update {
            it.copy(lastBackupMillis = System.currentTimeMillis(), lastBackupBytes = bytes, lastBackupTarget = target)
        }

    // ── task control ──
    fun dismissTask() {
        if (task !is BackupTask.Running) task = BackupTask.Idle
    }

    fun cancel() {
        if ((task as? BackupTask.Running)?.kind == BackupKind.Restore) restorePending = false
        job?.cancel()
        task = BackupTask.Idle
    }

    private fun launchTask(
        kind: BackupKind,
        target: String,
        steps: List<String>,
        block: suspend (BackupReport) -> BackupTaskResult,
    ) {
        job?.cancel()
        if (kind == BackupKind.Restore) {
            // Stop auto-sync for good: the old in-memory data must not overwrite what we restore.
            restorePending = true
            syncJob?.cancel()
        }
        job = scope.launch {
            task = BackupTask.Running(kind, target, steps, 0, null)
            try {
                val result = block { i, p -> task = BackupTask.Running(kind, target, steps, i, p) }
                task = BackupTask.Done(kind, result.message, result.needsRestart)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CrashReporter.record("BackupRepository", e)
                if (kind == BackupKind.Restore) restorePending = false // nothing was replaced
                task = BackupTask.Failed(kind, e.message ?: "Something went wrong")
            }
        }
    }

    // ───────────── Automatic sync after every change ─────────────

    /**
     * Call whenever user data changes (notes, people, visit logs, hours, settings). Safe from any
     * thread. Does nothing unless "Back up automatically" is on. Preference files listed in
     * [WATCHED_PREFS] call this by themselves; the notes file calls it after each write.
     */
    fun notifyDataChanged() {
        scope.launch { scheduleSync() }
    }

    private fun scheduleSync() {
        if (!prefs.autoBackup || restorePending || store == null) return
        store?.edit()?.putBoolean(K_PENDING, true)?.apply()
        if (syncing) {
            changedWhileSyncing = true
            return
        }
        syncJob?.cancel()
        syncJob = scope.launch {
            delay(SYNC_DEBOUNCE_MS)
            syncNow()
        }
    }

    private suspend fun syncNow() {
        val app = appContext ?: return
        if (!prefs.autoBackup || restorePending) return
        if (task is BackupTask.Running) { // a manual backup/restore is in progress: try again shortly
            scheduleSync()
            return
        }
        // Wi-Fi only: stay pending; the network callback (or next launch) picks it up.
        if (prefs.driveConnected && prefs.wifiOnly && !onUnmeteredNetwork(app)) return

        syncing = true
        changedWhileSyncing = false
        var failed = false
        try {
            withContext(Dispatchers.IO) { silentBackup(app) }
            refreshLocalBackup(app)
            syncFailures = 0
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failed = true
            store?.edit()?.putBoolean(K_PENDING, true)?.apply() // keep it queued
            if (e !is IOException) CrashReporter.record("BackupRepository", e, "Auto sync failed")
        } finally {
            syncing = false
        }
        if (failed) {
            syncFailures++
            if (syncFailures <= SYNC_MAX_RETRIES) {
                syncJob = scope.launch { delay(SYNC_RETRY_MS); syncNow() }
            }
        } else if (changedWhileSyncing) {
            scheduleSync()
        }
    }

    /** Same data as a manual backup, but with no progress screen. Drive when connected, else the on-device file. */
    private suspend fun silentBackup(app: Context) {
        // Cleared before reading, so a change made during the upload sets it again.
        store?.edit()?.putBoolean(K_PENDING, false)?.apply()
        try {
            if (prefs.driveConnected) {
                val tmp = File(app.cacheDir, "auto-sync-${System.currentTimeMillis()}.zip")
                try {
                    FileOutputStream(tmp).use { writeZip(app, it) { } }
                    val bytes = DriveBackup.uploadBackup(app, tmp)
                    markBackedUp(bytes, "Google Drive")
                    clearLocalBackup(app)
                } finally {
                    tmp.delete()
                }
            } else {
                val dir = backupDir(app).apply { mkdirs() }
                val file = File(dir, LOCAL_NAME)
                val tmp = File(dir, "$LOCAL_NAME.tmp")
                try {
                    FileOutputStream(tmp).use { writeZip(app, it) { } }
                    verifyZip { tmp.inputStream() }
                    if (!tmp.renameTo(file)) tmp.copyTo(file, overwrite = true)
                    markBackedUp(file.length(), "This device")
                } finally {
                    tmp.delete()
                }
            }
        } catch (e: Exception) {
            store?.edit()?.putBoolean(K_PENDING, true)?.apply()
            throw e
        }
    }

    private fun onUnmeteredNetwork(app: Context): Boolean = try {
        val cm = app.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    } catch (e: SecurityException) {
        true // can't tell: don't block the sync
    }

    /** When a connection comes back (e.g. Wi-Fi), send whatever is still waiting. */
    private fun watchNetwork(app: Context) {
        CrashReporter.attempt("BackupRepository") {
            val cm = app.getSystemService(ConnectivityManager::class.java) ?: return@attempt
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    scope.launch {
                        if (store?.getBoolean(K_PENDING, false) == true && !syncing) {
                            syncFailures = 0
                            scheduleSync()
                        }
                    }
                }
            })
        }
    }

    // ───────────── Real: snapshots on this device ─────────────

    fun suggestedFileName(): String =
        "study-backup-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".zip"

    private const val LOCAL_NAME = "backup.zip"

    private fun backupDir(ctx: Context): File =
        File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "backups")

    private fun localFile(ctx: Context): File = File(backupDir(ctx), LOCAL_NAME)

    fun refreshLocalBackup(ctx: Context) {
        val f = localFile(ctx.applicationContext)
        localBackup = if (f.isFile) BackupSnapshot(f, f.lastModified(), f.length()) else null
    }

    /**
     * Backs up into ONE file on this device. The new zip is written to a temp file, verified,
     * and only then replaces the old backup, so a failed run never destroys the previous copy.
     */
    fun backupNow(ctx: Context) {
        val app = ctx.applicationContext
        val dir = backupDir(app).apply { mkdirs() }
        val file = File(dir, LOCAL_NAME)
        val tmp = File(dir, "$LOCAL_NAME.tmp")
        launchTask(BackupKind.Backup, "this device", listOf("Collecting files", "Compressing", "Verifying")) { report ->
            withContext(Dispatchers.IO) {
                report(0, null)
                try {
                    FileOutputStream(tmp).use { writeZip(app, it) { p -> report(1, p) } }
                    report(2, null)
                    verifyZip { tmp.inputStream() }
                    if (!tmp.renameTo(file)) tmp.copyTo(file, overwrite = true)
                    markBackedUp(file.length(), "This device")
                    // Clean up zips left over from the old multi-backup behaviour.
                    dir.listFiles { f -> f.extension == "zip" && f.name != LOCAL_NAME }?.forEach { it.delete() }
                } finally {
                    tmp.delete()
                }
            }
            refreshLocalBackup(app)
            BackupTaskResult("Backup saved on this device")
        }
    }

    /** The one-tap backup: Google Drive when connected, otherwise the single file on this device. */
    fun backup(ctx: Context) {
        if (prefs.driveConnected) backupToDrive(ctx) else backupNow(ctx)
    }

    /** Restores from the same place [backup] writes to. */
    fun restoreBackup(ctx: Context) {
        if (prefs.driveConnected) restoreFromDrive(ctx) else restoreLocalBackup(ctx)
    }

    private fun clearLocalBackup(app: Context) {
        backupDir(app).listFiles { f -> f.extension == "zip" || f.name.endsWith(".tmp") }?.forEach { it.delete() }
        refreshLocalBackup(app)
    }

    /** Call once at launch: runs a snapshot when auto backup is on and one is due. */
    fun runAutoBackupIfDue(ctx: Context) {
        val p = prefs
        if (!p.autoBackup || task is BackupTask.Running) return
        // A change made earlier that never reached Drive (offline, Wi-Fi only, app killed).
        if (store?.getBoolean(K_PENDING, false) == true) {
            scheduleSync()
            return
        }
        val due = p.lastBackupMillis == 0L ||
            System.currentTimeMillis() - p.lastBackupMillis >= p.frequency.days * BACKUP_DAY_MS
        if (due) backup(ctx)
    }

    // ───────────── Real: export / import a file ─────────────

    fun exportTo(ctx: Context, uri: Uri) {
        val app = ctx.applicationContext
        launchTask(BackupKind.Backup, "a file", listOf("Collecting files", "Compressing", "Verifying")) { report ->
            withContext(Dispatchers.IO) {
                report(0, null)
                val out = app.contentResolver.openOutputStream(uri, "w")
                    ?: throw IOException("Couldn't open the destination file")
                out.use { writeZip(app, it) { p -> report(1, p) } }
                report(2, null)
                verifyZip { app.contentResolver.openInputStream(uri) ?: throw IOException("Couldn't read the file back") }
                val size = app.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
                markBackedUp(size, "A file")
            }
            BackupTaskResult("Backup file saved")
        }
    }

    fun importFrom(ctx: Context, uri: Uri) {
        val app = ctx.applicationContext
        restore(app) { app.contentResolver.openInputStream(uri) ?: throw IOException("Couldn't open that file") }
    }

    fun restoreLocalBackup(ctx: Context) {
        val app = ctx.applicationContext
        restore(app) {
            val f = localFile(app)
            if (!f.isFile) throw IOException("No backup found on this device")
            f.inputStream()
        }
    }

    private fun restore(app: Context, open: () -> InputStream) {
        launchTask(BackupKind.Restore, "this device", listOf("Reading backup", "Checking contents", "Restoring data")) { report ->
            val restored = withContext(Dispatchers.IO) { restoreZip(app, open, report) }
            BackupTaskResult("Restored $restored files. Restart the app to finish.", needsRestart = true)
        }
    }

    // ───────────── Zip helpers ─────────────

    private fun dataRoot(ctx: Context): File = ctx.filesDir.parentFile ?: ctx.filesDir

    private fun isExcluded(name: String) =
        name.startsWith("shared_prefs/backup_prefs") || name.startsWith("files/backups/") ||
            name.startsWith("shared_prefs/app_lock_prefs") // PIN hashes never leave the phone

    private suspend fun writeZip(ctx: Context, out: OutputStream, onProgress: (Float) -> Unit) {
        val root = dataRoot(ctx)
        val files = BACKUP_DATA_DIRS.flatMap { dir ->
            File(root, dir).walkTopDown().filter { it.isFile }.mapNotNull { f ->
                val name = f.relativeTo(root).invariantSeparatorsPath
                if (isExcluded(name)) null else f to name
            }.toList()
        }
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(
                JSONObject()
                    .put("format", 1)
                    .put("package", ctx.packageName)
                    .put("createdAt", System.currentTimeMillis())
                    .put("files", files.size)
                    .toString().toByteArray()
            )
            zip.closeEntry()
            files.forEachIndexed { i, (file, name) ->
                currentCoroutineContext().ensureActive()
                zip.putNextEntry(ZipEntry(name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                onProgress((i + 1f) / files.size)
            }
        }
    }

    private suspend fun verifyZip(open: () -> InputStream) {
        var hasManifest = false
        ZipInputStream(BufferedInputStream(open())).use { z ->
            var e = z.nextEntry
            while (e != null) {
                currentCoroutineContext().ensureActive()
                if (e.name == "manifest.json") hasManifest = true
                z.closeEntry()
                e = z.nextEntry
            }
        }
        if (!hasManifest) throw IOException("Backup verification failed")
    }

    private suspend fun restoreZip(ctx: Context, open: () -> InputStream, report: BackupReport): Int {
        val root = dataRoot(ctx).canonicalFile
        report(0, null)

        // Pass 1: make sure this is one of our backups and count what is inside.
        var hasManifest = false
        var total = 0
        ZipInputStream(BufferedInputStream(open())).use { z ->
            var e = z.nextEntry
            while (e != null) {
                if (e.name == "manifest.json") hasManifest = true
                else if (!e.isDirectory) total++
                z.closeEntry()
                e = z.nextEntry
            }
        }
        report(1, null)
        if (!hasManifest) throw IOException("This isn't a valid backup file.")

        // Pass 2: write the files (zip-slip safe, temp file then rename).
        var done = 0
        ZipInputStream(BufferedInputStream(open())).use { z ->
            var e = z.nextEntry
            while (e != null) {
                currentCoroutineContext().ensureActive()
                val name = e.name
                if (!e.isDirectory && name != "manifest.json" &&
                    BACKUP_DATA_DIRS.any { name.startsWith("$it/") } && !isExcluded(name)
                ) {
                    val target = File(root, name).canonicalFile
                    if (!target.path.startsWith(root.path + File.separator)) {
                        throw IOException("Backup contains an unsafe path.")
                    }
                    target.parentFile?.mkdirs()
                    val tmp = File(target.path + ".restore")
                    FileOutputStream(tmp).use { z.copyTo(it) }
                    if (!tmp.renameTo(target)) {
                        tmp.copyTo(target, overwrite = true)
                        tmp.delete()
                    }
                    done++
                    report(2, (done.toFloat() / total.coerceAtLeast(1)).coerceAtMost(1f))
                }
                z.closeEntry()
                e = z.nextEntry
            }
        }
        return done
    }

    // ───────────── Google Drive (real — appDataFolder, only this app can see files) ─────────────

    /**
     * Call after Google Sign-In succeeds. [email] is shown in the UI; access is via the signed-in
     * account + Drive.appdata scope. Backups live in the hidden appDataFolder (not in the user's
     * visible Drive).
     */
    fun onDriveConnected(email: String?, photoUrl: String? = null) {
        update { it.copy(driveConnected = true, driveAccount = email, drivePhoto = photoUrl) }
        driveConnecting = false
    }

    fun onDriveConnectFailed(message: String? = null) {
        driveConnecting = false
        if (!message.isNullOrBlank()) {
            task = BackupTask.Failed(BackupKind.Backup, message)
        }
    }

    fun beginDriveConnect() {
        if (driveConnecting) return
        driveConnecting = true
    }

    fun disconnectDrive(ctx: Context) {
        DriveBackup.signOut(ctx.applicationContext)
        update { it.copy(driveConnected = false, driveAccount = null, drivePhoto = null) }
    }

    /** Upload a fresh zip into Drive appDataFolder (overwrites previous app backup). */
    fun backupToDrive(ctx: Context) {
        val app = ctx.applicationContext
        if (!prefs.driveConnected) {
            task = BackupTask.Failed(BackupKind.Backup, "Connect Google Drive first")
            return
        }
        launchTask(
            BackupKind.Backup,
            "Google Drive",
            listOf("Preparing data", "Compressing", "Uploading to Drive", "Verifying copy"),
        ) { report ->
            withContext(Dispatchers.IO) {
                report(0, null)
                val tmp = File(app.cacheDir, "drive-upload-${System.currentTimeMillis()}.zip")
                try {
                    FileOutputStream(tmp).use { writeZip(app, it) { p -> report(1, p) } }
                    report(2, null)
                    val bytes = DriveBackup.uploadBackup(app, tmp) { p -> report(2, p) }
                    report(3, 1f)
                    markBackedUp(bytes, "Google Drive")
                    clearLocalBackup(app) // Drive is now the only copy: no duplicate on the phone
                    BackupTaskResult("Backup saved to Google Drive (private app data)")
                } finally {
                    tmp.delete()
                }
            }
        }
    }

    /** Download the latest appDataFolder backup and restore it (requires app restart). */
    fun restoreFromDrive(ctx: Context) {
        val app = ctx.applicationContext
        if (!prefs.driveConnected) {
            task = BackupTask.Failed(BackupKind.Restore, "Connect Google Drive first")
            return
        }
        launchTask(
            BackupKind.Restore,
            "Google Drive",
            listOf("Downloading backup", "Verifying", "Restoring files"),
        ) { report ->
            withContext(Dispatchers.IO) {
                report(0, null)
                val tmp = File(app.cacheDir, "drive-restore-${System.currentTimeMillis()}.zip")
                try {
                    DriveBackup.downloadBackup(app, tmp) { p -> report(0, p) }
                    report(1, null)
                    verifyZip { tmp.inputStream() }
                    report(2, null)
                    val count = restoreZip(app, { tmp.inputStream() }, report)
                    BackupTaskResult("Restored $count files from Google Drive. Restart the app.", needsRestart = true)
                } finally {
                    tmp.delete()
                }
            }
        }
    }

    /** Re-check last signed-in account on cold start so UI stays in sync. */
    fun refreshDriveSession(ctx: Context) {
        val account = DriveBackup.lastSignedInAccount(ctx.applicationContext)
        if (account != null) {
            update {
                it.copy(
                    driveConnected = true,
                    driveAccount = account.email ?: it.driveAccount,
                    drivePhoto = account.photoUrl?.toString() ?: it.drivePhoto,
                )
            }
        }
    }
}
