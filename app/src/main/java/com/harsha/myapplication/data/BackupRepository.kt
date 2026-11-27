package com.harsha.myapplication.data

import android.content.Context
import android.content.SharedPreferences
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
    val keepLast: Int = 5,
    val driveConnected: Boolean = false,
    val driveAccount: String? = null,
    val lastBackupMillis: Long = 0L,
    val lastBackupBytes: Long = 0L,
) {
    /** "2 days ago", "5 minutes ago"… */
    fun lastBackupLabel(): String =
        if (lastBackupMillis == 0L) "Never"
        else DateUtils.getRelativeTimeSpanString(
            lastBackupMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
        ).toString()

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

/** One automatic / manual snapshot stored on this device. */
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
        val demo: Boolean,
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
 * MainActivity.onCreate. [prefs], [task] and [snapshots] are Compose state.
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
    private const val K_KEEP = "keep_last"
    private const val K_DRIVE = "drive_connected"
    private const val K_ACCOUNT = "drive_account"
    private const val K_LAST = "last_backup"
    private const val K_LAST_BYTES = "last_bytes"

    private var store: SharedPreferences? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null

    var prefs by mutableStateOf(BackupPrefs())
        private set
    var task by mutableStateOf<BackupTask>(BackupTask.Idle)
        private set
    var snapshots by mutableStateOf<List<BackupSnapshot>>(emptyList())
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
                ?.let { runCatching { BackupFrequency.valueOf(it) }.getOrNull() }
                ?: BackupFrequency.WEEKLY,
            wifiOnly = sp.getBoolean(K_WIFI, true),
            keepLast = sp.getInt(K_KEEP, 5),
            driveConnected = sp.getBoolean(K_DRIVE, false),
            driveAccount = sp.getString(K_ACCOUNT, null),
            lastBackupMillis = sp.getLong(K_LAST, 0L),
            lastBackupBytes = sp.getLong(K_LAST_BYTES, 0L),
        )
        refreshSnapshots(context)
    }

    // ── settings ──
    fun setAutoBackup(on: Boolean) = update { it.copy(autoBackup = on) }
    fun setFrequency(f: BackupFrequency) = update { it.copy(frequency = f) }
    fun setWifiOnly(on: Boolean) = update { it.copy(wifiOnly = on) }
    fun setKeepLast(n: Int) = update { it.copy(keepLast = n) }

    private fun update(transform: (BackupPrefs) -> BackupPrefs) {
        val next = transform(prefs)
        prefs = next
        store?.edit()
            ?.putBoolean(K_AUTO, next.autoBackup)
            ?.putString(K_FREQ, next.frequency.name)
            ?.putBoolean(K_WIFI, next.wifiOnly)
            ?.putInt(K_KEEP, next.keepLast)
            ?.putBoolean(K_DRIVE, next.driveConnected)
            ?.putString(K_ACCOUNT, next.driveAccount)
            ?.putLong(K_LAST, next.lastBackupMillis)
            ?.putLong(K_LAST_BYTES, next.lastBackupBytes)
            ?.apply()
    }

    private fun markBackedUp(bytes: Long) =
        update { it.copy(lastBackupMillis = System.currentTimeMillis(), lastBackupBytes = bytes) }

    // ── task control ──
    fun dismissTask() {
        if (task !is BackupTask.Running) task = BackupTask.Idle
    }

    fun cancel() {
        job?.cancel()
        task = BackupTask.Idle
    }

    private fun launchTask(
        kind: BackupKind,
        target: String,
        steps: List<String>,
        demo: Boolean = false,
        block: suspend (BackupReport) -> BackupTaskResult,
    ) {
        job?.cancel()
        job = scope.launch {
            task = BackupTask.Running(kind, target, steps, 0, null, demo)
            try {
                val result = block { i, p -> task = BackupTask.Running(kind, target, steps, i, p, demo) }
                task = BackupTask.Done(kind, result.message, result.needsRestart)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                task = BackupTask.Failed(kind, e.message ?: "Something went wrong")
            }
        }
    }

    // ───────────── Real: snapshots on this device ─────────────

    fun suggestedFileName(): String =
        "study-backup-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".zip"

    private fun snapshotDir(ctx: Context): File =
        File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "backups")

    fun refreshSnapshots(ctx: Context) {
        snapshots = snapshotDir(ctx.applicationContext)
            .listFiles { f -> f.extension == "zip" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { BackupSnapshot(it, it.lastModified(), it.length()) }
            ?: emptyList()
    }

    fun deleteSnapshot(ctx: Context, snapshot: BackupSnapshot) {
        snapshot.file.delete()
        refreshSnapshots(ctx)
    }

    /** Quick backup into the app's backup folder (no file picker). */
    fun backupNow(ctx: Context) {
        val app = ctx.applicationContext
        val file = File(snapshotDir(app).apply { mkdirs() }, suggestedFileName())
        launchTask(BackupKind.Backup, "this device", listOf("Collecting files", "Compressing", "Verifying")) { report ->
            withContext(Dispatchers.IO) {
                report(0, null)
                FileOutputStream(file).use { writeZip(app, it) { p -> report(1, p) } }
                report(2, null)
                verifyZip { file.inputStream() }
                markBackedUp(file.length())
                prune(app)
            }
            refreshSnapshots(app)
            BackupTaskResult("Backup saved on this device")
        }
    }

    private fun prune(app: Context) {
        snapshotDir(app).listFiles { f -> f.extension == "zip" }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(prefs.keepLast.coerceAtLeast(1))
            ?.forEach { it.delete() }
    }

    /** Call once at launch: runs a snapshot when auto backup is on and one is due. */
    fun runAutoBackupIfDue(ctx: Context) {
        val p = prefs
        if (!p.autoBackup || task is BackupTask.Running) return
        val due = p.lastBackupMillis == 0L ||
            System.currentTimeMillis() - p.lastBackupMillis >= p.frequency.days * BACKUP_DAY_MS
        if (due) backupNow(ctx)
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
                markBackedUp(size)
            }
            BackupTaskResult("Backup file saved")
        }
    }

    fun importFrom(ctx: Context, uri: Uri) {
        val app = ctx.applicationContext
        restore(app) { app.contentResolver.openInputStream(uri) ?: throw IOException("Couldn't open that file") }
    }

    fun restoreSnapshot(ctx: Context, snapshot: BackupSnapshot) {
        restore(ctx.applicationContext) { snapshot.file.inputStream() }
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
        name.startsWith("shared_prefs/backup_prefs") || name.startsWith("files/backups/")

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
    fun onDriveConnected(email: String?) {
        update { it.copy(driveConnected = true, driveAccount = email) }
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
        update { it.copy(driveConnected = false, driveAccount = null) }
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
                    markBackedUp(bytes)
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
                )
            }
        }
    }
}
