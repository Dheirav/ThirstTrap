package dev.dheirav.thirsttrap.feature.plantdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.averageWateringIntervalDays
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One day's worth of events, for a sticky header. */
data class TimelineDay(val label: String, val events: List<CareEvent>)

data class PlantDetailUiState(
    val plant: Plant? = null,
    val days: List<TimelineDay> = emptyList(),
    val totalEvents: Int = 0,
    val averageIntervalDays: Double? = null,
    val loaded: Boolean = false,
)

@HiltViewModel
class PlantDetailViewModel @Inject constructor(
    private val repository: PlantRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val plantId: String = checkNotNull(savedStateHandle["id"])

    val state: StateFlow<PlantDetailUiState> =
        combine(
            repository.observePlant(plantId),
            repository.observeEvents(plantId),
        ) { plant, events ->
            PlantDetailUiState(
                plant = plant,
                days = groupByLocalDay(events),
                totalEvents = events.size,
                averageIntervalDays = averageWateringIntervalDays(
                    events.filter { it.type == CareEventType.WATERED }.map { it.timestampMillis },
                ),
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlantDetailUiState())

    fun deleteEvent(event: CareEvent) {
        viewModelScope.launch { repository.deleteEvent(event.id) }
    }

    /**
     * Grouped by the **local** civil day the event happened on, reconstructed
     * from its stored offset rather than the device's current zone. Water at
     * 23:00 IST, read it back in another timezone, and a naive conversion moves
     * it to the previous day - which is exactly the off-by-one the dual
     * timestamp columns exist to prevent.
     */
    private fun groupByLocalDay(events: List<CareEvent>): List<TimelineDay> {
        val now = System.currentTimeMillis()
        val todayIndex = localDayIndex(now, offsetMinutesNow())

        return events
            .groupBy { localDayIndex(it.timestampMillis, it.tzOffsetMinutes) }
            .toSortedMap(compareByDescending { it })
            .map { (dayIndex, dayEvents) ->
                TimelineDay(
                    label = when (todayIndex - dayIndex) {
                        0L -> "Today"
                        1L -> "Yesterday"
                        in 2L..6L -> "${todayIndex - dayIndex} days ago"
                        else -> formatDate(dayEvents.first())
                    },
                    events = dayEvents.sortedByDescending { it.timestampMillis },
                )
            }
    }

    private fun offsetMinutesNow(): Int =
        java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000

    private fun localDayIndex(utcMillis: Long, offsetMinutes: Int): Long =
        Math.floorDiv(utcMillis + offsetMinutes * 60_000L, 86_400_000L)

    private fun formatDate(event: CareEvent): String {
        val zone = java.time.ZoneOffset.ofTotalSeconds(event.tzOffsetMinutes * 60)
        val date = java.time.Instant.ofEpochMilli(event.timestampMillis).atZone(zone).toLocalDate()
        return date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
    }
}
