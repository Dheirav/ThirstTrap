package dev.dheirav.thirsttrap.data

import dev.dheirav.thirsttrap.data.dao.CareEventDao
import dev.dheirav.thirsttrap.data.dao.PlantDao
import dev.dheirav.thirsttrap.data.dao.WeightDao
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.assembleWeightState
import dev.dheirav.thirsttrap.domain.checkIntervalFromLogDays
import dev.dheirav.thirsttrap.domain.computeNextDue
import dev.dheirav.thirsttrap.domain.resolveIntervalDays
import kotlinx.coroutines.flow.first
import dev.dheirav.thirsttrap.data.dao.ReminderDao
import dev.dheirav.thirsttrap.data.entity.ReminderEntity
import dev.dheirav.thirsttrap.domain.DEFAULT_CHECK_INTERVAL_DAYS
import dev.dheirav.thirsttrap.domain.Reminder
import dev.dheirav.thirsttrap.domain.ReminderKind
import dev.dheirav.thirsttrap.domain.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao,
    private val plantDao: PlantDao,
    private val eventDao: CareEventDao,
    private val weightDao: WeightDao,
) : ReminderRepository {

    /**
     * The one place the check interval is decided.
     *
     * Assembles the weight state itself rather than taking a prediction from a
     * caller, because a caller that has to fetch it is a caller that can pass
     * null instead, which is how this came to behave as a weekly calendar.
     */
    override suspend fun rescheduleFromModel(plantId: String, nowMillis: Long) {
        val plantRow = plantDao.observePlant(plantId).first() ?: return
        val plant = plantRow.toDomain()
        val events = eventDao.observeForPlant(plantId).first().map { it.toDomain() }
        val readings = weightDao.observeForPlant(plantId).first().map { it.toDomainReading() }

        val waterings = events.filter { it.type == CareEventType.WATERED }
        val state = assembleWeightState(
            plant = plant,
            readings = readings,
            wateringEventsMillis = waterings.map { it.timestampMillis },
            repotEventsMillis = events
                .filter { it.type == CareEventType.REPOTTED || it.type == CareEventType.MEDIUM_CHANGED }
                .map { it.timestampMillis },
            nowMillis = nowMillis,
        )

        // The explicit setting lives on the reminder, not the plant.
        val explicit = dao.observeForPlant(plantId).first()
            .firstOrNull()?.intervalDays

        val interval = resolveIntervalDays(
            explicitIntervalDays = explicit,
            prediction = state.prediction,
            loggedAverageDays = checkIntervalFromLogDays(waterings.map { it.timestampMillis }),
        )

        // An assessment is a watering or a check: both mean the pot was looked
        // at, so both reset the clock.
        val lastAssessed = events
            .filter { it.type == CareEventType.WATERED || it.type == CareEventType.CHECKED }
            .maxOfOrNull { it.timestampMillis }

        val due = computeNextDue(lastAssessed, interval, nowMillis)
        TTLog.i(TTLog.REMINDER) {
            "plan plant=$plantId interval=${interval}d from=${state.prediction::class.simpleName}"
        }
        dao.reschedule(plantId, due)
    }

    override fun observeReminders(): Flow<List<Reminder>> =
        dao.observeAll().map { rows -> rows.map { it.toDomainReminder() } }

    override fun observeForPlant(plantId: String): Flow<List<Reminder>> =
        dao.observeForPlant(plantId).map { rows -> rows.map { it.toDomainReminder() } }

    override suspend fun dueNow(nowMillis: Long): List<Reminder> =
        dao.dueNow(nowMillis).map { it.toDomainReminder() }

    override suspend fun upsert(reminder: Reminder) {
        TTLog.i(TTLog.REMINDER) {
            "upsert reminder ${reminder.id} plant=${reminder.plantId} due=${reminder.nextDueAtMillis}"
        }
        dao.upsert(reminder.toReminderEntity(System.currentTimeMillis()))
    }

    override suspend fun delete(reminderId: String) = dao.delete(reminderId)

    override suspend fun snooze(reminderId: String, untilMillis: Long) {
        TTLog.i(TTLog.REMINDER) { "snooze $reminderId until $untilMillis" }
        dao.snooze(reminderId, untilMillis)
    }

    override suspend fun markFired(reminderId: String, atMillis: Long) =
        dao.markFired(reminderId, atMillis)

    override suspend fun reschedule(plantId: String, nextDueAtMillis: Long) {
        TTLog.i(TTLog.REMINDER) { "reschedule plant=$plantId to $nextDueAtMillis" }
        dao.reschedule(plantId, nextDueAtMillis)
    }

    /**
     * Bulk-clear. Pushes everything overdue out by one default interval rather
     * than recording a watering that did not happen - the point is to clear the
     * pile without lying about what you did.
     */
    override suspend fun clearAllOverdue(nowMillis: Long) =
        dao.clearOverdue(nowMillis, nowMillis + DEFAULT_CHECK_INTERVAL_DAYS * 86_400_000L)
}
