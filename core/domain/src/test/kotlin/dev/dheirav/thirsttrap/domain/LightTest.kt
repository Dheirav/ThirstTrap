package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LightTest {

    @Test
    fun `bands run from deep shade to direct sun`() {
        assertEquals(LightLevel.DEEP_SHADE, classifyLux(50f))
        assertEquals(LightLevel.LOW, classifyLux(400f))
        assertEquals(LightLevel.MODERATE, classifyLux(1_500f))
        assertEquals(LightLevel.BRIGHT_INDIRECT, classifyLux(5_000f))
        assertEquals(LightLevel.DIRECT, classifyLux(40_000f))
    }

    @Test
    fun `a dark room is not flattered`() {
        // A room that feels bright to a person is often under 1000 lux.
        assertTrue(classifyLux(300f) <= LightLevel.LOW)
    }

    @Test
    fun `no stated needs means no opinion`() {
        assertNull(assessLightFor(null, LightLevel.DIRECT))
        assertNull(assessLightFor("", LightLevel.DIRECT))
        assertNull(assessLightFor("   ", LightLevel.DIRECT))
    }

    @Test
    fun `a sun lover in deep shade is called out`() {
        assertNotNull(assessLightFor("bright indirect", LightLevel.DEEP_SHADE))
        assertNotNull(assessLightFor("full sun", LightLevel.LOW))
    }

    @Test
    fun `a shade lover in full sun is called out`() {
        assertNotNull(assessLightFor("prefers shade", LightLevel.DIRECT))
    }

    @Test
    fun `a match is confirmed rather than left silent`() {
        assertNotNull(assessLightFor("bright indirect", LightLevel.BRIGHT_INDIRECT))
        assertNotNull(assessLightFor("low light", LightLevel.LOW))
    }

    @Test
    fun `unrecognised wording produces silence, not a guess`() {
        assertNull(assessLightFor("east window in summer", LightLevel.MODERATE))
    }
}
