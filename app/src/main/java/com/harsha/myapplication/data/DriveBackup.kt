package com.harsha.myapplication.data

import com.harsha.myapplication.telemetry.CrashReporter
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Real Google Drive backup using the **appDataFolder** space.
 *
 * Files stored there are hidden from the user in the Drive UI and only this app
 * (with the drive.appdata scope) can list / read / delete them — no clutter in
 * the user's Drive.
 *
 * OAuth client (installed / Android):
 * 642904120818-q949sudkh4rtir1ac8meed2n05u92qa0.apps.googleusercontent.com
 */
object DriveBackup {

    /** Client ID from Google Cloud (Drive / OAuth). */
    const val CLIENT_ID =
        "642904120818-q949sudkh4rtir1ac8meed2n05u92qa0.apps.googleusercontent.com"

    /** Hidden app-only folder — recommended for private app backups. */
    private const val SCOPE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"

    /** Fixed name inside appDataFolder so we always overwrite the same snapshot. */
    private const val BACKUP_NAME = "ministry_assistant_backup.zip"

    private const val DRIVE_API = "https://www.googleapis.com/drive/v3"
    private const val DRIVE_UPLOAD = "https://www.googleapis.com/upload/drive/v3"

    fun signInClient(context: Context): GoogleSignInClient {
        // CLIENT_ID is registered in Google Cloud for this app (Drive API + OAuth).
        // Scope drive.appdata stores files only this app can see (hidden appDataFolder).
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(SCOPE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context.applicationContext, gso)
    }

    fun signInIntent(context: Context): Intent = signInClient(context).signInIntent

    fun lastSignedInAccount(context: Context): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context.applicationContext)

    fun hasAppDataScope(account: GoogleSignInAccount?): Boolean {
        if (account == null) return false
        return GoogleSignIn.hasPermissions(account, Scope(SCOPE_APPDATA))
    }

    fun signOut(context: Context) {
        CrashReporter.attempt("DriveBackup") { signInClient(context).signOut() }
    }

    private fun accessToken(context: Context, account: GoogleSignInAccount): String {
        val acct = account.account
            ?: throw IOException("No Google account on the signed-in session")
        // oauth2: + scope yields a short-lived access token for Drive REST
        return GoogleAuthUtil.getToken(context, acct, "oauth2:$SCOPE_APPDATA")
    }

    /**
     * Upload [file] into appDataFolder. Overwrites any previous file with [BACKUP_NAME].
     * Returns bytes uploaded.
     */
    fun uploadBackup(
        context: Context,
        file: File,
        onProgress: (Float) -> Unit = {},
    ): Long {
        val account = lastSignedInAccount(context)
            ?: throw IOException("Not signed in to Google")
        if (!hasAppDataScope(account)) {
            throw IOException("Drive app data permission is missing. Connect Google Drive again.")
        }
        val token = accessToken(context, account)
        val existingId = findBackupFileId(token)

        onProgress(0.05f)
        if (existingId != null) {
            putMedia(token, existingId, file, onProgress)
        } else {
            createMedia(token, file, onProgress)
        }
        onProgress(1f)
        return file.length()
    }

    /**
     * Download the app backup from appDataFolder into [dest].
     */
    fun downloadBackup(
        context: Context,
        dest: File,
        onProgress: (Float) -> Unit = {},
    ) {
        val account = lastSignedInAccount(context)
            ?: throw IOException("Not signed in to Google")
        if (!hasAppDataScope(account)) {
            throw IOException("Drive app data permission is missing. Connect Google Drive again.")
        }
        val token = accessToken(context, account)
        val id = findBackupFileId(token)
            ?: throw IOException("No backup found in Google Drive yet. Make a backup first.")
        onProgress(0.05f)
        getMedia(token, id, dest, onProgress)
        onProgress(1f)
    }

    // ── Drive REST helpers ──────────────────────────────────────────────────

    private fun findBackupFileId(token: String): String? {
        val q = "name = '$BACKUP_NAME' and 'appDataFolder' in parents and trashed = false"
        val encoded = URLEncoder.encode(q, StandardCharsets.UTF_8.name())
        val url = URL(
            "$DRIVE_API/files?spaces=appDataFolder&q=$encoded&fields=files(id,name)&pageSize=1"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 30_000
            readTimeout = 30_000
        }
        try {
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IOException("Drive list failed ($code): $body")
            }
            val files = JSONObject(body).optJSONArray("files") ?: return null
            if (files.length() == 0) return null
            return files.getJSONObject(0).getString("id")
        } finally {
            conn.disconnect()
        }
    }

    private fun createMedia(token: String, file: File, onProgress: (Float) -> Unit) {
        val boundary = "ministry_backup_" + System.currentTimeMillis()
        val meta = """{"name":"$BACKUP_NAME","parents":["appDataFolder"]}"""
        val metaPart = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(meta)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/zip\r\n\r\n")
        }.toByteArray(StandardCharsets.UTF_8)
        val end = "\r\n--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8)

        val url = URL("$DRIVE_UPLOAD/files?uploadType=multipart&fields=id,name,size")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            setRequestProperty("Content-Length", (metaPart.size + file.length() + end.size).toString())
            connectTimeout = 60_000
            readTimeout = 120_000
        }
        try {
            BufferedOutputStream(conn.outputStream).use { out ->
                out.write(metaPart)
                streamFile(file, out) { p -> onProgress(0.1f + 0.8f * p) }
                out.write(end)
                out.flush()
            }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IOException("Drive upload failed ($code): $body")
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun putMedia(
        token: String,
        fileId: String,
        file: File,
        onProgress: (Float) -> Unit,
    ) {
        val url = URL("$DRIVE_UPLOAD/files/$fileId?uploadType=media")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/zip")
            setRequestProperty("Content-Length", file.length().toString())
            connectTimeout = 60_000
            readTimeout = 120_000
        }
        try {
            BufferedOutputStream(conn.outputStream).use { out ->
                streamFile(file, out) { p -> onProgress(0.1f + 0.8f * p) }
                out.flush()
            }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                throw IOException("Drive update failed ($code): $body")
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun getMedia(
        token: String,
        fileId: String,
        dest: File,
        onProgress: (Float) -> Unit,
    ) {
        val url = URL("$DRIVE_API/files/$fileId?alt=media")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 30_000
            readTimeout = 120_000
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("Drive download failed ($code): $err")
            }
            val total = conn.contentLengthLong.coerceAtLeast(1L)
            dest.parentFile?.mkdirs()
            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(dest).use { output ->
                    val buf = ByteArray(64 * 1024)
                    var written = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n <= 0) break
                        output.write(buf, 0, n)
                        written += n
                        onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun streamFile(file: File, out: java.io.OutputStream, onProgress: (Float) -> Unit) {
        val total = file.length().coerceAtLeast(1L)
        FileInputStream(file).use { input ->
            val buf = ByteArray(64 * 1024)
            var written = 0L
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                out.write(buf, 0, n)
                written += n
                onProgress((written.toFloat() / total).coerceIn(0f, 1f))
            }
        }
    }
}
