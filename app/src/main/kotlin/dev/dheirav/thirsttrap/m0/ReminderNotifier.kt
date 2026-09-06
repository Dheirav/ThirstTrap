package dev.dheirav.thirsttrap.m0

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.dheirav.thirsttrap.R

/**
 * M0 prototype of the reminder, built to answer the second hard UX problem:
 * does "checked, not needed yet" feel as good to tap as "watered"?
 *
 * Three separate channels so the user can tune each independently rather than
 * muting the lot. Health alerts are LOW on purpose: informational, never buzz.
 */
object ReminderNotifier {

    const val CHANNEL_CHECKS = "watering_checks"
    const val CHANNEL_TASKS = "task_reminders"
    const val CHANNEL_HEALTH = "health_alerts"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CHECKS, "Watering checks", NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "A nudge to lift the pot and see how it feels." },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TASKS, "Task reminders", NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "One-off jobs you asked to be reminded about." },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_HEALTH, "Plant health alerts", NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Quiet notes when a pot's drying rate changes." },
        )
    }

    /**
     * One notification per plant, ever. The id is derived from the plant, so a
     * fresh reminder *replaces* the previous one instead of stacking. A plant
     * overdue three days running produces one notification, not three — the
     * requirements call this "no guilt stack".
     */
    private fun notificationId(plantId: String): Int = plantId.hashCode()

    fun showCheckReminder(context: Context, plantId: String, daysSince: Int) {
        val name = FakeData.nameOf(plantId)

        // Asks the user to assess. Never instructs them to water.
        val title = "Time to check the $name"
        val body = if (daysSince > 0) {
            "It's been $daysSince days since you checked. Lift the pot - does it feel light?"
        } else {
            "Lift the pot - does it feel light?"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_CHECKS)
            .setSmallIcon(R.drawable.ic_stat_drop)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            // Both answers are plain actions of equal weight. Neither is styled
            // as the primary one, because "still wet" is just as correct.
            .addAction(0, "Watered", action(context, plantId, ReminderActionReceiver.ACTION_WATERED))
            .addAction(0, "Still wet", action(context, plantId, ReminderActionReceiver.ACTION_STILL_WET))
            .addAction(0, "Snooze 1 day", action(context, plantId, ReminderActionReceiver.ACTION_SNOOZE))
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            try {
                NotificationManagerCompat.from(context)
                    .notify(notificationId(plantId), notification)
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS revoked between the check and the post.
                // The app stays fully usable; the due list carries the same
                // information in-app.
            }
        }
    }

    fun dismiss(context: Context, plantId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(plantId))
    }

    private fun action(context: Context, plantId: String, what: String): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = what
            putExtra(ReminderActionReceiver.EXTRA_PLANT_ID, plantId)
        }
        return PendingIntent.getBroadcast(
            context,
            (plantId + what).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
