package dev.dheirav.thirsttrap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import dev.dheirav.thirsttrap.feature.dashboard.DashboardScreen
import dev.dheirav.thirsttrap.feature.plantedit.PlantEditScreen
import dev.dheirav.thirsttrap.navigation.Routes
import dev.dheirav.thirsttrap.ui.ThirstTrapTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* never gated on */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThirstTrapTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = Routes.DASHBOARD) {
                    composable(Routes.DASHBOARD) {
                        DashboardScreen(
                            onAddPlant = { nav.navigate(Routes.plantEdit()) },
                            onEditPlant = { id -> nav.navigate(Routes.plantEdit(id)) },
                        )
                    }
                    composable(
                        route = "${Routes.PLANT_EDIT}?id={id}",
                        arguments = listOf(
                            navArgument("id") { type = NavType.StringType; defaultValue = "" },
                        ),
                    ) {
                        PlantEditScreen(onDone = { nav.popBackStack() })
                    }
                }
                LaunchedEffect(Unit) { ensureNotificationPermission() }
            }
        }
    }

    /**
     * Asked once, never gated on. If denied the app stays completely usable.
     * Step 4 moves this to the moment the first reminder is created.
     */
    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
