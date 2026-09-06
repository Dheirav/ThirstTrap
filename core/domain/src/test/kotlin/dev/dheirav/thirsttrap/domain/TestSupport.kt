package dev.dheirav.thirsttrap.domain

import kotlin.random.Random

const val DAY = 86_400_000L
const val T0 = 1_756_000_000_000L

fun reading(
    dayOffset: Double,
    grams: Double,
    context: ReadingContext = ReadingContext.ROUTINE,
    excluded: Boolean = false,
) = WeightReading(
    id = "r-$dayOffset-$grams",
    plantId = "p1",
    timestampMillis = T0 + (dayOffset * DAY).toLong(),
    grams = grams,
    context = context,
    excluded = excluded,
)

fun plant(
    anchors: Anchors? = Anchors(wetGrams = 1400.0, dryGrams = 1000.0, dryIsProvisional = false),
    trigger: Double = DEFAULT_DEPLETION_TRIGGER,
    ewma: Double? = null,
    medium: Medium = Medium.SOIL,
    needsRecalibration: Boolean = false,
) = Plant(
    id = "p1",
    name = "marbled pothos",
    medium = medium,
    depletionTrigger = trigger,
    anchors = anchors,
    slopeEwmaGramsPerDay = ewma,
    needsRecalibration = needsRecalibration,
)

/** A clean linear drying run: starts at [start] g, loses [perDay] g/day. */
fun linearRun(
    days: Int,
    start: Double,
    perDay: Double,
    noise: Double = 0.0,
    seed: Int = 42,
): List<WeightReading> {
    val rng = Random(seed)
    return (0 until days).map { d ->
        val jitter = if (noise == 0.0) 0.0 else (rng.nextDouble() - 0.5) * 2 * noise
        reading(d.toDouble(), start - perDay * d + jitter)
    }
}

fun segmentOf(readings: List<WeightReading>) = DryingSegment(readings, SegmentStart.WATERING)
