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
    /**
     * Off by default, and the only setting in the app that can cause a packet
     * to leave the phone. The requirements call offline-first a promise rather
     * than a default, so this asks first and stays asked.
     */
    val onlineSpeciesLookup: Boolean = false,
    /**
     * The can currently being measured into, in millilitres.
     */
    val wateringCanMl: Double = 1000.0,
    /**
     * The cans and bottles this person actually owns, in millilitres.
     *
     * Empty to begin with, and deliberately so. A shipped list of 250/500/1000
     * is a guess about someone else's cupboard, and every wrong guess is a chip
     * they have to read past to reach the one they use. These are typed once
     * and then they are the presets.
     */
    val wateringCanSizesMl: List<Double> = emptyList(),
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setReminderHour(hour: Int)
    suspend fun setDefaultDepletionTrigger(fraction: Double)
    suspend fun setUseExactAlarms(enabled: Boolean)
    suspend fun setOnlineSpeciesLookup(enabled: Boolean)
    suspend fun setWateringCanMl(ml: Double)
    suspend fun setWateringCanSizes(sizesMl: List<Double>)

    /** Dismissals are per drying cycle, so a new cycle can speak up again. */
    suspend fun dismissDiagnostic(key: String)
    suspend fun dismissedDiagnostics(): Set<String>
}
