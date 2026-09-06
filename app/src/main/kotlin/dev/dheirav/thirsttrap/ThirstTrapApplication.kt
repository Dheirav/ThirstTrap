package dev.dheirav.thirsttrap

import android.app.Application
import dev.dheirav.thirsttrap.m0.ReminderNotifier

class ThirstTrapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderNotifier.createChannels(this)
    }
}
