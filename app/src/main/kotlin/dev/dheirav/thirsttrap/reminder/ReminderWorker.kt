package dev.dheirav.thirsttrap.reminder

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.dheirav.thirsttrap.domain.calendarDaysAgo
import dev.dheirav.thirsttrap.domain.containerGroupKey
import dev.dheirav.thirsttrap.domain.potDisplayName
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.ReminderRepository
import kotlinx.coroutines.flow.first

/**
 * The daily sweep. Asks the database what is due and posts one notification per
 * plant - never more than one per plant, ever, and never more than one per pot.
 *
 * The pot part matters once plants share a container. Two plants in one jar had
 * two reminders on two unrelated schedules, so the same jar asked to be
 * assessed twice, and the second ask was about a pot the first one had already
 * dealt with. They are collapsed here rather than in the data: the rows stay
 * one per plant, which keeps the planning and the edit screen unchanged, and
 * they fall back into step on their own because computeNextDue measures from
 * the last assessment in the log and a shared check is written to every plant
 * in the pot.
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

        // One group per pot, and a group of one for every plant in its own,
        // which is the ordinary case and stays exactly as it was. Grouping by
        // the reminder's plant id when there is no container keeps those
        // separate rather than collecting every unshared plant into one bucket.
        val groups = due
            .filter { allPlants[it.plantId] != null }
            .groupBy { containerGroupKey(it.plantId, allPlants.getValue(it.plantId).containerId) }

        for ((_, group) in groups) {
            // Stable, so the same pot does not swap which plant it speaks for
            // between sweeps and post as a second notification.
            val members = group.mapNotNull { allPlants[it.plantId] }.sortedBy { it.id }
            val lead = members.first()

            // The longest wait in the pot. Taking the lead plant's own count
            // would under-report whenever the plant that happens to sort first
            // was assessed more recently than its neighbour.
            val daysSince = members.maxOf { plant ->
                val lastAssessed = plants.observeEvents(plant.id).first().firstOrNull {
                    it.type == CareEventType.WATERED || it.type == CareEventType.CHECKED
                }?.timestampMillis
                // Calendar days. Dividing the elapsed time made a check at
                // 22:00 read as "0 days" at 09:00 the next morning, which the
                // notification then rendered as "It's been 0 days since you
                // checked". D38 fixed the dashboard and not this.
                lastAssessed?.let { calendarDaysAgo(now, it, tzOffsetMinutesAt(now)) } ?: 0
            }

            val name = potDisplayName(members.map { it.name })

            ReminderNotifier.showCheckReminder(applicationContext, lead.id, name, daysSince)
            // Every reminder in the pot, not just the one that spoke. Marking
            // only the lead would leave the others due and they would post
            // again on the next sweep, which is the bug this is here to fix.
            group.forEach { reminders.markFired(it.id, now) }
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
