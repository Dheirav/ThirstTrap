package dev.dheirav.thirsttrap.feature.propagation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.PropagationCard
import dev.dheirav.thirsttrap.domain.PropagationStage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BoardUiState(
    val columns: Map<PropagationStage, List<PropagationCard>> = emptyMap(),
    val loaded: Boolean = false,
) {
    val isEmpty: Boolean get() = columns.values.all { it.isEmpty() }
    val total: Int get() = columns.values.sumOf { it.size }
}

@HiltViewModel
class PropagationViewModel @Inject constructor(
    private val plants: PlantRepository,
) : ViewModel() {

    val state: StateFlow<BoardUiState> =
        combine(
            plants.observePlants(includeArchived = false),
            plants.observePlants(includeArchived = true),
        ) { active, _ ->
            val cards = active
                .filter { it.isPropagating }
                .mapNotNull { plant ->
                    val stage = plant.effectiveStage ?: return@mapNotNull null
                    PropagationCard(
                        plant = plant,
                        stage = stage,
                        // Falls back to nothing rather than pretending the
                        // cutting arrived at this stage the moment it was added.
                        stageSinceMillis = plant.propagationStageSinceMillis,
                        lastEventMillis = null,
                    )
                }
            BoardUiState(
                columns = PropagationStage.entries.associateWith { s ->
                    cards.filter { it.stage == s }
                },
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BoardUiState())

    fun move(card: PropagationCard, to: PropagationStage) {
        viewModelScope.launch { plants.setPropagationStage(card.plant.id, to) }
    }
}
