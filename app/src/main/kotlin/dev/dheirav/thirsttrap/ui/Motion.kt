package dev.dheirav.thirsttrap.ui

import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Motion for an app about slow biological processes.
 *
 * Two rules, both from docs/DESIGN-RESEARCH.md section 3.
 *
 * **No overshoot.** Cozy-game UI guidance recommends bounce and spring; calm
 * technology forbids it. Motion is the one place those two vocabularies
 * contradict each other outright, and here calm wins: a plant diary should not
 * spring. Everything below is ease-out `tween`, never a low-damped spring.
 *
 * **Off means off.** `X4` claimed reduce-motion support in a codebase with zero
 * animations, which was true only by vacuum. Now that there is motion, the
 * system setting actually has to be read. When the user has turned animations
 * off, every spec here collapses to [snap] - no shortened duration, no "subtle"
 * version. Off.
 */
object Motion {
    /** Value changes: a bar filling, a number counting. */
    const val SETTLE_MS = 400

    /** Confirmations and content swaps. */
    const val CONFIRM_MS = 220

    @Composable
    @ReadOnlyComposable
    fun reduced(): Boolean {
        val context = LocalContext.current
        return Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }

    @Composable
    fun <T> settle(): FiniteAnimationSpec<T> {
        val off = reduced()
        return remember(off) {
            if (off) snap() else tween(SETTLE_MS, easing = FastOutSlowInEasing)
        }
    }

    @Composable
    fun <T> confirm(): FiniteAnimationSpec<T> {
        val off = reduced()
        return remember(off) {
            if (off) snap() else tween(CONFIRM_MS, easing = FastOutSlowInEasing)
        }
    }
}
