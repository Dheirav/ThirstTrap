package dev.dheirav.thirsttrap.photo

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * Camera capture and gallery import, without a single runtime permission.
 *
 * The Photo Picker needs no storage permission, and `TakePicture` writing into
 * a FileProvider URI needs no camera permission - the camera app owns that.
 *
 * **Everything the callback needs is `rememberSaveable`, deliberately.** While
 * the camera app is in front this Activity can be destroyed - MIUI does it
 * readily - and a plain `remember` comes back null. `TakePicture` reports only
 * a boolean, so if the destination URI is forgotten there is nothing left to
 * import and the photo is silently dropped into the cache. That is exactly what
 * happened on the first real capture: the camera wrote a 1.9 MB file and the
 * app had forgotten what to do with it.
 */
class PhotoCaptureLaunchers(
    val takePhoto: () -> Unit,
    val pickFromGallery: () -> Unit,
)

@Composable
fun rememberPhotoCapture(onPhoto: (Uri) -> Unit): PhotoCaptureLaunchers {
    val context = LocalContext.current
    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingUri
        pendingUri = null
        if (!ok) { Log.i(TAG, "capture cancelled"); return@rememberLauncherForActivityResult }
        if (uri == null) { Log.w(TAG, "capture returned but destination was lost"); return@rememberLauncherForActivityResult }
        Log.i(TAG, "capture ok: $uri")
        onPhoto(Uri.parse(uri))
    }

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) Log.i(TAG, "gallery pick cancelled") else onPhoto(uri)
    }

    return remember(context) {
        PhotoCaptureLaunchers(
            takePhoto = {
                val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                file.parentFile?.mkdirs()
                val uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file,
                )
                pendingUri = uri.toString()
                camera.launch(uri)
            },
            pickFromGallery = {
                gallery.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }
}

internal const val TAG = "TTPhoto"
