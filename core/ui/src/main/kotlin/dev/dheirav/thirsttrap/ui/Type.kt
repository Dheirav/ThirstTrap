package dev.dheirav.thirsttrap.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Newsreader for headings, Hanken Grotesk for everything else.
 *
 * ## Why a serif at all
 *
 * Trade Me measured their custom brand grotesque against Roboto and concluded
 * it was "probably not distinctly visually different to most users". That
 * finding holds for swapping one sans for another and collapses for a serif,
 * which is unmistakably not the system font. Vera and Blossom both use a serif
 * for plant names and section headings and a sans for everything else, which is
 * the pattern here.
 *
 * ## Why these two specifically
 *
 * Measured from the actual font binaries with fontTools rather than chosen from
 * specimens. **Fraunces has no OpenType numeral features at all** - its feature
 * set is `case, kern, liga, rvrn, ss01`, with proportional digits whose advances
 * range 1024-1461 units. Its numbers cannot be made to align, by any setting.
 * The same is true of DM Sans, Commissioner and Instrument Serif.
 *
 * This app displays grams, millilitres, percentages, day counts and chart
 * labels. A number that shifts sideways as it changes is a number you cannot
 * read at a glance, so that disqualifies four otherwise attractive faces.
 *
 * Newsreader and Hanken Grotesk are both **tabular by default**, so no
 * `fontFeatureSettings` plumbing is needed anywhere and no digit can jitter.
 *
 * ## Bundled, not downloadable
 *
 * The downloadable-fonts API does not support variable fonts at all, requires
 * Google Play Services, and lags new families by months. This app is offline
 * and private, so all three matter. One variable TTF replaces the six to nine
 * statics it would otherwise take: 584KB for both families.
 *
 * `minSdk 26` is exactly the floor for [FontVariation.Settings], so there is no
 * fallback branch to write.
 */

@OptIn(ExperimentalTextApi::class)
private fun newsreader(weight: Int, opticalSize: Float) = Font(
    R.font.newsreader,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        // Newsreader carries a real optical-size axis, so a plant name at 22sp
        // and a chart label at 11sp use genuinely different drawings rather
        // than one drawing scaled.
        FontVariation.Setting("opsz", opticalSize),
    ),
)

@OptIn(ExperimentalTextApi::class)
private fun hanken(weight: Int) = Font(
    R.font.hanken_grotesk,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Display = FontFamily(
    newsreader(400, 36f),
    newsreader(500, 30f),
    newsreader(600, 24f),
    newsreader(700, 20f),
)

private val Body = FontFamily(
    hanken(300),
    hanken(400),
    hanken(500),
    hanken(600),
    hanken(700),
)

// M3's default letter spacing is tuned for Roboto. On a humanist sans it reads
// loose, so body and label styles are set to 0 and the line heights opened up
// instead - space between lines rather than between letters.
private val base = Typography()

val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Display, fontWeight = FontWeight.W400),
    displayMedium = base.displayMedium.copy(fontFamily = Display, fontWeight = FontWeight.W400),
    displaySmall = base.displaySmall.copy(fontFamily = Display, fontWeight = FontWeight.W400),

    headlineLarge = base.headlineLarge.copy(fontFamily = Display, fontWeight = FontWeight.W500),
    headlineMedium = base.headlineMedium.copy(fontFamily = Display, fontWeight = FontWeight.W500),
    headlineSmall = base.headlineSmall.copy(fontFamily = Display, fontWeight = FontWeight.W500),

    titleLarge = base.titleLarge.copy(fontFamily = Display, fontWeight = FontWeight.W500),
    // titleMedium is the plant name on a card. Serif, because that is the one
    // word on the card that names a living thing.
    titleMedium = base.titleMedium.copy(
        fontFamily = Display, fontWeight = FontWeight.W600, letterSpacing = 0.sp,
    ),
    titleSmall = base.titleSmall.copy(fontFamily = Body, fontWeight = FontWeight.W600, letterSpacing = 0.sp),

    bodyLarge = base.bodyLarge.copy(fontFamily = Body, letterSpacing = 0.sp, lineHeight = 26.sp),
    bodyMedium = base.bodyMedium.copy(fontFamily = Body, letterSpacing = 0.sp, lineHeight = 22.sp),
    bodySmall = base.bodySmall.copy(fontFamily = Body, letterSpacing = 0.sp, lineHeight = 18.sp),

    labelLarge = base.labelLarge.copy(fontFamily = Body, letterSpacing = 0.sp),
    labelMedium = base.labelMedium.copy(fontFamily = Body, letterSpacing = 0.sp),
    labelSmall = base.labelSmall.copy(fontFamily = Body, letterSpacing = 0.sp),
)
