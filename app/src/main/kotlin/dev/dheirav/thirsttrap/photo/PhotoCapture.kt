package dev.dheirav.thirsttrap.photo

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * Camera capture and gallery import.
 *
 * The Photo Picker needs no storage permission. `TakePicture` used to need no
 * camera permission either, and the comment here said so, because the camera
 * app owns the camera and this only hands it a FileProvider URI to write into.
 *
 * That stopped being true the moment D35 added an unconditional CAMERA
 * declaration to the manifest for the in-process QR scanner. Android's rule is
 * specific and asymmetric: `ACTION_IMAGE_CAPTURE` is refused for a caller that
 * *declares* CAMERA without holding it, and allowed for one that never declares
 * it at all. So adding the scanner broke the photo button without touching a
 * line of this file.
 *
 * Observed, not reasoned about. With the permission revoked, tapping the photo
 * button killed the app:
 * ```
 * FATAL EXCEPTION: main
 * java.lang.SecurityException: Permission Denial: starting Intent
 *   { act=android.media.action.IMAGE_CAPTURE ... } with revoked permission
 *   android.permission.CAMERA
 *     at ...PhotoCaptureKt.rememberPhotoCapture$lambda(PhotoCapture.kt:66)
 * ```
 * Anyone who attached a photo before ever opening the scanner hit that, which
 * is most people, because the scanner is the niche feature.
 *
 * So the permission is asked for here, on the photo path, rather than being
 * left to the scanner to happen to have asked first. A denial is not a crash
 * and not a dialog: the gallery import needs no permission at all and is the
 * better answer for somebody who has just said no to the camera.
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
    // Survives the Activity being destroyed behind the permission dialog, for
    // the same reason pendingUri does.
    var awaitingPermission by rememberSaveable { mutableStateOf(false) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingUri
        pendingUri = null
        if (!ok) { Log.i(TAG, "capture cancelled"); return@rememberLauncherForActivityResult }
        if (uri == null) { Log.w(TAG, "capture returned but destination was lost"); return@rememberLauncherForActivityResult }
        Log.i(TAG, "capture ok: $uri")
        onPhoto(Uri.parse(uri))
    }

    // Declared before the launcher that uses it, so the lambda can call it.
    val launchCamera = remember(context) {
        {
            val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
            file.parentFile?.mkdirs()
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file,
            )
            pendingUri = uri.toString()
            camera.launch(uri)
        }
    }

    val askCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val wasAsking = awaitingPermission
        awaitingPermission = false
        if (!wasAsking) return@rememberLauncherForActivityResult
        if (granted) {
            launchCamera()
        } else {
            // No second prompt and no explanation the user did not ask for.
            // They said no to the camera; the gallery needs nothing.
            Log.i(TAG, "camera permission refused, no capture")
        }
    }

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) Log.i(TAG, "gallery pick cancelled") else onPhoto(uri)
    }

    return remember(context) {
        PhotoCaptureLaunchers(
            takePhoto = {
                val held = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED
                if (held) {
                    launchCamera()
                } else {
                    awaitingPermission = true
                    askCamera.launch(Manifest.permission.CAMERA)
                }
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
