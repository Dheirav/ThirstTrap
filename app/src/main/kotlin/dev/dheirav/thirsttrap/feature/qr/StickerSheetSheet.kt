package dev.dheirav.thirsttrap.feature.qr

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.photo.PhotoExport
import dev.dheirav.thirsttrap.ui.AlmanacSheet
import dev.dheirav.thirsttrap.ui.Button
import dev.dheirav.thirsttrap.ui.FieldLabel
import dev.dheirav.thirsttrap.ui.OutlinedButton
import dev.dheirav.thirsttrap.ui.Space
import androidx.core.content.ContextCompat

/**
 * Make the sheet, then either keep it or send it somewhere.
 *
 * A sheet rather than a page: there is nothing to come back to and nothing to
 * configure. The two buttons are the two things anybody wants, and which one is
 * offered depends on the platform rather than on a preference, because writing
 * to the gallery needs API 29 and sharing works everywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerSheetSheet(onDismiss: () -> Unit, viewModel: StickerSheetViewModel = hiltViewModel()) {
    val plants by viewModel.plants.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    AlmanacSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(Space.Block),
        ) {
            FieldLabel("Sticker sheet")
            Text(
                if (plants.isEmpty()) {
                    "Nothing to print yet. Add a plant first."
                } else {
                    "One page with a code for each of your ${plants.size} plants, " +
                        "and its name under it. Print it, cut it up, tape one to " +
                        "each pot."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Space.Line, bottom = Space.Block),
            )

            if (plants.isNotEmpty()) {
                if (PhotoExport.canSaveToGallery) {
                    Button(
                        onClick = {
                            val ok = saveSheet(context, viewModel)
                            Toast.makeText(
                                context,
                                if (ok) "Saved to Pictures/ThirstTrap" else "Could not save the sheet",
                                Toast.LENGTH_SHORT,
                            ).show()
                            if (ok) onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save to gallery") }
                }
                OutlinedButton(
                    onClick = {
                        val intent = shareSheet(context, viewModel)
                        if (intent != null) {
                            ContextCompat.startActivity(context, intent, null)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Could not make the sheet", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = Space.Line),
                ) { Text("Share or print") }
            }
        }
    }
}

private fun saveSheet(
    context: android.content.Context,
    viewModel: StickerSheetViewModel,
): Boolean {
    val bmp = StickerSheet.render(viewModel.plants.value) ?: return false
    val file = StickerSheet.writeToCache(context, bmp, "thirsttrap-stickers.png") ?: return false
    // Saved as a PNG even though the gallery helper names its mime type jpeg:
    // a QR code is hard edges, and JPEG ringing around them is exactly what
    // makes a printed code fail to scan.
    return PhotoExport.saveToGallery(context, file, "thirsttrap-stickers.png")
}

private fun shareSheet(
    context: android.content.Context,
    viewModel: StickerSheetViewModel,
): android.content.Intent? {
    val bmp = StickerSheet.render(viewModel.plants.value) ?: return null
    val file = StickerSheet.writeToCache(context, bmp, "thirsttrap-stickers.png") ?: return null
    return PhotoExport.shareIntent(context, file, caption = null)
}
