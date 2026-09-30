package dev.dheirav.thirsttrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.AppSettings
import dev.dheirav.thirsttrap.domain.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SettingsRepository,
) : ViewModel() {
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
