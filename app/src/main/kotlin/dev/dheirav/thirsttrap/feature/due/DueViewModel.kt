package dev.dheirav.thirsttrap.feature.due

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.CheckResult
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.Reminder
import dev.dheirav.thirsttrap.domain.ReminderRepository
import dev.dheirav.thirsttrap.domain.computeNextDue
import dev.dheirav.thirsttrap.domain.newId
import dev.dheirav.thirsttrap.domain.resolveIntervalDays
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DueItem(val reminder: Reminder, val plant: Plant, val overdueDays: Int)

data class DueUiState(val items: List<DueItem> = emptyList(), val loaded: Boolean = false)

@HiltViewModel
class DueViewModel @Inject constructor(
    private val reminders: ReminderRepository,
    private val plants: PlantRepository,
) : ViewModel() {

    val uiState: StateFlow<DueUiState> =
        combine(reminders.observeReminders(), plants.observePlants()) { rs, ps ->
            val now = System.currentTimeMillis()
            val byId = ps.associateBy { it.id }
            DueUiState(
                items = rs.filter { it.isDue(now) }
                    .mapNotNull { r ->
                        byId[r.plantId]?.let {
                            DueItem(r, it, ((now - r.nextDueAtMillis) / 86_400_000L).toInt())
                        }
                    }
                    .sortedBy { it.reminder.nextDueAtMillis },
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DueUiState())

    fun watered(item: DueItem) = record(item, CareEventType.WATERED, null)

    fun stillWet(item: DueItem) = record(item, CareEventType.CHECKED, CheckResult.STILL_HEAVY)

    private fun record(item: DueItem, type: CareEventType, result: CheckResult?) {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            plants.logEvent(
                CareEvent(
                    id = newId(),
                    plantId = item.plant.id,
                    timestampMillis = now,
                    tzOffsetMinutes = tzOffsetMinutesAt(now),
                    type = type,
                    checkResult = result,
                ),
            )
            // A check that concluded "still wet" resets the clock exactly as a
            // watering does. Both mean the pot was assessed.
            val interval = resolveIntervalDays(item.reminder.intervalDays, null, null)
            reminders.reschedule(item.plant.id, computeNextDue(now, interval, now))
        }
    }

    fun snooze(item: DueItem) {
        viewModelScope.launch {
            reminders.snooze(item.reminder.id, System.currentTimeMillis() + 86_400_000L)
        }
    }

    /** Clears the pile without recording waterings that did not happen. */
    fun clearAllOverdue() {
        viewModelScope.launch { reminders.clearAllOverdue(System.currentTimeMillis()) }
    }
}
