package dev.dheirav.thirsttrap.feature.qr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import dev.dheirav.thirsttrap.ui.AppIcons
import dev.dheirav.thirsttrap.ui.Button
import java.util.concurrent.Executors

/**
 * Scanning a pot sticker, entirely in-process.
 *
 * This replaced Google's play-services code scanner, which needed no camera
 * permission but shipped transport-backend-cct - the Clearcut telemetry
 * uploader - inside an app whose Settings promise "nothing leaves this phone".
 * The trade is one runtime permission prompt for a scanner where no frame,
 * and no fact about scanning, ever leaves the process. zxing was already here
 * generating the stickers, so decoding them with it costs no new dependency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanPotScreen(
    onBack: () -> Unit,
    onPlantId: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var denied by remember { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { ok ->
        granted = ok
        denied = !ok
    }
    LaunchedEffect(Unit) { if (!granted) ask.launch(Manifest.permission.CAMERA) }

    // A QR from anywhere else is not an error worth alarming anyone about -
    // it is just not one of ours. Shown inline and cleared by the next frame
    // that decodes.
    var foreignCode by remember { mutableStateOf(false) }
    // Frames keep arriving after a hit; only the first one navigates.
    var done by remember { mutableStateOf(false) }

    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan a pot sticker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.arrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                granted -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val view = PreviewView(ctx)
                            // COMPATIBLE renders through a TextureView. The
                            // default PERFORMANCE mode uses a SurfaceView,
                            // which on this app's own target phone (MIUI)
                            // painted uninitialised buffers - CRT static -
                            // instead of frames. Marginally more battery for
                            // a preview that actually shows the pot.
                            view.implementationMode =
                                PreviewView.ImplementationMode.COMPATIBLE
                            val providerFuture = ProcessCameraProvider.getInstance(ctx)
                            providerFuture.addListener({
                                val provider = providerFuture.get()
                                val preview = androidx.camera.core.Preview.Builder().build()
                                preview.surfaceProvider = view.surfaceProvider

                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                val mainExecutor = ContextCompat.getMainExecutor(ctx)
                                analysis.setAnalyzer(executor) { frame ->
                                    val text = frame.use { decodeQr(it) }
                                    if (text != null && !done) {
                                        val id = text.removePrefix("thirsttrap://plant/")
                                        if (id != text) {
                                            done = true
                                            // The analyzer runs on its own
                                            // thread; NavController does not.
                                            // Calling through directly crashed
                                            // on the first real scan.
                                            mainExecutor.execute { onPlantId(id) }
                                        } else {
                                            foreignCode = true
                                        }
                                    }
                                }

                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analysis,
                                )
                            }, ContextCompat.getMainExecutor(ctx))
                            view
                        },
                    )
                    if (foreignCode) {
                        Text(
                            "That code isn't one of this app's pot stickers.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                denied -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Scanning needs the camera - the frames stay on this phone, " +
                            "like everything else here.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = { ask.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.padding(top = 16.dp),
                    ) { Text("Allow the camera") }
                }
            }
        }
    }
}

/**
 * zxing over the Y plane. QR finder patterns make the symbol orientation-proof,
 * so the frame is decoded as delivered, without rotating buffers.
 */
private val reader = MultiFormatReader().apply {
    setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
}

private fun decodeQr(frame: ImageProxy): String? {
    val plane = frame.planes.firstOrNull() ?: return null
    val buffer = plane.buffer
    val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
    // The Y plane can carry padding bytes per row; rowStride is the real width.
    val source = PlanarYUVLuminanceSource(
        bytes,
        plane.rowStride,
        frame.height,
        0,
        0,
        frame.width,
        frame.height,
        false,
    )
    return try {
        reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
    } catch (_: com.google.zxing.NotFoundException) {
        null
    } catch (_: Exception) {
        null
    } finally {
        reader.reset()
    }
}
