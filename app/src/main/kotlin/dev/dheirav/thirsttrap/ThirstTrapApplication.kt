package dev.dheirav.thirsttrap

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.dheirav.thirsttrap.reminder.ReminderNotifier

@HiltAndroidApp
class ThirstTrapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderNotifier.createChannels(this)
    }
}
