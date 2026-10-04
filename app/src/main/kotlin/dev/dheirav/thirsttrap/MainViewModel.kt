package dev.dheirav.thirsttrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.AppSettings
import dev.dheirav.thirsttrap.domain.SettingsRepository
import dev.dheirav.thirsttrap.domain.ReminderRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SettingsRepository,
    reminders: ReminderRepository,
) : ViewModel() {
    /**
     * True once anything is actually scheduled.
     *
     * The notification permission used to be asked for in a LaunchedEffect(Unit)
     * the instant the user tapped "Open the diary", so Android's own dialog
     * about reminders arrived before the user owned a plant, let alone anything
     * to be reminded about. The docstring on ensureNotificationPermission
     * already named the right trigger and nothing had wired it. This is it.
     */
    val hasReminder: StateFlow<Boolean> =
        reminders.observeReminders()
            .map { it.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Null until DataStore has actually answered.
     *
     * It used to start at `AppSettings()`, whose `introSeen` is false, so every
     * cold launch flashed the first-run page for the few frames before the real
     * value arrived. A default is a guess, and a guess about whether the user
     * has already read something is one the UI must not act on.
     */
    val settings: StateFlow<AppSettings?> =
        settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
