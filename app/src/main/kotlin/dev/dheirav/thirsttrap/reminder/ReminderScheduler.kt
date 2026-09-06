package dev.dheirav.thirsttrap.reminder

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the daily sweep that posts whatever is due.
 *
 * **Inexact, deliberately** - handover decision D4. SCHEDULE_EXACT_ALARM is
 * denied by default on Android 13+, and USE_EXACT_ALARM is Play-restricted to
 * alarm and calendar apps, which this is not. A watering *check* does not need
 * to land at 09:00:00; "some time around nine" is the whole requirement.
 *
 * What that buys: no permission prompt beyond POST_NOTIFICATIONS, reboot
 * survival for free (WorkManager persists its own queue), and Doze-friendly
 * batching.
 *
 * The honest cost: periodic work fires within a window when the device is
 * awake, not at a precise instant, and Doze can defer it. Acceptable here, and
 * said plainly in the in-app help rather than glossed over.
 */
@Singleton
class ReminderScheduler @Inject constructor() {

    fun scheduleDailySweep(context: Context, hourOfDay: Int = DEFAULT_HOUR) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNext(hourOfDay), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().build())
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            // KEEP, not UPDATE: re-enqueuing on every app start would reset the
            // period each time and the sweep might never actually run.
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /** Debug affordance - without it every notification test costs a day. */
    fun runSweepNow(context: Context) {
        WorkManager.getInstance(context).enqueue(
            androidx.work.OneTimeWorkRequestBuilder<ReminderWorker>().addTag(WORK_TAG).build(),
        )
    }

    private fun millisUntilNext(hourOfDay: Int): Long {
        val now = Calendar.getInstance()
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    companion object {
        const val WORK_NAME = "thirsttrap-daily-reminder-sweep"
        const val WORK_TAG = "reminder-sweep"
        const val DEFAULT_HOUR = 9
    }
}
