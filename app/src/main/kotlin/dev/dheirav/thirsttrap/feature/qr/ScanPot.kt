package dev.dheirav.thirsttrap.feature.qr

import android.content.Context
import android.util.Log
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Scanning a pot sticker.
 *
 * Google's code scanner runs the camera inside Play Services, so this needs no
 * CAMERA permission at all - the app never sees a frame, only the decoded
 * string.
 */
object ScanPot {

    private const val TAG = "TTPhoto"

    fun scan(
        context: Context,
        onPlantId: (String) -> Unit,
        onProblem: (String) -> Unit,
    ) {
        val scanner = GmsBarcodeScanning.getClient(
            context,
            GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .enableAutoZoom()
                .build(),
        )

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val raw = barcode.rawValue
                val id = raw?.removePrefix("thirsttrap://plant/")
                when {
                    raw == null -> onProblem("Nothing readable in that code.")
                    // A QR from anywhere else is not an error worth alarming
                    // anyone about - it is just not one of ours.
                    id == raw -> onProblem("That code isn't one of this app's pot stickers.")
                    else -> onPlantId(id!!)
                }
            }
            .addOnCanceledListener { /* backing out is not a failure */ }
            .addOnFailureListener { error ->
                Log.w(TAG, "scan failed", error)
                // The scanner is a Play Services module downloaded on demand,
                // so the first use needs a network. Every other feature in this
                // app works offline, and someone hitting this deserves to know
                // which of those two things has gone wrong.
                onProblem(
                    if (isModuleUnavailable(error)) {
                        "The scanner needs to download once before it can work offline. " +
                            "Connect to wifi and try again."
                    } else {
                        "Could not start the scanner: ${error.message}"
                    },
                )
            }
    }

    private fun isModuleUnavailable(error: Exception): Boolean =
        (error as? com.google.mlkit.common.MlKitException)?.errorCode ==
            com.google.mlkit.common.MlKitException.UNAVAILABLE ||
            error.message?.contains("module", ignoreCase = true) == true

    /**
     * Fetches the scanner module ahead of time, so the first real scan does not
     * stall in front of someone holding a watering can. Fire-and-forget: if it
     * fails there is nothing useful to say yet, and the scan path explains
     * itself when it is actually needed.
     */
    fun prewarm(context: Context) {
        runCatching {
            ModuleInstall.getClient(context)
                .installModules(
                    com.google.android.gms.common.moduleinstall.ModuleInstallRequest.newBuilder()
                        .addApi(GmsBarcodeScanning.getClient(context))
                        .build(),
                )
                .addOnSuccessListener { Log.i(TAG, "scanner module ready") }
                .addOnFailureListener { Log.i(TAG, "scanner module not fetched yet: ${it.message}") }
        }
    }
}
