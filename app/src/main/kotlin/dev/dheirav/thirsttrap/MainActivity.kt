package dev.dheirav.thirsttrap

import dev.dheirav.thirsttrap.ui.MenuLabels
import dev.dheirav.thirsttrap.feature.ambient.AmbientScreen
import dev.dheirav.thirsttrap.feature.fertilizer.FertilizerScreen
import dev.dheirav.thirsttrap.feature.help.HowItWorksScreen
import dev.dheirav.thirsttrap.feature.help.TroubleshootScreen
import dev.dheirav.thirsttrap.feature.help.BackupHelpScreen
import dev.dheirav.thirsttrap.feature.help.WayfindingScreen
import dev.dheirav.thirsttrap.feature.help.WhyNoPredictionScreen
import dev.dheirav.thirsttrap.feature.intro.IntroScreen
import dev.dheirav.thirsttrap.feature.more.MoreScreen
import dev.dheirav.thirsttrap.feature.locations.LocationsScreen
import dev.dheirav.thirsttrap.feature.weighing.WeighingScreen
import dev.dheirav.thirsttrap.feature.stats.StatsScreen
import dev.dheirav.thirsttrap.ui.AppIcons
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.dheirav.thirsttrap.ui.Rule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.dheirav.thirsttrap.feature.dashboard.DashboardScreen
import dev.dheirav.thirsttrap.feature.due.DueScreen
import dev.dheirav.thirsttrap.feature.help.RemindersHelpScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.dheirav.thirsttrap.feature.debug.DebugScreen
import dev.dheirav.thirsttrap.feature.backup.BackupScreen
import dev.dheirav.thirsttrap.feature.light.LightMeterScreen
import dev.dheirav.thirsttrap.feature.care.CareScreen
import dev.dheirav.thirsttrap.feature.diagnose.DiagnoseScreen
import dev.dheirav.thirsttrap.feature.postmortem.PostMortemScreen
import dev.dheirav.thirsttrap.feature.qr.StickerScreen
import dev.dheirav.thirsttrap.feature.propagation.PropagationScreen
import dev.dheirav.thirsttrap.feature.settings.SettingsScreen
import dev.dheirav.thirsttrap.feature.weight.ScaleHelpScreen
import dev.dheirav.thirsttrap.feature.weight.WeightScreen
import dev.dheirav.thirsttrap.feature.logevent.LogEventScreen
import dev.dheirav.thirsttrap.feature.plantdetail.PlantDetailScreen
import dev.dheirav.thirsttrap.feature.plantedit.PlantEditScreen
import dev.dheirav.thirsttrap.navigation.Routes
import dev.dheirav.thirsttrap.ui.ThirstTrapTheme
import dev.dheirav.thirsttrap.ui.grain

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* never gated on */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()

            // targetSdk 35 makes edge-to-edge mandatory, so the app draws behind
            // the status bar - and nothing was telling the system which way to
            // colour its icons. In light mode they stayed white on a #F7FBF3
            // background, which measured 1.0:1. Invisible.
            val dark = isSystemInDarkTheme()
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                    // With three-button navigation the system paints its own
                    // opaque bar and, on API 29+, enforces a contrast scrim over
                    // anything drawn behind it. Both together made the bottom of
                    // every screen a light-grey slab that belonged to no theme.
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        window.isNavigationBarContrastEnforced = false
                    }
                }
            }

            // Nothing is drawn until the stored settings have answered. The
            // alternative is drawing from defaults and correcting a frame later,
            // which is what made the first-run page flash on every launch.
            val loaded = settings ?: return@setContent

            ThirstTrapTheme {
                // The first-run page sits in front of everything rather than
                // being a route, because it is not somewhere you navigate to and
                // there must be no way to reach it again by accident. Read where
                // it is written: D30a found a setting nothing consulted.
                var introDone by remember { mutableStateOf(false) }
                if (!loaded.introSeen && !introDone) {
                    // Grain here too. It was applied to the NavHost modifier
                    // only, and this returns before that, so the first thing
                    // anyone ever saw of this app was the one screen rendering
                    // on flat colour, which Grain.kt calls a void.
                    Box(Modifier.fillMaxSize().grain()) {
                        IntroScreen(onDone = { introDone = true })
                    }
                    return@ThirstTrapTheme
                }

                val nav = rememberNavController()
                val backStack by nav.currentBackStackEntryAsState()
                val route = backStack?.destination?.route
                val showBar = route == Routes.DASHBOARD || route == Routes.DUE ||
                    route == Routes.SETTINGS

                Scaffold(
                    // No top bar here, but a Scaffold still hands its content
                    // the status-bar inset - and every screen inside has its own
                    // Scaffold whose TopAppBar applies that inset again. Applied
                    // twice it cost ~82dp of dead space above every title. This
                    // Scaffold exists only to place the bottom nav, so it
                    // contributes no insets; NavigationBar handles its own.
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (showBar) {
                            // The bar was the last fully rounded shape in the
                            // app and the only tonal surface nobody chose: the
                            // pill indicator and the elevation tint are both
                            // Material defaults. Selection is now marked the
                            // way every other section break in the app is, with
                            // a rule, and the bar sits on the page rather than
                            // above it.
                            Column {
                                Rule()
                                // One navigation pattern for all three tabs.
                                //
                                // They had three. Plants popped the back stack
                                // to itself inclusively, Due and Settings only
                                // set launchSingleTop, and none of them saved or
                                // restored state. So Plants, Due, Settings, Due
                                // left four entries with Due in twice, and every
                                // return to Plants threw away the list's scroll
                                // position, which is the single cheapest cause
                                // of the app feeling slightly off to use.
                                //
                                // popUpTo the graph's start with saveState, plus
                                // restoreState, is the documented pattern: one
                                // entry per tab, and each tab remembers where it
                                // was. launchSingleTop stops a re-tap stacking a
                                // second copy of the tab you are already on.
                                fun goTab(dest: String) = nav.navigate(dest) {
                                    popUpTo(nav.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp,
                                ) {
                                    NavigationBarItem(
                                        selected = route == Routes.DASHBOARD,
                                        onClick = { goTab(Routes.DASHBOARD) },
                                        icon = { TabMark(route == Routes.DASHBOARD) { Icon(AppIcons.yard, contentDescription = null) } },
                                        label = { Text(MenuLabels.Tab.PLANTS) },
                                        colors = flatTabColors(),
                                    )
                                    NavigationBarItem(
                                        selected = route == Routes.DUE,
                                        onClick = { goTab(Routes.DUE) },
                                        icon = { TabMark(route == Routes.DUE) { Icon(AppIcons.notifications, contentDescription = null) } },
                                        label = { Text(MenuLabels.Tab.DUE) },
                                        colors = flatTabColors(),
                                    )
                                    NavigationBarItem(
                                        selected = route == Routes.SETTINGS,
                                        onClick = { goTab(Routes.SETTINGS) },
                                        icon = { TabMark(route == Routes.SETTINGS) { Icon(AppIcons.settings, contentDescription = null) } },
                                        label = { Text(MenuLabels.Tab.SETTINGS) },
                                        colors = flatTabColors(),
                                    )
                                }
                            }
                        }
                    },
                ) { barPadding ->
                    NavHost(
                        navController = nav,
                        startDestination = Routes.DASHBOARD,
                        // Grain over the whole app rather than per screen: it is
                        // a property of the paper, not of any one page.
                        modifier = Modifier.padding(barPadding).grain(),
                    ) {
                        composable(Routes.DASHBOARD) {
                            DashboardScreen(
                                onAddPlant = { nav.navigate(Routes.plantEdit()) },
                                onEditPlant = { id -> nav.navigate(Routes.plantEdit(id)) },
                                onOpenPlant = { id -> nav.navigate(Routes.plantDetail(id)) },
                                onLogMore = { id -> nav.navigate(Routes.logEvent(id)) },
                                onOpenWeighing = { nav.navigate(Routes.WEIGHING) },
                                onOpenMore = { nav.navigate(Routes.MORE) },
                                onWeighPlant = { id -> nav.navigate(Routes.weight(id)) },
                            )
                        }
                        composable(Routes.MORE) {
                            MoreScreen(
                                advanced = loaded.advancedFeatures,
                                onBack = { nav.popBackStack() },
                                onOpenFeeding = { nav.navigate(Routes.FERTILIZER) },
                                onOpenPropagation = { nav.navigate(Routes.PROPAGATION) },
                                onScanPot = { nav.navigate(Routes.SCAN) },
                                onOpenPlaces = { nav.navigate(Routes.PLACES) },
                                onOpenFigures = { nav.navigate(Routes.STATS) },
                                onOpenExperiments = { nav.navigate(Routes.EXPERIMENTS) },
                            )
                        }
                        composable(Routes.DUE) {
                            DueScreen(
                                onOpenHelp = { nav.navigate(Routes.HELP_REMINDERS) },
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
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                onOpenBackup = { nav.navigate(Routes.BACKUP) },
                                onOpenHowItWorks = { nav.navigate(Routes.HOW_IT_WORKS) },
                                onOpenTroubleshoot = { nav.navigate(Routes.TROUBLESHOOT) },
                                onOpenDebug = { nav.navigate(Routes.DEBUG) },
                            )
                        }
                        composable(
                            route = "${Routes.WHY_NO_DATE}/{reason}",
                            arguments = listOf(navArgument("reason") { type = NavType.StringType }),
                        ) { entry ->
                            val name = entry.arguments?.getString("reason")
                            WhyNoPredictionScreen(
                                asked = dev.dheirav.thirsttrap.domain.SuppressionReason.entries
                                    .firstOrNull { it.name == name },
                                onBack = { nav.popBackStack() },
                            )
                        }
                        composable(Routes.BACKUP_HELP) {
                            BackupHelpScreen(onBack = { nav.popBackStack() })
                        }
                        composable(Routes.WAYFINDING) {
                            WayfindingScreen(onBack = { nav.popBackStack() })
                        }
                        composable(Routes.HOW_IT_WORKS) {
                            HowItWorksScreen(
                                onBack = { nav.popBackStack() },
                                onOpenIntro = { nav.navigate(Routes.INTRO) },
                                onOpenScaleHelp = { nav.navigate(Routes.SCALE_HELP) },
                                onOpenWayfinding = { nav.navigate(Routes.WAYFINDING) },
                                onOpenBackupHelp = { nav.navigate(Routes.BACKUP_HELP) },
                            )
                        }
                        composable(Routes.TROUBLESHOOT) {
                            TroubleshootScreen(
                                onBack = { nav.popBackStack() },
                                onOpenReminderHelp = { nav.navigate(Routes.HELP_REMINDERS) },
                                onOpenWhyNoDate = { nav.navigate(Routes.whyNoDate()) },
                            )
                        }
                        composable(Routes.INTRO) {
                            IntroScreen(onDone = { nav.popBackStack() }, markSeen = false)
                        }
                        composable(Routes.BACKUP) {
                            BackupScreen(onBack = { nav.popBackStack() })
                        }
                        composable(
                            route = "${Routes.WEIGHT}/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) {
                            WeightScreen(
                                onBack = { nav.popBackStack() },
                                onOpenScaleHelp = { nav.navigate(Routes.SCALE_HELP) },
                                onExplainRefusal = { r ->
                                    nav.navigate(Routes.whyNoDate(r.name))
                                },
                            )
                        }
                        composable(
                            route = "${Routes.LIGHT}/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) {
                            LightMeterScreen(onBack = { nav.popBackStack() })
                        }
                        composable(
                            route = "${Routes.POST_MORTEM}/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) {
                            PostMortemScreen(onDone = {
                                nav.navigate(Routes.DASHBOARD) {
                                    popUpTo(Routes.DASHBOARD) { inclusive = true }
                                }
                            })
                        }
                        composable(
                            route = "${Routes.STICKER}/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) {
                            StickerScreen(onBack = { nav.popBackStack() })
                        }
                        composable(
                            route = "${Routes.CARE}/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) {
                            CareScreen(onBack = { nav.popBackStack() })
                        }
                        composable(
                            route = "${Routes.DIAGNOSE}/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) { entry ->
                            val id = entry.arguments?.getString("id").orEmpty()
                            DiagnoseScreen(
                                onBack = { nav.popBackStack() },
                                // Every leaf of this tree has had a "Log what
                                // you found" button since it was written, and
                                // nobody has ever seen it, because nothing
                                // passed this. Nothing could, while the only
                                // way in was from Help, which does not know
                                // which plant you are worried about.
                                onLogEvent = { nav.navigate(Routes.logEvent(id)) },
                            )
                        }
                        composable(Routes.WEIGHING) {
                            WeighingScreen(onBack = { nav.popBackStack() })
                        }
                        composable(Routes.SCAN) {
                            dev.dheirav.thirsttrap.feature.qr.ScanPotScreen(
                                onBack = { nav.popBackStack() },
                                // The sticker's job is "log something for this
                                // pot", so a hit lands on the quick-log screen
                                // with the scanner popped off the stack.
                                onPlantId = { id ->
                                    nav.navigate(Routes.logEvent(id)) {
                                        popUpTo(Routes.SCAN) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(Routes.EXPERIMENTS) {
                            dev.dheirav.thirsttrap.feature.experiments.ExperimentsScreen(
                                onBack = { nav.popBackStack() },
                                onOpen = { id -> nav.navigate(Routes.experiment(id)) },
                            )
                        }
                        composable(
                            route = "${Routes.EXPERIMENT}/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) {
                            dev.dheirav.thirsttrap.feature.experiments.ExperimentDetailScreen(
                                onBack = { nav.popBackStack() },
                                onOpenPlant = { id -> nav.navigate(Routes.plantDetail(id)) },
                            )
                        }
                        composable(Routes.FERTILIZER) {
                            FertilizerScreen(onBack = { nav.popBackStack() })
                        }
                        composable(Routes.PLACES) {
                            LocationsScreen(
                                advanced = loaded.advancedFeatures,
                                onBack = { nav.popBackStack() },
                                onMeasure = { nav.navigate(Routes.placeLight(it)) },
                                onOpenConditions = { nav.navigate(Routes.AMBIENT) },
                            )
                        }
                        composable(
                            route = "${Routes.PLACE_LIGHT}/{place}",
                            arguments = listOf(navArgument("place") { type = NavType.StringType }),
                        ) {
                            LightMeterScreen(onBack = { nav.popBackStack() })
                        }
                        composable(Routes.STATS) {
                            StatsScreen(onBack = { nav.popBackStack() })
                        }
                        composable(Routes.AMBIENT) {
                            AmbientScreen(onBack = { nav.popBackStack() })
                        }
                        composable(Routes.PROPAGATION) {
                            PropagationScreen(
                                onBack = { nav.popBackStack() },
                                onOpenPlant = { id -> nav.navigate(Routes.plantDetail(id)) },
                            )
                        }
                        composable(Routes.SCALE_HELP) {
                            ScaleHelpScreen(onBack = { nav.popBackStack() })
                        }
                        composable(Routes.DEBUG) {
                            DebugScreen(onBack = { nav.popBackStack() })
                        }
                        composable(
                            route = "${Routes.PLANT_DETAIL}/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                            // Lets a reminder notification open the plant it is about.
                            deepLinks = listOf(navDeepLink { uriPattern = "thirsttrap://plant/{id}" }),
                        ) {
                            PlantDetailScreen(
                                advanced = loaded.advancedFeatures,
                                onBack = { nav.popBackStack() },
                                onEdit = { id -> nav.navigate(Routes.plantEdit(id)) },
                                onLogMore = { id -> nav.navigate(Routes.logEvent(id)) },
                                onDiagnose = { id -> nav.navigate(Routes.diagnose(id)) },
                                onWeigh = { id -> nav.navigate(Routes.weight(id)) },
                                onMeasureLight = { id -> nav.navigate(Routes.light(id)) },
                                onSticker = { id -> nav.navigate(Routes.sticker(id)) },
                                onCare = { id -> nav.navigate(Routes.care(id)) },
                                // A swipe replaces this detail page instead of
                                // stacking on it: browse eight plants and Back
                                // should mean the dashboard, not a re-tour of
                                // the seven pages just swiped past.
                                onOpenPlant = { id ->
                                    nav.navigate(Routes.plantDetail(id)) {
                                        popUpTo("${Routes.PLANT_DETAIL}/{id}") { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(
                            route = "${Routes.PLANT_EDIT}?id={id}",
                            arguments = listOf(
                                navArgument("id") { type = NavType.StringType; defaultValue = "" },
                            ),
                        ) {
                            PlantEditScreen(
                                onDone = { nav.popBackStack() },
                                // Replaces the form rather than stacking on it:
                                // Back from the care notes should be the plant
                                // list, not the form for a plant that is
                                // already saved.
                                onOpenCare = { id ->
                                    nav.navigate(Routes.care(id)) {
                                        popUpTo(Routes.PLANT_EDIT + "?id={id}") { inclusive = true }
                                    }
                                },
                                // A deleted or archived plant has no detail
                                // screen to go back to - popping one step would
                                // land on a ghost.
                                onMarkDied = { id -> nav.navigate(Routes.postMortem(id)) },
                                onPlantGone = {
                                    nav.navigate(Routes.DASHBOARD) {
                                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                                    }
                                },
                            )
                        }
                    }
                }
                // Not on launch. The first time anything is actually
                // scheduled, which is the trigger this function's own docstring
                // named and nothing had wired.
                val hasReminder by mainViewModel.hasReminder.collectAsStateWithLifecycle()
                LaunchedEffect(hasReminder) {
                    if (hasReminder) ensureNotificationPermission()
                }
            }
        }
    }

    /**
     * Asked once, never gated on. If denied the app stays completely usable.
     * Called the moment the first reminder exists, not on launch.
     */
    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/** The pill indicator removed, so selection is carried by the rule in [TabMark]. */
@Composable
private fun flatTabColors() = NavigationBarItemDefaults.colors(
    indicatorColor = Color.Transparent,
)

/**
 * A 2dp rule over the selected tab.
 *
 * The app marks every other section break with a rule, so the bottom bar now
 * does too. The space is reserved whether or not the tab is selected, because a
 * mark that appears and disappears would shift the icons by 2dp as you move
 * between tabs.
 */
@Composable
private fun TabMark(selected: Boolean, icon: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .padding(bottom = 4.dp)
                .width(24.dp)
                .height(2.dp)
                .background(if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent),
        )
        icon()
    }
}
