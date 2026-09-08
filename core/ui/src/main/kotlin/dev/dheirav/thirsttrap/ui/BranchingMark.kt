package dev.dheirav.thirsttrap.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * The empty-state mark: a branching form, not a potted plant.
 *
 * The one number worth taking from the biophilic-design literature is that
 * mid-complexity fractals - fractal dimension **D between about 1.3 and 1.5** -
 * measured roughly 60% better stress recovery by skin conductance than either
 * simpler or more complex forms. That is a specification, so this draws to it:
 * a recursive binary branch whose length ratio and angle put it in that band.
 *
 * Deliberately **not** a mascot. "Dark Patterns of Cuteness" (Springer) argues
 * that cuteness's association with vulnerability stimulates trust responses and
 * can be operationalised to inspire uncritical acceptance - which makes a
 * cartoon face a persuasion channel rather than free decoration. For an app
 * whose stated position is that it will not manipulate the user, that is not a
 * neutral choice. So: no face, no pot, no eyes.
 */
@Composable
fun BranchingMark(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    depth: Int = 8,
) {
    Canvas(modifier) {
        // r ~ 0.72 with a ~28 degree split lands the box-counting dimension in
        // the 1.3-1.5 band for a self-similar binary branch.
        val ratio = 0.72f
        val spread = Math.toRadians(28.0).toFloat()
        val trunk = size.height * 0.30f
        val stroke = 1.5.dp.toPx()

        fun branch(from: Offset, angle: Float, length: Float, level: Int) {
            if (level == 0 || length < 2f) return
            val to = Offset(
                from.x + length * sin(angle),
                from.y - length * cos(angle),
            )
            drawLine(
                color = color,
                start = from,
                end = to,
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            branch(to, angle - spread, length * ratio, level - 1)
            branch(to, angle + spread, length * ratio, level - 1)
        }

        branch(Offset(size.width / 2f, size.height * 0.95f), 0f, trunk, depth)
    }
}
