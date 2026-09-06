package dev.dheirav.thirsttrap.domain

import kotlinx.coroutines.flow.Flow

interface ReminderRepository {

    fun observeReminders(): Flow<List<Reminder>>

    fun observeForPlant(plantId: String): Flow<List<Reminder>>

    /** Everything currently due, for the in-app list and the daily worker. */
    suspend fun dueNow(nowMillis: Long): List<Reminder>

    suspend fun upsert(reminder: Reminder)

    suspend fun delete(reminderId: String)

    suspend fun snooze(reminderId: String, untilMillis: Long)

    suspend fun markFired(reminderId: String, atMillis: Long)

    /** Pushes the next due date out after an assessment. */
    suspend fun reschedule(plantId: String, nextDueAtMillis: Long)

    /** Clears every overdue reminder at once, without recording a watering. */
    suspend fun clearAllOverdue(nowMillis: Long)
}
