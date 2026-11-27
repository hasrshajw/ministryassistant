package com.harsha.myapplication.data

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.harsha.myapplication.telemetry.CrashReporter
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/** How long the app may stay in the background before it asks for the PIN again. */
enum class LockTimeout(val label: String, val millis: Long) {
    Immediately("Immediately", 0L),
    OneMinute("1 minute", 60_000L),
    FiveMinutes("5 minutes", 300_000L),
}

/** Result of checking a PIN or recovery PIN. */
sealed interface PinCheck {
    data object Ok : PinCheck
    data class Wrong(val attemptsLeftBeforeLockout: Int) : PinCheck
    data class LockedOut(val remainingMillis: Long) : PinCheck
}

/** Which of the two secrets is being checked. Each has its own counter and lockout schedule. */
enum class PinKind(val key: String, val freeAttempts: Int, val baseLockMs: Long) {
    Pin("pin", freeAttempts = 5, baseLockMs = 30_000L),
    Recovery("rec", freeAttempts = 3, baseLockMs = 900_000L),
}

/**
 * In-app PIN lock.
 *
 *  - Neither the PIN nor the recovery PIN is stored. Each is kept as a salted PBKDF2 hash, which is then
 *    encrypted with an Android Keystore key (so a copied prefs file is useless on another phone).
 *  - Wrong tries are counted and persisted; after [PinKind.freeAttempts] the wait doubles each time.
 *    The wait is measured with elapsedRealtime, so changing the phone clock does not skip it.
 *  - Lives in `app_lock_prefs`, which BackupRepository excludes, so a restore never resets or leaks it.
 *  - Compose state ([isLocked], [isEnabled]) so the lock overlay reacts instantly.
 */
object SecurityRepository {
    private const val PREFS = "app_lock_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_TIMEOUT = "timeout"
    private const val KEY_ALIAS = "ministry_lock_key"
    private const val ITERATIONS = 120_000
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 6

    var isEnabled by mutableStateOf(false)
        private set
    var isLocked by mutableStateOf(false)
        private set
    var timeout by mutableStateOf(LockTimeout.OneMinute)
        private set

