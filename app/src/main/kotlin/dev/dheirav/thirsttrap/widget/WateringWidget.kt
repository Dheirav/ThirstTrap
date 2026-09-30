package dev.dheirav.thirsttrap.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.dheirav.thirsttrap.domain.PlantAttention
import dev.dheirav.thirsttrap.domain.PlantRepository
import dev.dheirav.thirsttrap.domain.Prediction
import dev.dheirav.thirsttrap.domain.sortByAttention
import kotlinx.coroutines.flow.first

/**
 * The home-screen widget: what wants water, without opening anything.
 *
 * The daily loop is one question, "does anything need me today", and answering
 * it cost three taps: unlock, find the app, read the list. A widget answers it
 * at zero.
 *
 * Read-only on purpose. Logging from outside the app already exists on the
 * reminder notification, which offers "Watered" and "Still wet" as equals, and
 * a widget row is too small to carry both. One of them alone would teach that
 * watering is the correct answer and restraint is not, which is the single thing
 * this app is built to avoid.
 */
class WateringWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun plantRepository(): PlantRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .plantRepository()

        val now = System.currentTimeMillis()
        val wanted = sortByAttention(
            repository.observeDashboard { System.currentTimeMillis() }.first(),
            now,
        ).filter { it.wantsAttention(now) }

        provideContent {
            GlanceTheme {
                Body(wanted)
            }
        }
    }
}

/**
 * Whether this plant belongs on the widget at all.
 *
 * Deliberately narrower than the list inside the app, which shows everything
 * sorted. A widget that lists every plant every day is wallpaper, and one that
 * lists nothing when nothing is due is doing its job.
 */
private fun PlantAttention.wantsAttention(nowMillis: Long): Boolean {
    val due = reminderDueMillis
    return (due != null && due <= nowMillis) || prediction is Prediction.WaterNow
}

@androidx.compose.runtime.Composable
private fun Body(wanted: List<PlantAttention>) {
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(14.dp),
    ) {
        Text(
            if (wanted.isEmpty()) "Nothing needs water" else "Wants water",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 13.sp(),
                fontWeight = FontWeight.Medium,
            ),
        )

        if (wanted.isEmpty()) {
            Spacer(GlanceModifier.height(6.dp))
            // Not "all done", not a tick, no count of anything. The app does
            // not keep score and neither does its widget.
            Text(
                "Nothing is asking for you today.",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp()),
            )
            return@Column
        }

        Spacer(GlanceModifier.height(8.dp))
        // Four is what fits the smallest useful size without scrolling, and a
        // widget that scrolls is a list, which is what the app already is.
        wanted.take(4).forEach { item ->
            Row(
                GlanceModifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable(actionStartActivity(openPlantIntent(item.plant.id))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(GlanceModifier.defaultWeight()) {
                    Text(
                        item.plant.name,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 14.sp(),
                        ),
                        maxLines = 1,
                    )
                    Text(
                        reason(item),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp(),
                        ),
                        maxLines = 1,
                    )
                }
            }
        }

        if (wanted.size > 4) {
            Text(
                "and ${wanted.size - 4} more",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp()),
            )
        }
    }
}

/**
 * The same deep link the reminder notification uses, rather than a second way
 * in. `thirsttrap://plant/{id}` is already declared in the manifest and already
 * handled, so the widget inherits behaviour that has been working for weeks.
 */
private fun openPlantIntent(plantId: String): Intent = Intent(
    Intent.ACTION_VIEW,
    Uri.parse("thirsttrap://plant/$plantId"),
).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

/** Why it is on the list, in the app's own words rather than a severity. */
private fun reason(item: PlantAttention): String = when {
    item.prediction is Prediction.WaterNow -> "past its trigger, measured"
    else -> "due for a look"
}

private fun Int.sp() = androidx.compose.ui.unit.TextUnit(
    this.toFloat(),
    androidx.compose.ui.unit.TextUnitType.Sp,
)

class WateringWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WateringWidget()
}
