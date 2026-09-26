package dev.dheirav.thirsttrap.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.Stats
import dev.dheirav.thirsttrap.domain.computeStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    plants: PlantRepository,
    weights: dev.dheirav.thirsttrap.domain.WeightRepository,
) : ViewModel() {

    val state: StateFlow<Stats> =
        combine(
            // Archived and dead plants are included on purpose: a collection's
            // history is the point of this screen, and leaving out what died
            // would make the outcomes table a lie.
            plants.observePlants(includeArchived = true),
            plants.observeAllEvents(),
            weights.observeAllReadings(),
        ) { plantList, events, readings ->
            computeStats(
                plantList,
                events,
                System.currentTimeMillis(),
                readingsByPlant = readings.groupBy { it.plantId },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Stats())
}
