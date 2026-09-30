package dev.dheirav.thirsttrap.feature.help

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.reminder.ReminderNotifier
import dev.dheirav.thirsttrap.reminder.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersHelpViewModel @Inject constructor(
    private val plants: PlantRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    /**
     * Posts a notification directly, in this process.
     *
     * This exercises the permission and the channel and nothing else, so it can
     * only answer "would a notification be shown", never "is background work
     * allowed". The screen used to claim the second, which is a false all-clear
     * on the one question the page exists to answer, and somebody acting on it
     * leaves Autostart off.
     */
    fun fireTestNotification(context: Context) {
        viewModelScope.launch {
            val plant = plants.observePlants().first().firstOrNull()
            ReminderNotifier.showCheckReminder(
                context = context,
                plantId = plant?.id ?: "test",
                plantName = plant?.name ?: "test plant",
                daysSince = 0,
            )
        }
    }

    /**
     * Runs the real sweep through WorkManager, which is the only thing that
     * actually tests whether the OEM is letting background work through.
     *
     * Nothing arrives if no plant is due, which is the honest outcome and why
     * the screen says so rather than promising a notification.
     */
    fun runTheRealSweep(context: Context) = scheduler.runSweepNow(context)
}
