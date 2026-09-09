package dev.dheirav.thirsttrap.reminder

import android.util.Log
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.data.PhotoRepositoryImpl
import dev.dheirav.thirsttrap.domain.PhotoRepository
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
 * Gives a check reminder to any plant that has none, then replans them all.
 *
 * The backfill half is needed because plants added before reminders existed
 * would otherwise stay invisible in the Due list forever, and it also covers a
 * reminder deleted by accident or a restored backup that predates this table.
 *
 * The replan half matters more. A due date is a prediction, so it goes stale on
 * its own: the drying curve changes as readings arrive, and a plant sitting at
 * a default week is one nobody has watered recently, which is exactly when the
 * default is least likely to be right. Replanning every plant on launch keeps
 * the Due list honest without waiting for the user to touch each plant. It is
 * idempotent, and it leaves an explicitly chosen interval alone.
 */
@Singleton
class ReminderBackfill @Inject constructor(
    private val plants: PlantRepository,
    private val reminders: ReminderRepository,
    private val photos: PhotoRepository,
) {
    suspend fun run() {
        (photos as? PhotoRepositoryImpl)?.backfillPhotoEvents()

        val allPlants = plants.observePlants().first()
        val covered = reminders.observeReminders().first().map { it.plantId }.toSet()
        val now = System.currentTimeMillis()

        val missing = allPlants.filterNot { it.id in covered }
        if (missing.isNotEmpty()) {
            Log.i(TAG, "backfilling reminders for ${missing.size} plant(s)")
        }

        for (plant in missing) {
            val events = plants.observeEvents(plant.id).first()
            val lastAssessed = events.firstOrNull {
                it.type == CareEventType.WATERED || it.type == CareEventType.CHECKED
            }?.timestampMillis

            // Default first, so the row exists, then planned from whatever the
            // plant's own history and readings say. A backfilled plant is by
            // definition one that has been around a while, so it is exactly the
            // case where a flat week is least likely to be right.
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

        for (plant in allPlants) {
            reminders.rescheduleFromModel(plant.id, now)
        }
    }

    private companion object {
        const val TAG = "TTReminder"
    }
}
