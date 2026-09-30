package dev.dheirav.thirsttrap.feature.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.SettingsRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IntroViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    /** Records that it has been read, then hands back to the caller. */
    fun dismiss(onDone: () -> Unit) {
        viewModelScope.launch {
            settings.setIntroSeen(true)
            onDone()
        }
    }
}
