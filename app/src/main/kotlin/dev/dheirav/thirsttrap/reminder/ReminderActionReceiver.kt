package dev.dheirav.thirsttrap.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import dev.dheirav.thirsttrap.domain.CareEvent
import dev.dheirav.thirsttrap.domain.CareEventType
import dev.dheirav.thirsttrap.domain.CheckResult
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.ReminderRepository
import dev.dheirav.thirsttrap.domain.newId
import dev.dheirav.thirsttrap.domain.tzOffsetMinutesAt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The notification's three actions, handled from the shade so the common case
 * is one tap without opening the app.
 *
 * The database write runs off the main thread inside goAsync(), because a
 * receiver's onReceive is on the main thread and Room will refuse to be called
 * there.
 */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: PlantRepository
    @Inject lateinit var reminders: ReminderRepository

    override fun onReceive(context: Context, intent: Intent) {
        val plantId = intent.getStringExtra(EXTRA_PLANT_ID) ?: return
        val plantName = intent.getStringExtra(EXTRA_PLANT_NAME) ?: "that plant"
        Log.i(TAG, "onReceive action=${intent.action} plant=$plantId")

        val type = when (intent.action) {
            ACTION_WATERED -> CareEventType.WATERED
            ACTION_STILL_WET -> CareEventType.CHECKED
            ACTION_SNOOZE -> null
            else -> return
        }

        // The confirmation for restraint is as warm as the one for watering. If
        // "Watered" felt rewarding and "Still wet" felt like a dismissal, the
        // app would be teaching that watering is the right answer - the exact
        // failure the weight method exists to avoid.
        val message = when (intent.action) {
            ACTION_WATERED -> "Logged - $plantName watered"
            ACTION_STILL_WET -> "Good call - checked, not thirsty yet"
            else -> "Snoozed $plantName for a day"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        ReminderNotifier.dismiss(context, plantId)

        if (type == null) return
        val pending = goAsync()
        val now = System.currentTimeMillis()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.logEvent(
                    CareEvent(
                        id = newId(),
                        plantId = plantId,
                        timestampMillis = now,
                        tzOffsetMinutes = tzOffsetMinutesAt(now),
                        type = type,
                        checkResult = if (type == CareEventType.CHECKED) CheckResult.STILL_HEAVY else null,
                    ),
                )
                // Tapping "Watered" in the shade is the least friction the app
                // offers, and it was the one path that logged the event without
                // replanning, so the same notification came back on the old
                // schedule as if nothing had been done.
                reminders.rescheduleFromModel(plantId, now)
                Log.i(TAG, "recorded ${type.name} for $plantId")
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TAG = "TTReminder"
        const val ACTION_WATERED = "dev.dheirav.thirsttrap.WATERED"
        const val ACTION_STILL_WET = "dev.dheirav.thirsttrap.STILL_WET"
        const val ACTION_SNOOZE = "dev.dheirav.thirsttrap.SNOOZE"
        const val EXTRA_PLANT_ID = "plant_id"
        const val EXTRA_PLANT_NAME = "plant_name"
    }
}
