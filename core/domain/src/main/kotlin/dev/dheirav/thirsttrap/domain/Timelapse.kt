package dev.dheirav.thirsttrap.domain

import kotlin.math.roundToInt

/**
 * A plant's photo history as a sequence of plates.
 *
 * Requirement 18 asks for a "scrubbable timelapse", and scrubbable is the
 * operative word: a scrubber, not a rendered video. Encoding an MP4 would mean
 * a codec, a temp file, a progress bar and an export flow, to produce something
 * the user can already make by dragging a finger. It would also be the first
 * thing in the app that could fail silently on somebody's device.
 *
 * Frames are the photos in the order they were taken, with the elapsed time
 * from the first one carried alongside - because in a plant diary the
 * interesting axis is not "photo 3 of 9", it is "day 41".
 */
data class TimelapseFrame(
    val photo: Photo,
    /** 0-based position in the sequence. */
    val index: Int,
    /** Whole days between the first photo and this one. */
    val daysSinceFirst: Int,
) {
    /** "Day 0" reads oddly for the first plate; it is the beginning. */
    val dayLabel: String get() = if (daysSinceFirst == 0) "the first photo" else "day $daysSinceFirst"
}

/**
 * Orders a plant's photos into frames.
 *
 * Sorted by when the photo was *taken*, not by when it was imported: a backup
 * restored onto a new phone would otherwise play the plant's life back in the
 * order the files happened to be written.
 */
fun buildTimelapse(photos: List<Photo>): List<TimelapseFrame> {
    val ordered = photos.sortedBy { it.takenAtMillis }
    val first = ordered.firstOrNull()?.takenAtMillis ?: return emptyList()
    return ordered.mapIndexed { i, photo ->
        TimelapseFrame(
            photo = photo,
            index = i,
            daysSinceFirst = ((photo.takenAtMillis - first) / MILLIS_PER_DAY).roundToInt(),
        )
    }
}

/**
 * Whether a sequence is worth showing at all.
 *
 * One photo is not a timelapse, and the screen says so rather than opening onto
 * a scrubber that cannot move.
 */
fun List<TimelapseFrame>.isPlayable(): Boolean = size >= MIN_TIMELAPSE_FRAMES

/** How long the whole sequence spans, in days. */
fun List<TimelapseFrame>.spanDays(): Int = lastOrNull()?.daysSinceFirst ?: 0
