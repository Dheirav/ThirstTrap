package dev.dheirav.thirsttrap.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.dheirav.thirsttrap.R

/**
 * Three channels so each can be tuned separately rather than muted wholesale.
 * Health alerts are LOW deliberately: informational, never buzz.
 */
object ReminderNotifier {

    const val CHANNEL_CHECKS = "watering_checks"
    const val CHANNEL_TASKS = "task_reminders"
    const val CHANNEL_HEALTH = "health_alerts"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CHECKS, "Watering checks", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "A nudge to lift the pot and see how it feels." },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_TASKS, "Task reminders", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "One-off jobs you asked to be reminded about." },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_HEALTH, "Plant health alerts", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Quiet notes when a pot's drying rate changes." },
        )
    }

    /**
     * One notification per plant, ever. The id derives from the plant, so a new
     * reminder REPLACES the previous one rather than stacking. A plant overdue
     * three days running yields one notification - the "no guilt stack" rule.
     */
    private fun notificationId(plantId: String): Int = plantId.hashCode()

    fun showCheckReminder(context: Context, plantId: String, plantName: String, daysSince: Int) {
        // Asks the user to assess. Never instructs them to water.
        val title = "Time to check the $plantName"
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
            // Tapping the body opens the plant it is about. Without this the
            // notification is inert AND setAutoCancel has nothing to cancel on.
            .setContentIntent(openPlant(context, plantId))
            // Equal weight. Neither answer is styled as the primary one, because
            // "still wet" is just as correct as "watered".
            .addAction(0, "Watered", action(context, plantId, plantName, ReminderActionReceiver.ACTION_WATERED))
            .addAction(0, "Still wet", action(context, plantId, plantName, ReminderActionReceiver.ACTION_STILL_WET))
            .addAction(0, "Snooze 1 day", action(context, plantId, plantName, ReminderActionReceiver.ACTION_SNOOZE))
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            try {
                NotificationManagerCompat.from(context).notify(notificationId(plantId), notification)
            } catch (_: SecurityException) {
                // Permission revoked between the check and the post. The app
                // stays usable; the dashboard carries the same information.
            }
        }
    }

    fun dismiss(context: Context, plantId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(plantId))
    }

    /** Deep-links into the plant, so the reminder leads somewhere. */
    private fun openPlant(context: Context, plantId: String): PendingIntent {
        val intent = Intent(
            Intent.ACTION_VIEW,
            android.net.Uri.parse("thirsttrap://plant/$plantId"),
            context,
            Class.forName("dev.dheirav.thirsttrap.MainActivity"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        return PendingIntent.getActivity(
            context,
            plantId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun action(context: Context, plantId: String, plantName: String, what: String): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = what
            putExtra(ReminderActionReceiver.EXTRA_PLANT_ID, plantId)
            putExtra(ReminderActionReceiver.EXTRA_PLANT_NAME, plantName)
        }
        return PendingIntent.getBroadcast(
            context,
            (plantId + what).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
