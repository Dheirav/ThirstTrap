package dev.dheirav.thirsttrap

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dev.dheirav.thirsttrap.reminder.ReminderBackfill
import dev.dheirav.thirsttrap.reminder.ReminderNotifier
import dev.dheirav.thirsttrap.domain.SettingsRepository
import dev.dheirav.thirsttrap.reminder.ReminderScheduler
import dev.dheirav.thirsttrap.widget.WateringWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ThirstTrapApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var backfill: ReminderBackfill
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var plants: dev.dheirav.thirsttrap.domain.PlantRepository

    /** WorkManager is initialised on demand via this, not by androidx.startup. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        ReminderNotifier.createChannels(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            // KEEP policy, so this is safe to call on every launch.
            scheduler.scheduleDailySweep(this@ThirstTrapApplication, settings.settings.first().reminderHour)
            // Idempotent; covers plants and photos that predate their tables.
            backfill.run()
        }

        // Keep the widget honest. Glance does not observe a flow; something has
        // to tell it the world moved, and the alternative is a widget that is
        // up to half an hour stale after you water something, which teaches
        // people not to trust it. Distinct-until-changed on the names and
        // reasons, so an unrelated write does not redraw it.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            plants.observeDashboard { System.currentTimeMillis() }
                .map { list ->
                    list.joinToString("|") { "${it.plant.id}:${it.reminderDueMillis}:${it.prediction}" }
                }
                .distinctUntilChanged()
                .collect {
                    runCatching { WateringWidget().updateAll(this@ThirstTrapApplication) }
                }
        }
    }
}
