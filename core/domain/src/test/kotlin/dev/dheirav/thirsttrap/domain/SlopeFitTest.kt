package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SlopeFitTest {

    @Test
    fun `theil-sen recovers a clean slope exactly`() {
        val points = (0..4).map { it.toDouble() to 100.0 - 20.0 * it }
        assertEquals(-20.0, theilSenSlope(points)!!, 1e-9)
    }

    @Test
    fun `theil-sen needs two points`() {
        assertNull(theilSenSlope(listOf(0.0 to 100.0)))
        assertNull(theilSenSlope(emptyList()))
    }

    @Test
    fun `duplicate timestamps do not divide by zero`() {
        val points = listOf(0.0 to 100.0, 0.0 to 98.0, 1.0 to 80.0)
        val slope = theilSenSlope(points)!!
        assertTrue(slope.isFinite())
    }

    /**
     * The test that justifies choosing Theil-Sen over least squares. One wildly
     * bad weigh-in — pot half off the scale — must not move the answer much.
     * Least squares fails this: it lands near -33 g/day on the same input.
     */
    @Test
    fun `a single wild outlier barely moves the fit`() {
        val clean = (0..4).map { it.toDouble() to 1400.0 - 20.0 * it }
        // The outlier must sit at an END of the x-range. One at the centre has
        // zero leverage on a least-squares slope - it only moves the intercept -
        // so a mid-series outlier would make this test pass for both estimators
        // and prove nothing.
        val corrupted = clean.toMutableList().apply { this[4] = 4.0 to 1200.0 }

        val cleanSlope = theilSenSlope(clean)!!
        val dirtySlope = theilSenSlope(corrupted)!!

        assertEquals(-20.0, cleanSlope, 1e-9)
        assertTrue(
            "outlier moved Theil-Sen slope to $dirtySlope",
            abs(dirtySlope - cleanSlope) / abs(cleanSlope) < 0.10,
        )

        // Least squares on the same data, for contrast.
        val n = corrupted.size
        val mx = corrupted.sumOf { it.first } / n
        val my = corrupted.sumOf { it.second } / n
        val ols = corrupted.sumOf { (it.first - mx) * (it.second - my) } /
            corrupted.sumOf { (it.first - mx) * (it.first - mx) }
        assertTrue(
            "least squares should be dragged well past 10%, was $ols",
            abs(ols - cleanSlope) / abs(cleanSlope) > 0.30,
        )
    }

    @Test
    fun `two readings fall back to a two-point fit`() {
        val fit = fitSegmentSlope(segmentOf(linearRun(2, 1400.0, 20.0)), plant().anchors)
        assertTrue(fit is SlopeFit.Fitted)
        assertEquals(SlopeMethod.TWO_POINT, (fit as SlopeFit.Fitted).method)
    }

    @Test
    fun `one reading is insufficient`() {
        val fit = fitSegmentSlope(segmentOf(linearRun(1, 1400.0, 20.0)), plant().anchors)
        assertEquals(SlopeFit.Insufficient, fit)
    }

    @Test
    fun `a flat pot reports no measurable drying rather than a tiny slope`() {
        val flat = (0..4).map { reading(it.toDouble(), 1400.0) }
        val fit = fitSegmentSlope(segmentOf(flat), plant().anchors)
        assertTrue(fit is SlopeFit.NoMeasurableDrying)
    }

    @Test
    fun `only the last five readings are used`() {
        // A count is not a recency claim. Over a straight line any five
        // readings give the same slope, so the old fixture passed just as
        // happily with take() as with takeLast() - checked by mutation, and it
        // did. The run has to bend for the two to disagree: 40 g/day for the
        // first fifteen days, then 5 g/day for the last five.
        val slowTail = buildList {
            addAll((0..14).map { reading(it.toDouble(), 2000.0 - 40.0 * it) })
            addAll((15..19).map { reading(it.toDouble(), 1400.0 - 5.0 * (it - 15)) })
        }
        val fit = fitSegmentSlope(segmentOf(slowTail), plant().anchors) as SlopeFit.Fitted
        assertEquals(MAX_FIT_READINGS, fit.readingCount)
        // The recent five, not the first five and not all twenty.
        assertEquals(-5.0, fit.gramsPerDay, 0.5)
    }

    @Test
    fun `excluded readings are ignored`() {
        val readings = listOf(
            reading(0.0, 1400.0),
            reading(1.0, 1.0, excluded = true),
            reading(2.0, 1360.0),
            reading(3.0, 1340.0),
        )
        val fit = fitSegmentSlope(segmentOf(readings), plant().anchors) as SlopeFit.Fitted
        assertEquals(-20.0, fit.gramsPerDay, 1.0)
    }

    @Test
    fun `ewma seeds then converges on a stable rate`() {
        var ewma: Double? = null
        repeat(4) { ewma = updateEwma(ewma, -20.0) }
        assertEquals(-20.0, ewma!!, 1e-9)
    }

    @Test
    fun `ewma tracks a step change within about three segments`() {
        var ewma: Double? = -10.0
        repeat(3) { ewma = updateEwma(ewma, -30.0) }
        assertTrue("ewma was $ewma", ewma!! < -20.0)
    }

    @Test
    fun `the smoothing weight itself is pinned, not just its direction`() {
        // Both tests above are blind to alpha. The first sits on the fixed
        // point, where a*x + (1-a)*x = x for every alpha, and the second
        // asserts a bound that holds for any alpha above about 0.206. Setting
        // EWMA_ALPHA = 1.0, meaning no smoothing at all, left all 256 tests
        // passing - which is the whole reason this test exists.
        //
        // One step from a known prior is the smallest thing that can see it:
        // 0.3 x (-30) + 0.7 x (-10) = -16.0, and no other alpha gives that.
        assertEquals(-16.0, updateEwma(-10.0, -30.0)!!, 1e-9)
        // And the inertia section 5 of WATERING-MODEL is arguing for: a single
        // odd segment must not drag the prior most of the way to it.
        val oneOddWeek = updateEwma(-10.0, -40.0)!!
        assertTrue("moved $oneOddWeek of the way", oneOddWeek > -20.0)
    }
}
