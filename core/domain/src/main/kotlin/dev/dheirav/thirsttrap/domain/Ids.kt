package dev.dheirav.thirsttrap.domain

import java.util.UUID

/**
 * UUID text keys rather than autoincrement integers.
 *
 * Offline-first plus a possible future sync means two devices could otherwise
 * mint the same id, and it makes export/import idempotent - re-importing a
 * backup updates rows instead of duplicating them. See docs/DATA-MODEL.md.
 */
fun newId(): String = UUID.randomUUID().toString()

/**
 * The local UTC offset, in minutes, at a given instant.
 *
 * Stored alongside every timestamp because "days since watered" has to be
 * counted in the local calendar the user actually lived. Water at 23:00 IST,
 * store only UTC, and the event lands on the previous day - every interval
 * calculation is then off by one.
 */
fun tzOffsetMinutesAt(epochMillis: Long): Int =
    java.util.TimeZone.getDefault().getOffset(epochMillis) / 60_000
