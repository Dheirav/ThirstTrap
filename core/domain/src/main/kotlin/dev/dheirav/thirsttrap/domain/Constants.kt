package dev.dheirav.thirsttrap.domain

/**
 * Every tunable in the watering model, in one place, each with the reasoning.
 * Specified in docs/WATERING-MODEL.md; the section references below point at it.
 */

/** MAD (managed allowable depletion) guideline from commercial weigh-to-target irrigation. §2 */
const val DEFAULT_DEPLETION_TRIGGER = 0.5

/**
 * Provisional dry anchor = wet x (1 - this). The requirements give 35-45% for
 * peat/coco mixes; 0.40 is the midpoint. Replaced adaptively by observation. §2
 */
const val PROVISIONAL_DEPLETION_FRACTION = 0.40

/**
 * A pre-water reading below this fraction of the wet anchor is rejected as a
 * mis-weigh (pot lifted off the scale, wrong plant) rather than believed as a
 * genuinely bone-dry pot. §2
 */
const val DRY_ANCHOR_FLOOR_FRACTION = 0.30

/**
 * A reading this much of the range above its predecessor is a watering. The
 * requirements say 5-10%; 8% is the midpoint and far above scale noise. §3
 */
const val SEGMENT_JUMP_FRACTION = 0.08

/** Beyond this gap the old segment is stale — conditions have changed. §3 */
const val STALE_GAP_DAYS = 21.0

/** Theil-Sen over the last N readings. 5 gives 10 pairs; more adds nothing. §4 */
const val MAX_FIT_READINGS = 5

/** Below this |slope| there is no measurable drying. Floor, and fraction-of-range. §4 */
const val MIN_SLOPE_GRAMS_PER_DAY = 1.0
const val MIN_SLOPE_RANGE_FRACTION = 0.005

/**
 * EWMA smoothing for the cross-segment slope prior. 0.3 means roughly three
 * segments to adapt — at an ~8-day cycle that is ~24 days, fast enough to track
 * the 2-5x seasonal swing, slow enough that one odd week does not throw it. §5
 */
const val EWMA_ALPHA = 0.3

/** Beyond this, say "more than 2 weeks". Extrapolating 30 days from 5 readings is fiction. §6 */
const val ETA_CAP_DAYS = 14.0

/** Drying this much faster than the plant's own baseline suggests channelling. §7 */
const val FAST_DRYING_MULTIPLIER = 1.8

/** Drying this much slower suggests roots that have stopped drinking. §7 */
const val SLOW_DRYING_MULTIPLIER = 0.4

/** A "pot staying heavy" alert needs at least this long since watering. §7 */
const val SLOW_DRYING_MIN_DAYS = 5.0

/** Diagnostics need a baseline: this many closed segments before either can fire. §7 */
const val MIN_CLOSED_SEGMENTS_FOR_DIAGNOSTICS = 2

const val MILLIS_PER_DAY = 86_400_000.0

// --- Ambient context (F23) -------------------------------------------------

/**
 * How much the room has to move before it counts as having moved.
 *
 * Below these the reading difference is as likely to be where the thermometer
 * was sitting as a real change in the room, and attributing a drying-rate
 * change to it would be inventing a cause.
 */
const val AMBIENT_TEMP_SHIFT_C = 3.0
const val AMBIENT_HUMIDITY_SHIFT_PCT = 10.0

/** A drying rate within this ratio of the baseline has not meaningfully changed. */
const val AMBIENT_DRYING_SHIFT_RATIO = 1.25

/** Readings older than this say nothing about the room the plant is in now. */
const val AMBIENT_STALE_DAYS = 30.0

/** One reading is an anecdote. Two is the minimum for a period average. */
const val AMBIENT_MIN_READINGS_PER_PERIOD = 2
