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
        SELECT * FROM reminders
        WHERE enabled = 1
          AND next_due_at <= :now
          AND (snoozed_until IS NULL OR snoozed_until <= :now)
        ORDER BY next_due_at
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
