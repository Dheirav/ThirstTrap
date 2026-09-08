package dev.dheirav.thirsttrap.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import androidx.datastore.preferences.preferencesDataStore
import dev.dheirav.thirsttrap.domain.AppSettings
import dev.dheirav.thirsttrap.domain.DEFAULT_DEPLETION_TRIGGER
import dev.dheirav.thirsttrap.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore, not SharedPreferences - docs/ARCHITECTURE.md section 3. */
private val Context.dataStore by preferencesDataStore(name = "thirsttrap_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val context: Context,
) : SettingsRepository {

    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val reminderHourKey = intPreferencesKey("reminder_hour")
    private val triggerKey = doublePreferencesKey("default_depletion_trigger")
    private val exactAlarmsKey = booleanPreferencesKey("use_exact_alarms")
    private val dismissedKey = stringSetPreferencesKey("dismissed_diagnostics")

    override val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            dynamicColor = prefs[dynamicColorKey] ?: false,
            reminderHour = prefs[reminderHourKey] ?: 9,
            defaultDepletionTrigger = prefs[triggerKey] ?: DEFAULT_DEPLETION_TRIGGER,
            useExactAlarms = prefs[exactAlarmsKey] ?: false,
        )
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[dynamicColorKey] = enabled }
    }

    override suspend fun setReminderHour(hour: Int) {
        context.dataStore.edit { it[reminderHourKey] = hour.coerceIn(0, 23) }
    }

    override suspend fun setUseExactAlarms(enabled: Boolean) {
        context.dataStore.edit { it[exactAlarmsKey] = enabled }
    }

    override suspend fun dismissDiagnostic(key: String) {
        context.dataStore.edit {
            it[dismissedKey] = (it[dismissedKey] ?: emptySet()) + key
        }
    }

    override suspend fun dismissedDiagnostics(): Set<String> =
        context.dataStore.data.first()[dismissedKey] ?: emptySet()

    override suspend fun setDefaultDepletionTrigger(fraction: Double) {
        context.dataStore.edit { it[triggerKey] = fraction.coerceIn(0.1, 0.9) }
    }
}
