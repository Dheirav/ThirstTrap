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

data class DueItem(
    val reminder: Reminder,
    val plant: Plant,
    /** Days since the plant was last assessed - NOT days past the due date. */
    val daysSinceChecked: Int?,
    /** So "Watered" here records the same amount the dashboard would. */
    val suggestedWaterMl: Double?,
)

data class DueUiState(val items: List<DueItem> = emptyList(), val loaded: Boolean = false)

@HiltViewModel
class DueViewModel @Inject constructor(
    private val reminders: ReminderRepository,
    private val plants: PlantRepository,
) : ViewModel() {

    val uiState: StateFlow<DueUiState> =
        combine(
            reminders.observeReminders(),
            plants.observeDashboard { System.currentTimeMillis() },
        ) { rs, dash ->
            val now = System.currentTimeMillis()
            val byId = dash.associateBy { it.plant.id }
            DueUiState(
                items = rs.filter { it.isDue(now) }
                    .mapNotNull { r ->
                        byId[r.plantId]?.let { att ->
                            DueItem(
                                reminder = r,
                                plant = att.plant,
                                daysSinceChecked = att.lastCheckedMillis
                                    ?.let { ((now - it) / 86_400_000L).toInt() },
                                suggestedWaterMl = att.suggestedWaterMl,
                            )
                        }
                    }
                    .sortedBy { it.reminder.nextDueAtMillis },
                loaded = true,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DueUiState())

    fun watered(item: DueItem, onLogged: (CareEvent, Long) -> Unit) =
        record(item, CareEventType.WATERED, null, item.suggestedWaterMl, onLogged)

    fun stillWet(item: DueItem, onLogged: (CareEvent, Long) -> Unit) =
        record(item, CareEventType.CHECKED, CheckResult.STILL_HEAVY, null, onLogged)

    private fun record(
        item: DueItem,
        type: CareEventType,
        result: CheckResult?,
        amountMl: Double?,
        onLogged: (CareEvent, Long) -> Unit,
    ) {
        val now = System.currentTimeMillis()
        val previousDue = item.reminder.nextDueAtMillis
        val event = CareEvent(
            id = newId(),
            plantId = item.plant.id,
            timestampMillis = now,
            tzOffsetMinutes = tzOffsetMinutesAt(now),
            type = type,
            checkResult = result,
            amountMl = amountMl,
        )
        viewModelScope.launch {
            plants.logEvent(event)
            // A check that concluded "still wet" resets the clock exactly as a
            // watering does. Both mean the pot was assessed.
            // Was resolveIntervalDays(explicit, null, null), so marking a plant
            // done pushed it out a flat week no matter what the pot was doing.
            reminders.rescheduleFromModel(item.plant.id, now)
            onLogged(event, previousDue)
        }
    }

    /**
     * Undo has to put back BOTH halves. Deleting the event alone would leave
     * the reminder pushed a week out with nothing recorded - the same
     * half-undo that made the dashboard's undo look like a no-op.
     */
    fun undo(item: DueItem, event: CareEvent, previousDueMillis: Long) {
        viewModelScope.launch {
            plants.deleteEvent(event.id)
            reminders.reschedule(item.plant.id, previousDueMillis)
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
