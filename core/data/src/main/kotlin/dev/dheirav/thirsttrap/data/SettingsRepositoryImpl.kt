package dev.dheirav.thirsttrap.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.dheirav.thirsttrap.domain.AppSettings
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

    override val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            dynamicColor = prefs[dynamicColorKey] ?: false,
            reminderHour = prefs[reminderHourKey] ?: 9,
        )
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[dynamicColorKey] = enabled }
    }

    override suspend fun setReminderHour(hour: Int) {
        context.dataStore.edit { it[reminderHourKey] = hour.coerceIn(0, 23) }
    }
}
