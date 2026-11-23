package com.harsha.myapplication.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.LruCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

/**
 * Private, app-internal storage for note images.
 *
 * Picked images are down-scaled (longest edge <= [MAX_EDGE]), rotated according to EXIF and
 * re-encoded (JPEG, or PNG when the picture has transparency), so notes never depend on a URI
 * permission that can disappear later.
 *
 * The storage folder is created lazily, so importing works even if [init] was not called yet.
 */
object NoteImages {

    private const val DIR_NAME = "note_images"
    private const val MAX_EDGE = 1600

    @Volatile
    private var dir: File? = null

    /** Small in-memory cache so scrolling / re-opening a note does not re-decode files. */
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun init(context: Context) {
        ensureDir(context)
    }

    private fun ensureDir(context: Context): File {
        dir?.let { if (it.exists() || it.mkdirs()) return it }
        return synchronized(this) {
            val d = File(context.applicationContext.filesDir, DIR_NAME)
            d.mkdirs()
            dir = d
            d
        }
    }

    /** Copies [uri] into app storage. Returns the stored file name, or null on failure. */
    suspend fun import(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val target = ensureDir(app)
        var outFile: File? = null
        try {
            val resolver = app.contentResolver

            // 1) Read the size only
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val longest = max(bounds.outWidth, bounds.outHeight)
            if (longest <= 0) return@withContext null

            // 2) Decode a reduced copy (power-of-two sample keeps memory low)
            var sample = 1
            while (longest / (sample * 2) >= MAX_EDGE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return@withContext null

            // 3) Exact scale to MAX_EDGE + EXIF rotation in a single pass
            val decodedLongest = max(decoded.width, decoded.height)
            val scale = if (decodedLongest > MAX_EDGE) MAX_EDGE.toFloat() / decodedLongest else 1f
            val rotation = readRotation(app, uri)
            val bitmap = if (scale != 1f || rotation != 0) {
                val matrix = Matrix().apply {
                    if (scale != 1f) postScale(scale, scale)
                    if (rotation != 0) postRotate(rotation.toFloat())
                }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                    .also { if (it !== decoded) decoded.recycle() }
            } else decoded

            // 4) Encode
            val name = encodeToFile(target, bitmap).also { outFile = File(target, it) }
            bitmap.recycle()
            if (File(target, name).length() == 0L) {
                File(target, name).delete()
                return@withContext null
            }
            name
        } catch (e: CancellationException) {
            outFile?.delete()
            throw e
        } catch (_: Throwable) { // includes OutOfMemoryError on huge pictures
            outFile?.delete()
            null
        }
    }

    private fun encodeToFile(target: File, bitmap: Bitmap): String {
        val png = bitmap.hasAlpha()
        val name = "img_${UUID.randomUUID()}." + if (png) "png" else "jpg"
        FileOutputStream(File(target, name)).use {
            if (png) bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            else bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it)
        }
        return name
    }

    /** Width / height of a stored image (cheap: reads only the file header when not cached). */
    suspend fun aspectRatio(name: String): Float? = withContext(Dispatchers.IO) {
        cache.get(name)?.let { return@withContext it.width.toFloat() / it.height }
        val file = resolve(name) ?: return@withContext null
        if (!file.exists()) return@withContext null
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            BitmapFactory.decodeFile(file.path, o)
        } catch (_: Throwable) {
            return@withContext null
        }
        if (o.outWidth > 0 && o.outHeight > 0) o.outWidth.toFloat() / o.outHeight else null
    }

    /** Saves a rotated copy and returns its new file name (the original is left untouched). */
    suspend fun rotate(name: String, degrees: Int = 90): String? = withContext(Dispatchers.IO) {
        val target = dir ?: return@withContext null
        try {
            val src = decode(name) ?: return@withContext null
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            val rotated = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
            val newName = encodeToFile(target, rotated)
            if (rotated !== src) rotated.recycle()
            newName
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
    }

    /** Cached bitmap if present (no disk access), so images never flash a spinner on re-composition. */
    fun peek(name: String): Bitmap? = cache.get(name)

    /** Cached, off-main-thread load for display. */
    suspend fun load(name: String): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(name) ?: decode(name)?.also { cache.put(name, it) }
    }

    /** Decodes a stored image (already <= [MAX_EDGE], so safe to load directly). */
    fun decode(name: String): Bitmap? {
        val file = resolve(name) ?: return null
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.path)
        } catch (_: Throwable) {
            null
        }
    }

    fun delete(name: String) {
        cache.remove(name)
        resolve(name)?.delete()
    }

    /**
     * Removes image files that no note references any more (called once at start-up).
     * Files newer than [olderThan] are kept, so an image imported while this runs is never lost.
     */
    fun deleteUnreferenced(referenced: Set<String>, olderThan: Long = Long.MAX_VALUE) {
        dir?.listFiles()?.forEach {
            if (it.name !in referenced && it.lastModified() < olderThan) {
                cache.remove(it.name)
                it.delete()
            }
        }
    }

    private fun resolve(name: String): File? {
        if (name.isBlank() || name.contains('/') || name.contains('\\') || name.contains("..")) return null
        return dir?.let { File(it, name) }
    }

    private fun readRotation(context: Context, uri: Uri): Int = try {
        if (Build.VERSION.SDK_INT < 24) {
            0
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        }
    } catch (_: Throwable) {
        0
    }
}