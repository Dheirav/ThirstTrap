package dev.dheirav.thirsttrap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The sticker URI is shared with the notification deep link and, one day, an
 * iOS port. It is the one piece of the QR feature that ports for free, so its
 * shape is worth pinning down.
 */
class PlantUriTest {

    @Test
    fun `the scheme and host are what the manifest filters on`() {
        val id = "04b7b47c-d09c-4b5a-99c8-1e36cf235057"
        assertEquals("thirsttrap://plant/$id", "thirsttrap://plant/$id")
    }

    @Test
    fun `a plant id round-trips through the uri`() {
        val id = newId()
        val uri = "thirsttrap://plant/$id"
        assertEquals(id, uri.removePrefix("thirsttrap://plant/"))
    }

    @Test
    fun `a foreign qr does not decode to a plant id`() {
        val foreign = "https://example.com/whatever"
        assertTrue(foreign.removePrefix("thirsttrap://plant/") == foreign)
    }
}
