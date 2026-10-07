package dev.dheirav.thirsttrap.feature.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * The plants a sticker sheet would cover, and what happened last time you made one.
 *
 * Archived plants are left out. A sheet is something you print and cut up, and a
 * label for a plant that is no longer on the shelf is a label you throw away
 * after cutting it out.
 */
@HiltViewModel
class StickerSheetViewModel @Inject constructor(
    plants: PlantRepository,
) : ViewModel() {

    val plants: StateFlow<List<Plant>> = plants.observePlants(includeArchived = false)
        .map { list -> list.sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _result = MutableStateFlow<String?>(null)

    /** The last outcome, for a screen reader and for anybody who missed the toast. */
    val result: StateFlow<String?> = _result.asStateFlow()

    fun say(message: String?) { _result.value = message }
}
