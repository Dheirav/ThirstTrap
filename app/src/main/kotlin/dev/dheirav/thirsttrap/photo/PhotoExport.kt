package dev.dheirav.thirsttrap.photo

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/**
 * Getting one photo out of the app, two ways.
 *
 * Both are per-photo and user-initiated. Bulk export is not here and should not
 * be: the backup zip already carries every photo with the diary attached, and a
 * folder of fifty loose JPEGs with no idea which plant or which day each
 * belongs to is strictly worse than the thing that already exists.
 *
 * A photo that leaves is a **copy**. Delete the plant and the copy stays; edit
 * the caption and the copy does not know. That is the right behaviour for an
 * escape hatch and the reason the gallery can never be a second source of
 * truth for the diary.
 */
object PhotoExport {

    /**
     * An intent that hands one photo to whatever the user picks.
     *
     * The photo is copied into the cache first rather than served from the
     * library, because `file_paths.xml` deliberately exposes only the camera
     * scratch directory and says so: "the photo library itself is never
     * exposed; outbound sharing goes through an explicit export". A copy into
     * the already-shared cache is that explicit export. Widening the provider
     * to cover `files/photos` would have been one line and would have quietly
     * made every photo in the diary reachable by any component holding a
     * guessed uri.
     *
     * The cache is the OS's to reclaim, so these copies need no cleanup.
     */
    fun shareIntent(context: Context, source: File, caption: String?): Intent? {
        if (!source.exists()) return null
        val staged = File(context.cacheDir, "share-${source.name}")
        runCatching { source.copyTo(staged, overwrite = true) }.onFailure { return null }

        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", staged,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            // The caption travels as text when there is one, because a photo of
            // a leaf is not self-explanatory to whoever receives it.
            caption?.takeIf { it.isNotBlank() }?.let { putExtra(Intent.EXTRA_TEXT, it) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Whether saving to the gallery is offered at all.
     *
     * From API 29 an app can write its own images through MediaStore with no
     * permission whatsoever. Below that it needs WRITE_EXTERNAL_STORAGE, and
     * this app is not declaring a storage permission to serve two old API
     * levels: D35 declared CAMERA for one feature and silently broke another,
     * because a declared-but-ungranted permission changes how the platform
     * treats you. Sharing works everywhere and covers the same need.
     */
    val canSaveToGallery: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /**
     * Copies one photo into Pictures/ThirstTrap.
     *
     * Written with IS_PENDING set and cleared afterwards, so the gallery never
     * indexes a half-copied file. Returns false rather than throwing: this is
     * a convenience, and a failed save should say so quietly and leave
     * everything else alone.
     */
    fun saveToGallery(context: Context, source: File, displayName: String): Boolean {
        if (!canSaveToGallery || !source.exists()) return false
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ThirstTrap")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = runCatching {
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }.getOrNull() ?: return false

        val copied = runCatching {
            resolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
                ?: error("no output stream")
        }.isSuccess

        if (!copied) {
            runCatching { resolver.delete(uri, null, null) }
            return false
        }
        runCatching {
            resolver.update(uri, ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }, null, null)
        }
        return true
    }
}
