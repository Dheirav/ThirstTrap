package dev.dheirav.thirsttrap.feature.help

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.reminder.ReminderNotifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersHelpViewModel @Inject constructor(
    private val plants: PlantRepository,
) : ViewModel() {

    /**
     * Fires immediately rather than on a delay: the user is standing here
     * waiting for it, and a test they have to wait for is a test they skip.
     */
    fun fireTestReminder(context: Context) {
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
}
