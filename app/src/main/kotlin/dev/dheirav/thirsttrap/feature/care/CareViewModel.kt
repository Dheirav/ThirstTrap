package dev.dheirav.thirsttrap.feature.care

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.SpeciesCare
import dev.dheirav.thirsttrap.domain.findSpeciesCare
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CareUiState(
    val plant: Plant? = null,
    val care: SpeciesCare? = null,
    /** True when the species was recognised but the plant already matches it. */
    val alreadyApplied: Boolean = false,
)

@HiltViewModel
class CareViewModel @Inject constructor(
    private val plants: PlantRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plantId: String = checkNotNull(savedStateHandle["id"])

    val state: StateFlow<CareUiState> =
        plants.observePlant(plantId).map { plant ->
            // Matched on the species the user typed, falling back to the plant's
            // own name - people often name a plant after what it is.
            val care = findSpeciesCare(plant?.species) ?: findSpeciesCare(plant?.name)
            CareUiState(
                plant = plant,
                care = care,
                alreadyApplied = plant != null && care != null &&
                    kotlin.math.abs(plant.depletionTrigger - care.depletionTrigger) < 0.01 &&
                    plant.targetDryness == care.water,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CareUiState())

    private val _applied = MutableStateFlow(false)
    val applied: StateFlow<Boolean> = _applied.asStateFlow()

    /**
     * Copies the suggestions onto the plant.
     *
     * The depletion trigger is the valuable one: it is the single number a user
     * has no way to guess, and getting it wrong makes every later prediction
     * wrong in the same direction.
     */
    fun applySuggestions() {
        val s = state.value
        val plant = s.plant ?: return
        val care = s.care ?: return
        viewModelScope.launch {
            plants.observePlant(plantId).first()?.let { current ->
                plants.upsertPlant(
                    current.copy(
                        depletionTrigger = care.depletionTrigger,
                        targetDryness = care.water,
                        lightNeeds = current.lightNeeds ?: care.light,
                        // Only where the user has not already said otherwise.
                        medium = if (current.medium == dev.dheirav.thirsttrap.domain.Medium.SOIL) {
                            care.medium
                        } else {
                            current.medium
                        },
                    ),
                )
            }
            _applied.value = true
        }
    }
}
