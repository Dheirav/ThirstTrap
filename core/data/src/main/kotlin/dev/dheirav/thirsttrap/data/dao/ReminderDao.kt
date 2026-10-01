package dev.dheirav.thirsttrap.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.dheirav.thirsttrap.data.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY next_due_at")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE plant_id = :plantId ORDER BY next_due_at")
    fun observeForPlant(plantId: String): Flow<List<ReminderEntity>>

    @Query(
        """
        SELECT r.* FROM reminders r
        JOIN plants p ON p.id = r.plant_id
        WHERE r.enabled = 1
          AND r.next_due_at <= :now
          AND (r.snoozed_until IS NULL OR r.snoozed_until <= :now)
          -- The plant's own state, not just the reminder's flag. This used to
          -- trust `enabled`, which meant it depended on something having
          -- remembered to switch the flag off at some point in the past, and
          -- one thing did not: un-archiving a plant re-enabled its reminders
          -- regardless of whether the plant was dead. A dead flax cup was
          -- still being asked about weeks later.
          --
          -- Checking the status here is the version that cannot drift. There
          -- is no repair needed for the row that was already wrong, because
          -- the query stops returning it.
          AND p.status NOT IN ('DEAD', 'GIVEN_AWAY')
        ORDER BY r.next_due_at
        """,
    )
    suspend fun dueNow(now: Long): List<ReminderEntity>

    @Query("SELECT created_at FROM reminders WHERE id = :id")
    suspend fun createdAtOf(id: String): Long?

    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun delete(reminderId: String)

    @Query("UPDATE reminders SET snoozed_until = :until WHERE id = :reminderId")
    suspend fun snooze(reminderId: String, until: Long)

    @Query("UPDATE reminders SET last_fired_at = :at WHERE id = :reminderId")
    suspend fun markFired(reminderId: String, at: Long)

    @Query("UPDATE reminders SET enabled = :enabled WHERE plant_id = :plantId")
    suspend fun setEnabledForPlant(plantId: String, enabled: Boolean)

    @Query("UPDATE reminders SET next_due_at = :nextDue, snoozed_until = NULL WHERE plant_id = :plantId")
    suspend fun reschedule(plantId: String, nextDue: Long)

    @Query(
        """
        UPDATE reminders SET next_due_at = :nextDue, snoozed_until = NULL
        WHERE enabled = 1 AND next_due_at <= :now
        """,
    )
    suspend fun clearOverdue(now: Long, nextDue: Long)
}
