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

    /**
     * Reschedules using everything the app knows about this plant.
     *
     * [resolveIntervalDays] documents a priority order - explicit setting, then
     * the weight prediction, then the logged average, then a week - and it was
     * being called from four places, three of which passed nulls for the
     * prediction and the average. So the reminder tracked the pot only if you
     * happened to open the weight screen, and behaved as a flat weekly calendar
     * everywhere else: exactly the thing the app exists to replace. The logged
     * average was never passed by anybody, so that branch had never run outside
     * its unit tests.
     *
     * Gathering the inputs here rather than at each call site is the actual
     * fix. A caller cannot forget to pass something it never handles.
     */
    suspend fun rescheduleFromModel(plantId: String, nowMillis: Long)

    /** Clears every overdue reminder at once, without recording a watering. */
    suspend fun clearAllOverdue(nowMillis: Long)
}
