package dev.dheirav.thirsttrap.reminder

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.ReminderRepository
import kotlinx.coroutines.flow.first

/**
 * The daily sweep. Asks the database what is due and posts one notification per
 * plant - never more than one per plant, ever.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val reminders: ReminderRepository,
    private val plants: PlantRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val now = System.currentTimeMillis()
        val due = reminders.dueNow(now)
        Log.i(TAG, "sweep: ${due.size} due")

        val allPlants = plants.observePlants().first().associateBy { it.id }

        for (reminder in due) {
            val plant = allPlants[reminder.plantId] ?: continue
            val events = plants.observeEvents(plant.id).first()
            val lastAssessed = events.firstOrNull {
                it.type == CareEventType.WATERED || it.type == CareEventType.CHECKED
            }?.timestampMillis

            val daysSince = lastAssessed
                ?.let { ((now - it) / 86_400_000L).toInt() }
                ?: 0

            ReminderNotifier.showCheckReminder(applicationContext, plant.id, plant.name, daysSince)
            reminders.markFired(reminder.id, now)
        }
        Result.success()
    } catch (t: Throwable) {
        Log.e(TAG, "sweep failed", t)
        Result.retry()
    }

    companion object {
        private const val TAG = "TTReminder"
    }
}
