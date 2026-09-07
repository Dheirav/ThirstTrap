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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.dheirav.thirsttrap.feature.dashboard.DashboardScreen
import dev.dheirav.thirsttrap.feature.due.DueScreen
import dev.dheirav.thirsttrap.feature.help.RemindersHelpScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.feature.debug.DebugScreen
import androidx.compose.material.icons.filled.Settings
import dev.dheirav.thirsttrap.feature.backup.BackupScreen
import dev.dheirav.thirsttrap.feature.settings.SettingsScreen
import dev.dheirav.thirsttrap.feature.compare.CompareScreen
import dev.dheirav.thirsttrap.feature.logevent.LogEventScreen
import dev.dheirav.thirsttrap.feature.plantdetail.PlantDetailScreen
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
            val mainViewModel: MainViewModel = hiltViewModel()
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()

            ThirstTrapTheme(dynamicColor = settings.dynamicColor) {
                val nav = rememberNavController()
                val backStack by nav.currentBackStackEntryAsState()
                val route = backStack?.destination?.route
                val showBar = route == Routes.DASHBOARD || route == Routes.DUE ||
                    route == Routes.SETTINGS

                Scaffold(
                    bottomBar = {
                        if (showBar) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = route == Routes.DASHBOARD,
                                    onClick = { nav.navigate(Routes.DASHBOARD) { popUpTo(Routes.DASHBOARD) { inclusive = true } } },
                                    icon = { Icon(Icons.Filled.Yard, contentDescription = null) },
                                    label = { Text("Plants") },
                                )
                                NavigationBarItem(
                                    selected = route == Routes.DUE,
                                    onClick = { nav.navigate(Routes.DUE) { launchSingleTop = true } },
                                    icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                                    label = { Text("Due") },
                                )
                                NavigationBarItem(
                                    selected = route == Routes.SETTINGS,
                                    onClick = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                    label = { Text("Settings") },
                                )
                            }
                        }
                    },
                ) { barPadding ->
                    NavHost(
                        navController = nav,
                        startDestination = Routes.DASHBOARD,
                        modifier = Modifier.padding(barPadding),
                    ) {
                        composable(Routes.DASHBOARD) {
                            DashboardScreen(
                                onAddPlant = { nav.navigate(Routes.plantEdit()) },
                                onEditPlant = { id -> nav.navigate(Routes.plantEdit(id)) },
                                onOpenPlant = { id -> nav.navigate(Routes.plantDetail(id)) },
                                onLogMore = { id -> nav.navigate(Routes.logEvent(id)) },
                            )
                        }
                        composable(Routes.DUE) {
                            DueScreen(
                                onOpenHelp = { nav.navigate(Routes.HELP_REMINDERS) },
                                onOpenDebug = { nav.navigate(Routes.DEBUG) },
                            )
                        }
                        composable(Routes.HELP_REMINDERS) {
                            RemindersHelpScreen(onBack = { nav.popBackStack() })
                        }
                        composable(
                            route = "${Routes.LOG_EVENT}/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) {
                            LogEventScreen(onDone = { nav.popBackStack() })
                        }
                        composable(
                            route = "${Routes.COMPARE}/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) {
                            CompareScreen(onBack = { nav.popBackStack() })
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                onOpenBackup = { nav.navigate(Routes.BACKUP) },
                                onOpenHelp = { nav.navigate(Routes.HELP_REMINDERS) },
                                onOpenDebug = { nav.navigate(Routes.DEBUG) },
                            )
                        }
                        composable(Routes.BACKUP) {
                            BackupScreen(onBack = { nav.popBackStack() })
                        }
                        composable(Routes.DEBUG) {
                            DebugScreen(onBack = { nav.popBackStack() })
                        }
                        composable(
                            route = "${Routes.PLANT_DETAIL}/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) {
                            PlantDetailScreen(
                                onBack = { nav.popBackStack() },
                                onEdit = { id -> nav.navigate(Routes.plantEdit(id)) },
                                onCompare = { id -> nav.navigate(Routes.compare(id)) },
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
