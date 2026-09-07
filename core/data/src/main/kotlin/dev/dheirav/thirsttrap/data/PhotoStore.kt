package dev.dheirav.thirsttrap.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Photos on disk. The database only ever holds relative paths.
 *
 * App-private internal storage: no permission needed, invisible to other apps,
 * and wiped on uninstall - which is exactly why export matters.
 * docs/ARCHITECTURE.md section 5.
 */
@Singleton
class PhotoStore @Inject constructor(
    private val context: Context,
) {
    private val root: File get() = File(context.filesDir, "photos")

    fun absoluteFile(relativePath: String): File = File(context.filesDir, relativePath)

    fun newRelativePath(plantId: String, photoId: String): String = "photos/$plantId/$photoId.jpg"

    /** A scratch file for the camera to write its full-size capture into. */
    fun tempCaptureFile(): File =
        File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg").apply {
            parentFile?.mkdirs()
        }

    data class Saved(val relativePath: String, val width: Int, val height: Int, val bytes: Long, val takenAt: Long?)

    /**
     * Reads an image, downscales it, strips its metadata and writes a JPEG.
     *
     * Three things happen here deliberately:
     *  - **Downscale to [MAX_EDGE].** The storage budget is <1 GB for three
     *    years of ten plants; a 12 MP original blows that in weeks.
     *  - **Re-encode rather than copy.** Decoding to a Bitmap and writing a new
     *    JPEG drops every EXIF tag, including GPS. A plant photo carrying the
     *    coordinates of someone's home should not end up in an export.
     *  - **Apply the orientation tag before discarding it**, or stripping EXIF
     *    silently rotates every portrait photo.
     */
    fun saveFrom(source: Uri, plantId: String, photoId: String): Saved? {
        val takenAt = readExifTimestamp(source)

        // Read the dimensions without allocating a bitmap.
        //
        // The null check is on the STREAM, not on decodeStream's result: with
        // inJustDecodeBounds set, decodeStream always returns null by design,
        // so `openInputStream(...)?.use { decodeStream(...) } ?: return null`
        // bails out on every single image no matter how valid it is. That cost
        // two real captures before it was spotted.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = context.contentResolver.openInputStream(source) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val decoded = context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        val oriented = applyOrientation(source, decoded)
        val scaled = scaleToMaxEdge(oriented)

        val target = absoluteFile(newRelativePath(plantId, photoId))
        target.parentFile?.mkdirs()
        FileOutputStream(target).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }

        val result = Saved(
            relativePath = newRelativePath(plantId, photoId),
            width = scaled.width,
            height = scaled.height,
            bytes = target.length(),
            takenAt = takenAt,
        )
        if (scaled !== oriented) scaled.recycle()
        if (oriented !== decoded) oriented.recycle()
        decoded.recycle()
        return result
    }

    /**
     * Removes camera scratch files.
     *
     * A capture that is imported has served its purpose, and one that is not -
     * cancelled, or interrupted by the Activity being destroyed - would
     * otherwise sit in the cache forever at full camera resolution. Two such
     * files accumulated during development before this existed.
     */
    fun clearStaleCaptures(exceptUri: String? = null, olderThanMillis: Long = 0L) {
        val cutoff = System.currentTimeMillis() - olderThanMillis
        context.cacheDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("capture_") }
            ?.filterNot { exceptUri != null && exceptUri.endsWith(it.name) }
            ?.filter { it.lastModified() <= cutoff }
            ?.forEach { runCatching { it.delete() } }
    }

    fun delete(relativePath: String) {
        runCatching { absoluteFile(relativePath).delete() }
    }

    fun deleteForPlant(plantId: String) {
        runCatching { File(root, plantId).deleteRecursively() }
    }

    /** Files on disk with no row pointing at them - see docs/DATA-MODEL.md. */
    fun orphanFiles(knownRelativePaths: Set<String>): List<File> {
        if (!root.exists()) return emptyList()
        return root.walkTopDown()
            .filter { it.isFile }
            .filterNot { file ->
                val rel = file.relativeToOrNull(context.filesDir)?.path ?: return@filterNot false
                rel in knownRelativePaths
            }
            .toList()
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        while (max(width, height) / sample > MAX_EDGE * 2) sample *= 2
        return sample
    }

    private fun scaleToMaxEdge(bitmap: Bitmap): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= MAX_EDGE) return bitmap
        val ratio = MAX_EDGE.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    private fun applyOrientation(source: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(source)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: return bitmap

        val matrix = android.graphics.Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrDefault(bitmap)
    }

    /** A gallery import keeps its original capture time, not the import time. */
    private fun readExifTimestamp(source: Uri): Long? = runCatching {
        context.contentResolver.openInputStream(source)?.use { stream ->
            val raw = ExifInterface(stream).getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: return@use null
            java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
                .parse(raw)?.time
        }
    }.getOrNull()

    private companion object {
        /** Requirements NFR: ~200-500 KB per photo. */
        const val MAX_EDGE = 1600
        const val JPEG_QUALITY = 80
    }
}
