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
    private val onlineLookupKey = booleanPreferencesKey("online_species_lookup")
    private val canMlKey = doublePreferencesKey("watering_can_ml")
    private val canSizesKey = stringSetPreferencesKey("watering_can_sizes_ml")

    override val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            dynamicColor = prefs[dynamicColorKey] ?: false,
            reminderHour = prefs[reminderHourKey] ?: 9,
            defaultDepletionTrigger = prefs[triggerKey] ?: DEFAULT_DEPLETION_TRIGGER,
            useExactAlarms = prefs[exactAlarmsKey] ?: false,
            // Absent means off. A missing preference must never be read as
            // consent to use the network.
            onlineSpeciesLookup = prefs[onlineLookupKey] ?: false,
            wateringCanMl = prefs[canMlKey] ?: 1000.0,
            // A set on disk because DataStore has no list; sorted on read so
            // the chips are in a stable order however they were added.
            wateringCanSizesMl = prefs[canSizesKey]
                .orEmpty().mapNotNull { it.toDoubleOrNull() }.sorted(),
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

    override suspend fun setOnlineSpeciesLookup(enabled: Boolean) {
        context.dataStore.edit { it[onlineLookupKey] = enabled }
    }

    override suspend fun setWateringCanMl(ml: Double) {
        context.dataStore.edit { it[canMlKey] = ml.coerceIn(1.0, 100_000.0) }
    }

    override suspend fun setWateringCanSizes(sizesMl: List<Double>) {
        val cleaned = sizesMl.filter { it > 0 }.distinct().sorted().take(12)
        context.dataStore.edit { prefs ->
            prefs[canSizesKey] = cleaned.map { it.toString() }.toSet()
        }
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
