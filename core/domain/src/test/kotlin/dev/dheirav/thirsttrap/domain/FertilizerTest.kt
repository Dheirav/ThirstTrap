package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The arithmetic is trivial; the refusals are the point. A dilution the app
 * cannot read, and a dose nobody can pour, both have to be said out loud rather
 * than rounded into a confident number.
 */
class FertilizerTest {

    @Test
    fun `reads the two forms a bottle actually uses`() {
        assertEquals(Dilution.Ratio(200.0), parseDilution("1:200"))
        assertEquals(Dilution.Ratio(200.0), parseDilution(" 1 : 200 "))
        assertEquals(Dilution.Ratio(500.0), parseDilution("1/500"))
        assertEquals(Dilution.MlPerLitre(5.0), parseDilution("5ml/l"))
        assertEquals(Dilution.MlPerLitre(2.5), parseDilution("2.5 ml per litre"))
        assertEquals(Dilution.MlPerLitre(2.5), parseDilution("2,5 ml per liter"))
    }

    @Test
    fun `says nothing about wording it cannot act on`() {
        // Each of these is a real thing printed on a bottle, and each would be
        // a different number if guessed at.
        for (t in listOf("a capful", "1 tsp per gallon", "as directed", "weekly",
                         "half strength", "", "  ", "1:0", "0 ml/l")) {
            assertNull("should not have parsed $t", parseDilution(t))
        }
        assertNull(parseDilution(null))
    }

    @Test
    fun `a ratio is one part in that many parts of water`() {
        assertEquals(5.0, concentrateMl(Dilution.Ratio(200.0), 1000.0), 1e-9)
        assertEquals(2.5, concentrateMl(Dilution.Ratio(200.0), 500.0), 1e-9)
    }

    @Test
    fun `millilitres per litre scale with the can`() {
        assertEquals(5.0, concentrateMl(Dilution.MlPerLitre(5.0), 1000.0), 1e-9)
        assertEquals(1.25, concentrateMl(Dilution.MlPerLitre(5.0), 250.0), 1e-9)
    }

    @Test
    fun `a dose nobody can pour is not offered as a dose`() {
        // 1:2000 into a 250 ml can is 0.125 ml. The number is right and the
        // instruction is useless.
        val d = doseFor(Dilution.Ratio(2000.0), 250.0)
        assertTrue("got $d", d is DoseAdvice.TooSmall)
        d as DoseAdvice.TooSmall
        assertEquals(0.125, d.concentrateMl, 1e-9)
        // Carries a can size that makes it measurable, rounded up to a litre.
        assertEquals(1000.0, d.suggestedWaterMl, 1e-9)
        assertTrue(concentrateMl(Dilution.Ratio(2000.0), d.suggestedWaterMl) >= UNPOURABLE_ML)
    }

    @Test
    fun `the suggested volume always clears the threshold`() {
        for (parts in listOf(500.0, 1000.0, 2000.0, 5000.0, 10000.0)) {
            val d = doseFor(Dilution.Ratio(parts), 100.0)
            if (d is DoseAdvice.TooSmall) {
                assertTrue(
                    "parts=$parts suggested=${d.suggestedWaterMl}",
                    concentrateMl(Dilution.Ratio(parts), d.suggestedWaterMl) >= UNPOURABLE_ML,
                )
            }
        }
    }

    @Test
    fun `no dilution recorded means no number`() {
        assertEquals(DoseAdvice.Unknown, doseFor(null, 1000.0))
        assertEquals(DoseAdvice.Unknown, doseFor(Dilution.Ratio(200.0), 0.0))
    }

    @Test
    fun `labels read the way the bottle does`() {
        assertEquals("1:200", Dilution.Ratio(200.0).label)
        assertEquals("5 ml/L", Dilution.MlPerLitre(5.0).label)
        assertEquals("2.5 ml/L", Dilution.MlPerLitre(2.5).label)
    }
}
