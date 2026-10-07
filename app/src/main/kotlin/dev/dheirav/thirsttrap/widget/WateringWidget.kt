package dev.dheirav.thirsttrap.widget

import dev.dheirav.thirsttrap.ui.Space

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
import androidx.glance.layout.Box
import androidx.glance.material3.ColorProviders
import dev.dheirav.thirsttrap.ui.DarkScheme
import dev.dheirav.thirsttrap.ui.LightScheme
import androidx.glance.layout.Column
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
            // The app's own paper and ink, not the system widget grey. The
            // schemes are the same ones the app is painted in, so the widget
            // cannot drift away from it.
            GlanceTheme(colors = ColorProviders(light = LightScheme, dark = DarkScheme)) {
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
    // A plant that died or was given away wants nothing. The widget has its own
    // copy of this question, which is how it went on listing a dead flax cup
    // after the Due list and the reminder sweep had both stopped: three places
    // asking "is this plant owed anything" and only two of them were taught
    // about the fourth answer.
    if (plant.status.isGone) return false
    val due = reminderDueMillis
    return (due != null && due <= nowMillis) || prediction is Prediction.WaterNow
}

@androidx.compose.runtime.Composable
private fun Body(wanted: List<PlantAttention>) {
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(14.dp)
            .padding(horizontal = Space.Entry, vertical = Space.Line),
    ) {
        // Uppercase rather than letterspaced: Glance's TextStyle has no
        // letterSpacing, and caps is the half of the almanac running head that
        // survives the translation.
        Text(
            if (wanted.isEmpty()) "THIRSTTRAP" else "WANTS WATER",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 10.sp(),
                fontWeight = FontWeight.Medium,
            ),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(5.dp))
        Rule()
        Spacer(GlanceModifier.height(7.dp))

        if (wanted.isEmpty()) {
            Text(
                "Nothing is asking for you.",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp()),
            )
            return@Column
        }

        // Three at this size. A widget that scrolls is a list, and the app is
        // already the list.
        wanted.take(3).forEachIndexed { i, item ->
            if (i > 0) Spacer(GlanceModifier.height(6.dp))
            Column(
                GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity(openPlantIntent(item.plant.id))),
            ) {
                Text(
                    item.plant.name,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 14.sp(),
                        fontWeight = FontWeight.Medium,
                    ),
                    maxLines = 1,
                )
                Text(
                    reason(item),
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 11.sp(),
                    ),
                    maxLines = 1,
                )
            }
        }

        if (wanted.size > 3) {
            Spacer(GlanceModifier.height(5.dp))
            Text(
                "and ${wanted.size - 3} more",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 10.sp()),
            )
        }
    }
}

/** The almanac's hairline, which is most of what makes the app look printed. */
@androidx.compose.runtime.Composable
private fun Rule() {
    Box(
        GlanceModifier
            .fillMaxWidth()
            .height(1.dp)
            .background(GlanceTheme.colors.outline),
    ) {}
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