    private var initialized = false
    private var backgroundedAt = 0L

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val p = prefs(context)
        isEnabled = p.getBoolean(KEY_ENABLED, false) && p.contains("pin_hash") && p.contains("rec_hash")
        timeout = LockTimeout.entries.firstOrNull { it.name == p.getString(KEY_TIMEOUT, null) }
            ?: LockTimeout.OneMinute
        isLocked = isEnabled // cold start always asks
    }

    // ───────────── Lifecycle (call from MainActivity.onStop / onStart) ─────────────

    fun onBackgrounded() {
        backgroundedAt = SystemClock.elapsedRealtime()
        if (isEnabled && timeout == LockTimeout.Immediately) isLocked = true
    }

    fun onForegrounded() {
        if (!isEnabled || isLocked || backgroundedAt == 0L) return
        if (SystemClock.elapsedRealtime() - backgroundedAt >= timeout.millis) isLocked = true
    }

    fun unlock() {
        isLocked = false
        backgroundedAt = 0L
    }

    // ───────────── Setup, change, turn off ─────────────

    /** Turns the lock on. The two PINs must differ and be [MIN_LENGTH]..[MAX_LENGTH] digits. */
    fun enable(context: Context, pin: String, recoveryPin: String): Boolean {
        if (!isValid(pin) || !isValid(recoveryPin) || pin == recoveryPin) return false
        return try {
            prefs(context).edit()
                .putBoolean(KEY_ENABLED, true)
                .also { storeSecret(it, PinKind.Pin, pin); storeSecret(it, PinKind.Recovery, recoveryPin) }
                .also { clearCounters(it) }
                .commit()
                .also { if (it) { isEnabled = true; isLocked = false } }
        } catch (e: Exception) {
            CrashReporter.record("SecurityRepository.enable", e)
            false
        }
    }

    fun changePin(context: Context, newPin: String): Boolean {
        if (!isEnabled || !isValid(newPin)) return false
        if (matches(context, PinKind.Recovery, newPin)) return false // must stay different
        return try {
            prefs(context).edit().also { storeSecret(it, PinKind.Pin, newPin); clearCounters(it) }.commit()
        } catch (e: Exception) {
            CrashReporter.record("SecurityRepository.changePin", e); false
        }
    }

    fun changeRecoveryPin(context: Context, newRecovery: String): Boolean {
        if (!isEnabled || !isValid(newRecovery)) return false
        if (matches(context, PinKind.Pin, newRecovery)) return false
        return try {
            prefs(context).edit().also { storeSecret(it, PinKind.Recovery, newRecovery); clearCounters(it) }.commit()
        } catch (e: Exception) {
            CrashReporter.record("SecurityRepository.changeRecoveryPin", e); false
        }
    }

    fun disable(context: Context) {
        prefs(context).edit().clear().commit()
        isEnabled = false
        isLocked = false
    }

    fun setTimeout(context: Context, value: LockTimeout) {
        prefs(context).edit().putString(KEY_TIMEOUT, value.name).apply()
        timeout = value
    }

    fun isValid(pin: String) = pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it in '0'..'9' }

    /** True for 0000, 1111, 1234, 4321 and similar: refused at setup. */
    fun isTooSimple(pin: String): Boolean {
        if (pin.toSet().size == 1) return true
        val d = pin.map { it - '0' }
        val step = d[1] - d[0]
        return (step == 1 || step == -1) && d.zipWithNext().all { (a, b) -> b - a == step }
    }

    // ───────────── Checking ─────────────

    /** Remaining lockout for [kind] in ms, or 0. */
    fun lockoutRemaining(context: Context, kind: PinKind): Long {
        val p = prefs(context)
        val until = p.getLong("${kind.key}_until", 0L)
        if (until == 0L) return 0L
        val dur = p.getLong("${kind.key}_dur", 0L)
        val at = p.getLong("${kind.key}_at", 0L)
        val now = SystemClock.elapsedRealtime()
        // elapsedRealtime restarts on reboot: then the stored deadline is meaningless, so wait the full time.
        val remaining = if (now < at) dur else until - now
        return remaining.coerceIn(0L, dur)
    }

    fun check(context: Context, kind: PinKind, entered: String): PinCheck {
        val wait = lockoutRemaining(context, kind)
        if (wait > 0) return PinCheck.LockedOut(wait)
        val p = prefs(context)
        if (matches(context, kind, entered)) {
            p.edit().putInt("${kind.key}_fails", 0).putLong("${kind.key}_until", 0L).apply()
            return PinCheck.Ok
        }
        val fails = p.getInt("${kind.key}_fails", 0) + 1
        val over = fails - kind.freeAttempts
        if (over >= 0) {
            val dur = (kind.baseLockMs shl over.coerceAtMost(6)).coerceAtMost(6L * 60 * 60 * 1000)
            val now = SystemClock.elapsedRealtime()
            p.edit().putInt("${kind.key}_fails", fails)
                .putLong("${kind.key}_dur", dur).putLong("${kind.key}_at", now).putLong("${kind.key}_until", now + dur)
                .apply()
            return PinCheck.LockedOut(dur)
        }
        p.edit().putInt("${kind.key}_fails", fails).apply()
        return PinCheck.Wrong(kind.freeAttempts - fails)
    }

    private fun clearCounters(e: android.content.SharedPreferences.Editor) {
        PinKind.entries.forEach { k ->
            e.putInt("${k.key}_fails", 0).putLong("${k.key}_until", 0L)
        }
    }

    // ───────────── Hashing + Keystore wrapping ─────────────

    private fun storeSecret(e: android.content.SharedPreferences.Editor, kind: PinKind, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val algo = if (Build.VERSION.SDK_INT >= 26) "PBKDF2WithHmacSHA256" else "PBKDF2WithHmacSHA1"
        val hash = derive(algo, pin, salt)
        e.putString("${kind.key}_salt", b64(salt))
        e.putString("${kind.key}_algo", algo)
        e.putString("${kind.key}_hash", b64(wrap(hash)))
    }

    private fun matches(context: Context, kind: PinKind, entered: String): Boolean = try {
        val p = prefs(context)
        val salt = unb64(p.getString("${kind.key}_salt", null) ?: return false)
        val algo = p.getString("${kind.key}_algo", null) ?: return false
        val stored = unwrap(unb64(p.getString("${kind.key}_hash", null) ?: return false))
        MessageDigest.isEqual(stored, derive(algo, entered, salt)) // constant-time compare
    } catch (e: Exception) {
        CrashReporter.record("SecurityRepository.matches", e)
        false
    }

    private fun derive(algo: String, pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance(algo)
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)).encoded

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    /** iv(12) + ciphertext. */
    private fun wrap(data: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        return c.iv + c.doFinal(data)
    }

    private fun unwrap(blob: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
            .apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, blob.copyOfRange(0, 12))) }
        return c.doFinal(blob, 12, blob.size - 12)
    }

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun unb64(s: String) = Base64.decode(s, Base64.NO_WRAP)
}
