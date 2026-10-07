package dev.dheirav.thirsttrap.domain

/**
 * Which reading is "now" and which is "last", during a weighing round.
 *
 * The round shows two numbers per pot so you can see the change, and the change
 * is the whole reason for carrying the pot to the scale. That only works while
 * the two numbers come from different rounds.
 *
 * These live here rather than inline in the view model because the distinction
 * is a decision, not plumbing, and inline it had no test and was wrong.
 */

/**
 * The reading this round produced, or null if this pot has not been weighed yet.
 *
 * Latest rather than first, so re-weighing a pot to correct a typo replaces the
 * number instead of being ignored.
 */
fun readingThisRound(
    readings: List<WeightReading>,
    roundStartedAtMillis: Long,
): WeightReading? = readings
    .filter { it.timestampMillis >= roundStartedAtMillis }
    .maxByOrNull { it.timestampMillis }

/**
 * The reading to compare this round against: the most recent one from BEFORE
 * the round began.
 *
 * The bound is what makes it correct. Taking simply the most recent reading
 * means that the moment a pot is weighed, the reading just saved is both the
 * newest reading and therefore also its own predecessor, so "Last" and "Now"
 * print the same number, every delta in the round summary comes out as +0 g,
 * and the hint counting pots "with a previous weight to compare against"
 * counts pots whose only weight is the one from a minute ago.
 *
 * Excluded readings are skipped, because a reading marked as not representing
 * the pot is not a thing to measure change against.
 */
fun previousReading(
    readings: List<WeightReading>,
    roundStartedAtMillis: Long,
): WeightReading? = readings
    .filter { !it.excluded && it.timestampMillis < roundStartedAtMillis }
    .maxByOrNull { it.timestampMillis }
