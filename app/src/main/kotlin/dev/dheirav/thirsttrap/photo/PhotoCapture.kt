package dev.dheirav.thirsttrap.photo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * Camera capture and gallery import, without a single runtime permission.
 *
 * The Photo Picker (`PickVisualMedia`) needs no storage permission at all, and
 * `TakePicture` writing into a FileProvider URI needs no camera permission -
 * the camera app owns that. Two features, zero permission prompts.
 */
class PhotoCaptureLaunchers(
    val takePhoto: () -> Unit,
    val pickFromGallery: () -> Unit,
)

@Composable
fun rememberPhotoCapture(onPhoto: (Uri) -> Unit): PhotoCaptureLaunchers {
    val context = LocalContext.current
    // Held across recompositions: TakePicture reports success, not a Uri, so
    // the destination has to be remembered from before the launch.
    val pending = remember { arrayOfNulls<Uri>(1) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) pending[0]?.let(onPhoto)
        pending[0] = null
    }

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(onPhoto) }

    return remember {
        PhotoCaptureLaunchers(
            takePhoto = {
                val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                file.parentFile?.mkdirs()
                val uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file,
                )
                pending[0] = uri
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
