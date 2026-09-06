package dev.dheirav.thirsttrap.domain

/**
 * A single drying cycle: the readings between one watering and the next.
 *
 * Never fit a line across a watering. That is the cardinal rule of the model —
 * a slope averaged over a sawtooth is meaningless. docs/WATERING-MODEL.md §3.
 */
data class DryingSegment(
    val readings: List<WeightReading>,
    val startReason: SegmentStart,
) {
    val isEmpty: Boolean get() = readings.isEmpty()
    val first: WeightReading? get() = readings.firstOrNull()
    val last: WeightReading? get() = readings.lastOrNull()
}

enum class SegmentStart { WATERING, REPOT, CALIBRATION, GAP }

/**
 * Splits readings into drying segments.
 *
 * A new segment starts on any of:
 *  1. a jump of >= [SEGMENT_JUMP_FRACTION] of the range above the previous reading;
 *  2. a watering event between two readings — authoritative, and catches the
 *     case where the user watered but did not weigh;
 *  3. a repot or medium change;
 *  4. a gap longer than [STALE_GAP_DAYS].
 *
 * @param disruptiveEventsMillis timestamps of watering / repot / medium-change events.
 */
fun segmentReadings(
    readings: List<WeightReading>,
    anchors: Anchors?,
    wateringEventsMillis: List<Long> = emptyList(),
    repotEventsMillis: List<Long> = emptyList(),
): List<DryingSegment> {
    val usable = readings.filterNot { it.excluded }.sortedBy { it.timestampMillis }
    if (usable.isEmpty()) return emptyList()

    val jumpThreshold = anchors?.let { SEGMENT_JUMP_FRACTION * it.rangeGrams }

    val segments = mutableListOf<DryingSegment>()
    var current = mutableListOf(usable.first())
    var currentStart = if (usable.first().context == ReadingContext.CALIBRATION) {
        SegmentStart.CALIBRATION
    } else {
        SegmentStart.WATERING
    }

    for (i in 1 until usable.size) {
        val prev = usable[i - 1]
        val next = usable[i]

        val gapDays = (next.timestampMillis - prev.timestampMillis) / MILLIS_PER_DAY
        val rose = jumpThreshold != null && (next.grams - prev.grams) >= jumpThreshold
        val watered = wateringEventsMillis.any { it > prev.timestampMillis && it <= next.timestampMillis }
        val repotted = repotEventsMillis.any { it > prev.timestampMillis && it <= next.timestampMillis }

        val reason = when {
            repotted -> SegmentStart.REPOT
            rose || watered -> SegmentStart.WATERING
            gapDays > STALE_GAP_DAYS -> SegmentStart.GAP
            else -> null
        }

        if (reason != null) {
            segments += DryingSegment(current.toList(), currentStart)
            current = mutableListOf(next)
            currentStart = reason
        } else {
            current += next
        }
    }
    segments += DryingSegment(current.toList(), currentStart)
    return segments
}
