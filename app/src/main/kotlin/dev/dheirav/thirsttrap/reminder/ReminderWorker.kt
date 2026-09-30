package dev.dheirav.thirsttrap.reminder

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.dheirav.thirsttrap.domain.calendarDaysAgo
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
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

            // Calendar days. Dividing the elapsed time made a check at 22:00
            // read as "0 days" at 09:00 the next morning, which the
            // notification then rendered as "It's been 0 days since you
            // checked". D38 fixed the dashboard and not this.
            val daysSince = lastAssessed
                ?.let { calendarDaysAgo(now, it, tzOffsetMinutesAt(now)) }
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
