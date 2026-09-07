package dev.dheirav.thirsttrap.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.dheirav.thirsttrap.data.ExportRepositoryImpl
import dev.dheirav.thirsttrap.data.MaintenanceRepository
import dev.dheirav.thirsttrap.data.TTLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Debug source set only, so it cannot reach a release build.
 *
 * Exists because the things most worth testing - an export, a re-import, the
 * reminder sweep - all sit behind a file picker or a notification, and driving
 * those by simulated taps means guessing at coordinates on somebody's actual
 * phone. This makes them checkable from a shell instead:
 *
 *     adb shell am broadcast -a dev.dheirav.thirsttrap.debug.EXPORT \
 *       -n dev.dheirav.thirsttrap/dev.dheirav.thirsttrap.debug.DebugReceiver
 */
@AndroidEntryPoint
class DebugReceiver : BroadcastReceiver() {

    @Inject lateinit var exporter: ExportRepositoryImpl
    @Inject lateinit var maintenance: MaintenanceRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_EXPORT -> exporter.exportToFileForDebug("debug")
                        .onSuccess { TTLog.i(TAG) { "DEBUG export -> ${it.absolutePath} ${it.length()}B" } }
                        .onFailure { TTLog.e(TAG, { "DEBUG export failed" }, it) }

                    ACTION_IMPORT -> exporter.importFromCacheForDebug()
                        .onSuccess { TTLog.i(TAG) { "DEBUG import -> $it" } }
                        .onFailure { TTLog.e(TAG, { "DEBUG import failed" }, it) }

                    ACTION_STORAGE -> {
                        val report = maintenance.report()
                        TTLog.i(TAG) { "DEBUG storage -> $report" }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "TTDebug"
        const val ACTION_EXPORT = "dev.dheirav.thirsttrap.debug.EXPORT"
        const val ACTION_IMPORT = "dev.dheirav.thirsttrap.debug.IMPORT"
        const val ACTION_STORAGE = "dev.dheirav.thirsttrap.debug.STORAGE"
    }
}
