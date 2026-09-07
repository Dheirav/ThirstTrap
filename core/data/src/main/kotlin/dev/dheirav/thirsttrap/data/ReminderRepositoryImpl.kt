package dev.dheirav.thirsttrap.data

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

private fun ReminderEntity.toDomain() = Reminder(
    id = id,
    plantId = plantId,
    kind = if (kind.equals("TASK", true)) ReminderKind.TASK else ReminderKind.CHECK,
    title = title,
    intervalDays = intervalDays,
    nextDueAtMillis = nextDueAt,
    enabled = enabled,
    snoozedUntilMillis = snoozedUntil,
    lastFiredAtMillis = lastFiredAt,
)

private fun Reminder.toEntity(createdAt: Long) = ReminderEntity(
    id = id,
    plantId = plantId,
    kind = kind.name,
    title = title,
    intervalDays = intervalDays,
    nextDueAt = nextDueAtMillis,
    enabled = enabled,
    snoozedUntil = snoozedUntilMillis,
    lastFiredAt = lastFiredAtMillis,
    createdAt = createdAt,
)

@Singleton
class ReminderRepositoryImpl @Inject constructor(
    private val dao: ReminderDao,
) : ReminderRepository {

    override fun observeReminders(): Flow<List<Reminder>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeForPlant(plantId: String): Flow<List<Reminder>> =
        dao.observeForPlant(plantId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun dueNow(nowMillis: Long): List<Reminder> =
        dao.dueNow(nowMillis).map { it.toDomain() }

    override suspend fun upsert(reminder: Reminder) {
        TTLog.i(TTLog.REMINDER) {
            "upsert reminder ${reminder.id} plant=${reminder.plantId} due=${reminder.nextDueAtMillis}"
        }
        dao.upsert(reminder.toEntity(System.currentTimeMillis()))
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
