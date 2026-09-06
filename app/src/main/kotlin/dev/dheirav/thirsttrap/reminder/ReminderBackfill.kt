package dev.dheirav.thirsttrap.reminder

import android.util.Log
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.Reminder
import dev.dheirav.thirsttrap.domain.ReminderKind
import dev.dheirav.thirsttrap.domain.ReminderRepository
import dev.dheirav.thirsttrap.domain.computeNextDue
import dev.dheirav.thirsttrap.domain.newId
import dev.dheirav.thirsttrap.domain.resolveIntervalDays
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gives a check reminder to any plant that has none.
 *
 * Needed because plants added before reminders existed would otherwise stay
 * invisible in the Due list forever - and it also covers a reminder deleted by
 * accident, or a restored backup that predates this table. Idempotent, so it is
 * safe to run on every launch.
 */
@Singleton
class ReminderBackfill @Inject constructor(
    private val plants: PlantRepository,
    private val reminders: ReminderRepository,
) {
    suspend fun run() {
        val allPlants = plants.observePlants().first()
        val covered = reminders.observeReminders().first().map { it.plantId }.toSet()
        val now = System.currentTimeMillis()

        val missing = allPlants.filterNot { it.id in covered }
        if (missing.isEmpty()) return
        Log.i(TAG, "backfilling reminders for ${missing.size} plant(s)")

        for (plant in missing) {
            val events = plants.observeEvents(plant.id).first()
            val lastAssessed = events.firstOrNull {
                it.type == CareEventType.WATERED || it.type == CareEventType.CHECKED
            }?.timestampMillis

            reminders.upsert(
                Reminder(
                    id = newId(),
                    plantId = plant.id,
                    kind = ReminderKind.CHECK,
                    nextDueAtMillis = computeNextDue(
                        lastAssessed,
                        resolveIntervalDays(null, null, null),
                        now,
                    ),
                ),
            )
        }
    }

    private companion object {
        const val TAG = "TTReminder"
    }
}
