package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * "Watered today" has to mean today, not "within 24 hours".
 *
 * Both are one line of arithmetic and they disagree for most of every day,
 * which is why the wrong one survived: it is right whenever you look at the app
 * at the same hour you watered.
 */
class CalendarDaysTest {

    /** IST, where this diary usually lives. */
    private val ist = 330
    /** +04, where it currently is. */
    private val gulf = 240

    private fun at(day: Long, hour: Int, minute: Int = 0, offsetMinutes: Int): Long =
        day * MILLIS_PER_DAY.toLong() - offsetMinutes * 60_000L +
            hour * 3_600_000L + minute * 60_000L

    @Test
    fun `last night at eleven is yesterday, not today`() {
        val watered = at(20_000, 23, offsetMinutes = ist)
        val now = at(20_001, 8, offsetMinutes = ist)
        // Nine hours elapsed, so the old division said 0 and printed "today".
        assertEquals(0, ((now - watered) / MILLIS_PER_DAY.toLong()).toInt())
        assertEquals(1, calendarDaysAgo(now, watered, ist))
    }

    @Test
    fun `twenty-five hours earlier on the same clock hour is one day`() {
        val then = at(20_000, 9, offsetMinutes = ist)
        val now = at(20_001, 10, offsetMinutes = ist)
        assertEquals(1, calendarDaysAgo(now, then, ist))
    }

    @Test
    fun `the same instant is today when it is today`() {
        val now = at(20_000, 14, offsetMinutes = ist)
        assertEquals(0, calendarDaysAgo(now, now - 3_600_000L, ist))
    }

    @Test
    fun `the frame decides, which is the point of storing one`() {
        // 00:45 IST on day 20_001 is 23:15 on day 20_000 in the Gulf. Read from
        // India it is today; read from Oman the same instant was yesterday.
        val logged = at(20_001, 0, 45, offsetMinutes = ist)
        val now = at(20_001, 12, offsetMinutes = ist)
        assertEquals(0, calendarDaysAgo(now, logged, ist))
        assertEquals(1, calendarDaysAgo(now, logged, gulf))
    }
}
