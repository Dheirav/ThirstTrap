package dev.dheirav.thirsttrap.feature.qr

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StickerViewModel @Inject constructor(
    plants: PlantRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val plantId: String = checkNotNull(savedStateHandle["id"])

    val plant: StateFlow<Plant?> =
        plants.observePlant(plantId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
