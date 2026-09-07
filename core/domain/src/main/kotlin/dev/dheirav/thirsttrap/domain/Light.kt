package dev.dheirav.thirsttrap.domain

/**
 * Reading a lux number as a plant would.
 *
 * The bands come from horticultural practice rather than photography: what
 * matters is roughly how much usable light a leaf gets, and the honest answer
 * for most indoor spots is "less than you think". A room that feels bright to
 * a human is often under 1,000 lux, while overcast daylight outdoors is 10,000.
 */
enum class LightLevel(val label: String, val blurb: String) {
    DEEP_SHADE(
        "Deep shade",
        "Too dark for most things. Aspidistra, pothos and ZZ plants survive here; " +
            "little else will do more than hang on.",
    ),
    LOW(
        "Low light",
        "Fine for pothos, snake plants, ferns and philodendron. Flowering is unlikely.",
    ),
    MODERATE(
        "Moderate, indirect",
        "The comfortable middle - most foliage houseplants do well here.",
    ),
    BRIGHT_INDIRECT(
        "Bright, indirect",
        "What most labels mean by 'bright indirect'. Monstera, calathea and " +
            "begonias are happy.",
    ),
    DIRECT(
        "Direct sun",
        "Succulents, cacti and citrus want this. It will scorch a fern.",
    ),
}

/**
 * @param lux a single instantaneous reading.
 *
 * Bands are deliberately coarse. Phone light sensors are uncalibrated and read
 * differently from each other, so anything finer than this would be false
 * precision.
 */
fun classifyLux(lux: Float): LightLevel = when {
    lux < 200f -> LightLevel.DEEP_SHADE
    lux < 800f -> LightLevel.LOW
    lux < 2_000f -> LightLevel.MODERATE
    lux < 10_000f -> LightLevel.BRIGHT_INDIRECT
    else -> LightLevel.DIRECT
}

/**
 * Whether a measured level plausibly suits what the user wrote in the plant's
 * light needs. Free text, so this is a keyword match and says nothing when it
 * cannot tell - a confident wrong answer would be worse than silence.
 */
fun assessLightFor(needs: String?, measured: LightLevel): String? {
    val n = needs?.lowercase()?.trim().orEmpty()
    if (n.isEmpty()) return null

    val wantsBright = listOf("bright", "direct", "full sun", "sun").any { it in n }
    val wantsShade = listOf("shade", "low", "indirect", "dim").any { it in n }

    return when {
        wantsBright && measured <= LightLevel.LOW ->
            "You noted this one wants bright light. Here it is getting rather less."
        wantsShade && measured == LightLevel.DIRECT ->
            "You noted this one prefers shade. This spot is full sun."
        wantsBright && measured >= LightLevel.BRIGHT_INDIRECT ->
            "That matches what you noted for this plant."
        wantsShade && measured <= LightLevel.MODERATE ->
            "That matches what you noted for this plant."
        else -> null
    }
}
