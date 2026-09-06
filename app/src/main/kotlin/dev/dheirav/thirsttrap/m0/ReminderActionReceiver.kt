package dev.dheirav.thirsttrap.m0

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Handles the notification's three actions from the shade, so the common case
 * is one tap without opening the app at all.
 *
 * M0 writes to the in-memory fake and toasts. In M1 this delegates to a
 * short-lived CoroutineWorker — a database write must not happen on the
 * receiver's main thread.
 */
class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val plantId = intent.getStringExtra(EXTRA_PLANT_ID) ?: return
        val name = FakeData.nameOf(plantId)

        // The confirmation for restraint is as warm as the one for watering.
        // If "Watered" felt rewarding and "Still wet" felt like a dismissal,
        // the app would be teaching the user that watering is the right answer
        // - which is the exact failure the weight method exists to avoid.
        val message = when (intent.action) {
            ACTION_WATERED -> {
                FakeData.logWatered(plantId)
                "Logged - $name watered"
            }
            ACTION_STILL_WET -> {
                FakeData.logChecked(plantId, stillWet = true)
                "Good call - checked, not thirsty yet"
            }
            ACTION_SNOOZE -> "Snoozed $name for a day"
            else -> return
        }

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        ReminderNotifier.dismiss(context, plantId)
    }

    companion object {
        const val ACTION_WATERED = "dev.dheirav.thirsttrap.WATERED"
        const val ACTION_STILL_WET = "dev.dheirav.thirsttrap.STILL_WET"
        const val ACTION_SNOOZE = "dev.dheirav.thirsttrap.SNOOZE"
        const val EXTRA_PLANT_ID = "plant_id"
    }
}
