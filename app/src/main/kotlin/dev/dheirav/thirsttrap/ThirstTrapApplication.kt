package dev.dheirav.thirsttrap

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dev.dheirav.thirsttrap.reminder.ReminderBackfill
import dev.dheirav.thirsttrap.reminder.ReminderNotifier
import dev.dheirav.thirsttrap.domain.SettingsRepository
import dev.dheirav.thirsttrap.reminder.ReminderScheduler
import kotlinx.coroutines.flow.first
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
    }
}
