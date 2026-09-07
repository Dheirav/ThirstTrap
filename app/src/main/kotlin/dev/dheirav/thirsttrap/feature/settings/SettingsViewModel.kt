package dev.dheirav.thirsttrap.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.data.MaintenanceRepository
import dev.dheirav.thirsttrap.data.StorageReport
import dev.dheirav.thirsttrap.domain.AppSettings
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.dheirav.thirsttrap.domain.SettingsRepository
import dev.dheirav.thirsttrap.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val maintenance: MaintenanceRepository,
    private val scheduler: ReminderScheduler,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val state: StateFlow<AppSettings> =
        settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _storage = MutableStateFlow<StorageReport?>(null)
    val storage: StateFlow<StorageReport?> = _storage.asStateFlow()

    private val _cleanupMessage = MutableStateFlow<String?>(null)
    val cleanupMessage: StateFlow<String?> = _cleanupMessage.asStateFlow()

    init { refreshStorage() }

    fun refreshStorage() {
        viewModelScope.launch { _storage.value = maintenance.report() }
    }

    fun setDynamicColor(on: Boolean) = viewModelScope.launch { settings.setDynamicColor(on) }

    fun setReminderHour(hour: Int) = viewModelScope.launch {
        settings.setReminderHour(hour)
        // Re-enqueue, or the setting would change a number and nothing else.
        scheduler.scheduleDailySweep(context, hour, replaceExisting = true)
    }

    /**
     * Re-checked every time rather than remembered: the OS can revoke this, and
     * a stale "yes" would mean reminders silently stopping.
     */
    fun canScheduleExact(): Boolean =
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
            true
        } else {
            context.getSystemService(android.app.AlarmManager::class.java)
                ?.canScheduleExactAlarms() == true
        }

    fun setUseExactAlarms(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !canScheduleExact()) {
                // Explain first, then send them to the settings page - never
                // bounce someone to a system screen with no context.
                _needsExactPermission.value = true
                return@launch
            }
            settings.setUseExactAlarms(enabled)
        }
    }

    private val _needsExactPermission = MutableStateFlow(false)
    val needsExactPermission: StateFlow<Boolean> = _needsExactPermission.asStateFlow()

    fun dismissExactPermissionPrompt() { _needsExactPermission.value = false }

    fun openExactAlarmSettings() {
        _needsExactPermission.value = false
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
        runCatching {
            context.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun setDefaultTrigger(fraction: Double) =
        viewModelScope.launch { settings.setDefaultDepletionTrigger(fraction) }

    fun cleanUp() {
        viewModelScope.launch {
            val (files, rows) = maintenance.cleanUp()
            _cleanupMessage.value = when {
                files == 0 && rows == 0 -> "Nothing to clean up."
                else -> "Removed $files stray ${plural(files, "file")} and " +
                    "$rows broken ${plural(rows, "entry", "entries")}."
            }
            refreshStorage()
        }
    }

    fun dismissCleanupMessage() { _cleanupMessage.value = null }

    private fun plural(n: Int, one: String, many: String = one + "s") = if (n == 1) one else many
}
