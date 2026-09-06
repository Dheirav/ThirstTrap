package dev.dheirav.thirsttrap

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import dev.dheirav.thirsttrap.m0.DashboardScreen
import dev.dheirav.thirsttrap.m0.ReminderNotifier
import dev.dheirav.thirsttrap.ui.ThirstTrapTheme

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* see below */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ThirstTrapTheme {
                DashboardScreen(
                    onSendTestReminder = { plantId -> scheduleTestReminder(plantId) },
                )
            }
            LaunchedEffect(Unit) { ensureNotificationPermission() }
        }
    }

    /**
     * Asked once, and never gated on. If it is denied the app stays completely
     * usable - the due list carries the same information in-app. M1 moves this
     * to the moment the user creates their first reminder rather than launch.
     */
    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /**
     * The debug affordance from docs/NOTIFICATIONS.md section 7: without a way to
     * fire a reminder on demand, every notification test costs a day of waiting.
     * Ten seconds is long enough to background the app and see it arrive in the
     * shade, which is the case that actually matters.
     */
    private fun scheduleTestReminder(plantId: String) {
        Handler(Looper.getMainLooper()).postDelayed({
            ReminderNotifier.showCheckReminder(this, plantId, daysSince = 9)
        }, 10_000)
    }
}
