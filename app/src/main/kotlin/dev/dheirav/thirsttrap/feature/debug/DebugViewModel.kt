package dev.dheirav.thirsttrap.feature.debug

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.BuildConfig
import dev.dheirav.thirsttrap.data.ExportRepositoryImpl
import dev.dheirav.thirsttrap.domain.AppSettings
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.ReminderRepository
import dev.dheirav.thirsttrap.domain.SettingsRepository
import dev.dheirav.thirsttrap.reminder.ReminderNotifier
import dev.dheirav.thirsttrap.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Feature X6. Debug builds only.
 *
 * docs/NOTIFICATIONS.md section 7 is blunt about why this exists: notification
 * delivery cannot be meaningfully automated, and without a way to fire one on
 * demand every test in the matrix costs a day of waiting.
 */
@HiltViewModel
class DebugViewModel @Inject constructor(
    private val plants: PlantRepository,
    private val reminders: ReminderRepository,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val exporter: ExportRepositoryImpl,
) : ViewModel() {

    /** Re-imports the debug archive, to prove importing twice changes nothing. */
    fun importFromCache() {
        viewModelScope.launch {
            exporter.importFromCacheForDebug()
                .onSuccess { _status.value = "Imported: $it" }
                .onFailure { _status.value = "Failed: ${it.message}" }
        }
    }

    /** Writes an export to a fixed path so the archive can be checked directly. */
    fun exportToCache() {
        viewModelScope.launch {
            exporter.exportToFileForDebug(BuildConfig.VERSION_NAME)
                .onSuccess { _status.value = "Wrote ${it.absolutePath} (${it.length()} bytes)" }
                .onFailure { _status.value = "Failed: ${it.message}" }
        }
    }

    val appSettings: StateFlow<AppSettings> =
        settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status.asStateFlow()

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settings.setDynamicColor(enabled) }
    }

    fun fireReminderNow(context: Context) {
        viewModelScope.launch {
            val plant = plants.observePlants().first().firstOrNull()
            if (plant == null) { _status.value = "No plants to remind about"; return@launch }
            ReminderNotifier.showCheckReminder(context, plant.id, plant.name, daysSince = 0)
            _status.value = "Fired for ${plant.name}"
        }
    }

    /**
     * Pulls every reminder forward so they are due immediately. This is what
     * makes the reminder-tone question answerable today rather than in a week.
     */
    fun makeAllDueNow() {
        viewModelScope.launch {
            val all = reminders.observeReminders().first()
            val now = System.currentTimeMillis()
            all.forEach { reminders.upsert(it.copy(nextDueAtMillis = now - 1, snoozedUntilMillis = null)) }
            _status.value = "${all.size} reminder(s) now due"
        }
    }

    /** Runs the real WorkManager sweep, rather than faking a notification. */
    fun runSweepNow(context: Context) {
        scheduler.runSweepNow(context)
        _status.value = "Sweep enqueued"
    }

    fun dumpWorkQueue(context: Context) {
        viewModelScope.launch {
            val infos = runCatching {
                WorkManager.getInstance(context)
                    .getWorkInfosForUniqueWork(ReminderScheduler.WORK_NAME).get()
            }.getOrNull().orEmpty()
            _status.value = if (infos.isEmpty()) {
                "No periodic work enqueued"
            } else {
                infos.joinToString("\n") { "${it.state} runs=${it.runAttemptCount}" }
            }
        }
    }

    fun resetReminders() {
        viewModelScope.launch {
            val all = reminders.observeReminders().first()
            all.forEach { reminders.reschedule(it.plantId, System.currentTimeMillis() + 7 * 86_400_000L) }
            _status.value = "Reset ${all.size} reminder(s) to +7 days"
        }
    }
}
