package dev.dheirav.thirsttrap.feature.experiments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.ExperimentRepository
import dev.dheirav.thirsttrap.domain.ExperimentWithSubjects
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExperimentDetailUiState(
    val experiment: ExperimentWithSubjects? = null,
    /** Subject plants resolved to names, keyed by plant id. */
    val plantsById: Map<String, Plant> = emptyMap(),
    /** Plants not yet enrolled, for the add-subject picker. */
    val candidates: List<Plant> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class ExperimentDetailViewModel @Inject constructor(
    private val experiments: ExperimentRepository,
    plants: PlantRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val experimentId: String = checkNotNull(savedStateHandle["id"])

    val state: StateFlow<ExperimentDetailUiState> =
        combine(
            experiments.observe(experimentId),
            plants.observePlants(includeArchived = false),
        ) { e, plantList ->
            val enrolled = e?.subjects?.map { it.plantId }?.toSet().orEmpty()
            ExperimentDetailUiState(
                experiment = e,
                plantsById = plantList.associateBy { it.id },
                candidates = plantList.filter { it.id !in enrolled },
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExperimentDetailUiState())

    fun addSubject(plantId: String, label: String) {
        if (label.isBlank()) return
        viewModelScope.launch { experiments.addSubject(experimentId, plantId, label.trim()) }
    }

    fun removeSubject(plantId: String) {
        viewModelScope.launch { experiments.removeSubject(experimentId, plantId) }
    }

    fun conclude(conclusion: String, onDone: () -> Unit) {
        if (conclusion.isBlank()) return
        viewModelScope.launch {
            experiments.conclude(experimentId, conclusion.trim())
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            experiments.delete(experimentId)
            onDone()
        }
    }
}
