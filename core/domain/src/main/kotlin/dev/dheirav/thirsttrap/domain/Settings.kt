package dev.dheirav.thirsttrap.domain

import kotlinx.coroutines.flow.Flow

/**
 * User settings. Deliberately small - this grows with X1, not before.
 */
data class AppSettings(
    /**
     * Material You. Off by default: dynamic colour means the app's own palette
     * is never actually seen, and that palette is the intended look. Switchable
     * for anyone who prefers their wallpaper theme to win.
     */
    val dynamicColor: Boolean = false,
    /** Local hour the daily reminder sweep runs. */
    val reminderHour: Int = 9,
    /** Starting depletion trigger for newly added plants. */
    val defaultDepletionTrigger: Double = DEFAULT_DEPLETION_TRIGGER,
    /**
     * Off by default - handover D4. SCHEDULE_EXACT_ALARM is denied by default
     * on Android 13+, and a watering check does not need to land at 09:00:00.
     */
    val useExactAlarms: Boolean = false,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setReminderHour(hour: Int)
    suspend fun setDefaultDepletionTrigger(fraction: Double)
    suspend fun setUseExactAlarms(enabled: Boolean)

    /** Dismissals are per drying cycle, so a new cycle can speak up again. */
    suspend fun dismissDiagnostic(key: String)
    suspend fun dismissedDiagnostics(): Set<String>
}
