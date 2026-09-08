package dev.dheirav.thirsttrap.feature.care

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.LookupFailure
import dev.dheirav.thirsttrap.domain.LookupResult
import dev.dheirav.thirsttrap.domain.SettingsRepository
import dev.dheirav.thirsttrap.domain.SpeciesCare
import dev.dheirav.thirsttrap.domain.SpeciesLookup
import dev.dheirav.thirsttrap.domain.SpeciesLookupService
import dev.dheirav.thirsttrap.domain.findSpeciesCare
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
    /** Whether the user has opted into online name resolution at all. */
    val onlineLookupEnabled: Boolean = false,
)

/** State of the optional name lookup, which is never started on its own. */
sealed interface LookupState {
    data object Idle : LookupState
    data object Running : LookupState
    data class Found(val lookup: SpeciesLookup) : LookupState
    data class Failed(val reason: LookupFailure) : LookupState
}

@HiltViewModel
class CareViewModel @Inject constructor(
    private val plants: PlantRepository,
    private val settings: SettingsRepository,
    private val lookupService: SpeciesLookupService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plantId: String = checkNotNull(savedStateHandle["id"])

    private val _lookup = MutableStateFlow<LookupState>(LookupState.Idle)
    val lookup: StateFlow<LookupState> = _lookup.asStateFlow()

    /**
     * Resolves the typed name against GBIF and links the Wikipedia article.
     *
     * Only ever runs from an explicit tap. It cannot produce care advice - see
     * [SpeciesLookupService] - so a wrong answer here costs a wrong link, never
     * a wrong watering schedule.
     */
    fun lookUpOnline() {
        val name = state.value.plant?.species?.takeIf { it.isNotBlank() }
            ?: state.value.plant?.name.orEmpty()
        if (name.isBlank() || _lookup.value == LookupState.Running) return
        _lookup.value = LookupState.Running
        viewModelScope.launch {
            _lookup.value = when (val r = lookupService.lookUp(name)) {
                is LookupResult.Found -> LookupState.Found(r.lookup)
                is LookupResult.Failed -> LookupState.Failed(r.reason)
            }
        }
    }

    val state: StateFlow<CareUiState> =
        combine(
            plants.observePlant(plantId),
            settings.settings,
        ) { plant, appSettings ->
            // Matched on the species the user typed, falling back to the plant's
            // own name - people often name a plant after what it is.
            val care = findSpeciesCare(plant?.species) ?: findSpeciesCare(plant?.name)
            CareUiState(
                plant = plant,
                care = care,
                alreadyApplied = plant != null && care != null &&
                    kotlin.math.abs(plant.depletionTrigger - care.depletionTrigger) < 0.01 &&
                    plant.targetDryness == care.water,
                onlineLookupEnabled = appSettings.onlineSpeciesLookup,
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
