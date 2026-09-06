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
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setReminderHour(hour: Int)
}
