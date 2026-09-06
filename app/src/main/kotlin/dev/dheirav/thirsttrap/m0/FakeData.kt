package dev.dheirav.thirsttrap.m0

import dev.dheirav.thirsttrap.domain.Anchors
import dev.dheirav.thirsttrap.domain.Medium
import dev.dheirav.thirsttrap.domain.Plant
import dev.dheirav.thirsttrap.domain.PlantAttention
import dev.dheirav.thirsttrap.domain.Prediction
import dev.dheirav.thirsttrap.domain.ReadingContext
import dev.dheirav.thirsttrap.domain.WeightReading
import dev.dheirav.thirsttrap.domain.predictWatering
import dev.dheirav.thirsttrap.domain.segmentReadings
import dev.dheirav.thirsttrap.domain.sortByAttention
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * M0 only. In-memory, hardcoded, thrown away when Room lands in M1.
 *
 * It exists so the two hard UX problems can be answered on a real phone before
 * any architecture is committed to: is the one-tap log genuinely faster than a
 * paper note, and does the reminder tone reward restraint. Everything here is
 * disposable except the domain calls, which are the real thing.
 */
object FakeData {

    private const val DAY = 86_400_000L

    data class LogEntry(val id: Long, val plantId: String, val label: String, val atMillis: Long)

    private var nextId = 1L
    private val _log = MutableStateFlow<List<LogEntry>>(emptyList())
    val log: StateFlow<List<LogEntry>> = _log.asStateFlow()

    private val now = System.currentTimeMillis()

    private val readings = mapOf(
        // Calibrated, drying steadily: should show a real prediction.
        "pothos" to (0..4).map { d ->
            WeightReading("w$d", "pothos", now - (4 - d) * DAY, 1400.0 - 22.0 * d, ReadingContext.ROUTINE)
        },
        // Calibrated but only one reading: must say "weigh once more", not a number.
        "monstera" to listOf(
            WeightReading("m0", "monstera", now - DAY, 2100.0, ReadingContext.POST_WATER),
        ),
        // Past its trigger: should read "water now".
        "fern" to (0..3).map { d ->
            WeightReading("f$d", "fern", now - (3 - d) * DAY, 900.0 - 40.0 * d, ReadingContext.ROUTINE)
        },
    )

    val plants = listOf(
        Plant(
            id = "pothos", name = "marbled pothos", species = "Epipremnum aureum",
            medium = Medium.SOIL, location = "desk",
            anchors = Anchors(1400.0, 1000.0, dryIsProvisional = false),
            slopeEwmaGramsPerDay = -20.0,
        ),
        Plant(
            id = "monstera", name = "monstera", species = "Monstera deliciosa",
            medium = Medium.SOIL, location = "windowsill",
            anchors = Anchors(2100.0, 1500.0, dryIsProvisional = true),
        ),
        Plant(
            id = "fern", name = "maidenhair fern", medium = Medium.SOIL,
            location = "bathroom", depletionTrigger = 0.3,
            anchors = Anchors(900.0, 600.0, dryIsProvisional = false),
            slopeEwmaGramsPerDay = -35.0,
        ),
        Plant(
            id = "cutting", name = "pothos cutting", medium = Medium.WATER,
            location = "kitchen jar",
        ),
        Plant(
            id = "snake", name = "snake plant", medium = Medium.SOIL,
            location = "hallway", depletionTrigger = 0.75,
        ),
    )

    private val lastWatered = mutableMapOf(
        "pothos" to now - 4 * DAY,
        "monstera" to now - DAY,
        "fern" to now - 3 * DAY,
        "cutting" to now - 6 * DAY,
        "snake" to now - 26 * DAY,
    )

    /** One plant is deliberately overdue, so the attention sort has something to do. */
    private val remindersDue = mapOf("fern" to now - 2 * DAY)

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    fun attention(nowMillis: Long): List<PlantAttention> {
        val items = plants.map { plant ->
            val segments = segmentReadings(readings[plant.id].orEmpty(), plant.anchors)
            val prediction = predictWatering(plant, segments, nowMillis)
            PlantAttention(
                plant = plant,
                lastWateredMillis = lastWatered[plant.id],
                lastCheckedMillis = lastWatered[plant.id],
                reminderDueMillis = remindersDue[plant.id],
                depletion = plant.anchors?.let { a ->
                    readings[plant.id]?.lastOrNull()?.let { a.depletionAt(it.grams) }
                },
                prediction = prediction,
            )
        }
        return sortByAttention(items, nowMillis)
    }

    fun logWatered(plantId: String): LogEntry = record(plantId, "Watered")

    fun logChecked(plantId: String, stillWet: Boolean): LogEntry =
        record(plantId, if (stillWet) "Checked - still wet" else "Checked - watered")

    private fun record(plantId: String, label: String): LogEntry {
        val entry = LogEntry(nextId++, plantId, label, System.currentTimeMillis())
        _log.value = listOf(entry) + _log.value
        lastWatered[plantId] = entry.atMillis
        _version.value += 1
        return entry
    }

    /** Undo is a real removal, not a tombstone — see docs/UI-SPEC.md section 2. */
    fun undo(entry: LogEntry) {
        _log.value = _log.value.filterNot { it.id == entry.id }
        _version.value += 1
    }

    fun nameOf(plantId: String): String = plants.first { it.id == plantId }.name

    fun predictionLabel(prediction: Prediction): String? = when (prediction) {
        is Prediction.WaterNow -> "Needs water now"
        is Prediction.Eta -> when {
            prediction.capped -> "More than 2 weeks"
            prediction.days < 1.0 -> "Water today"
            prediction.days < 2.0 -> "Water tomorrow"
            else -> "Water in about ${prediction.days.toInt()} days"
        }
        is Prediction.NeedAnotherReading -> null
    }
}
