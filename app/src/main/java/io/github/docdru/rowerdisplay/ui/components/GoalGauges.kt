package io.github.docdru.rowerdisplay.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.docdru.rowerdisplay.ui.theme.LocalRowerColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** A full disc is always one hour, like a physical Time Timer. */
const val DISC_FULL_SECONDS = 3600

/**
 * Time goal: the remaining time as a wedge from 12 o'clock, counter-clockwise, that
 * shrinks to nothing. Past the goal ([remainingS] negative) a wedge in the
 * goal-passed colour grows clockwise with the time over.
 */
@Composable
fun TimerDisc(remainingS: Int, modifier: Modifier = Modifier) {
    val colors = LocalRowerColors.current
    val description =
        if (remainingS >= 0) "$remainingS seconds left" else "${-remainingS} seconds over"
    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        val radius = size.minDimension / 2
        val discRadius = radius * 0.88f
        drawCircle(colors.surface, discRadius, center)

        for (i in 0 until 12) {
            val angle = i * PI / 6
            val direction = Offset(sin(angle).toFloat(), -cos(angle).toFloat())
            drawLine(
                color = colors.tick,
                start = center + direction * (radius * 0.93f),
                end = center + direction * radius,
                strokeWidth = radius * 0.035f,
            )
        }

        val seconds = min(abs(remainingS), DISC_FULL_SECONDS)
        val sweep = seconds.toFloat() / DISC_FULL_SECONDS * 360f
        if (sweep > 0f) {
            drawArc(
                color = if (remainingS > 0) colors.goal else colors.goalPassed,
                startAngle = -90f,
                // Remaining runs counter-clockwise, time over clockwise.
                sweepAngle = if (remainingS > 0) -sweep else sweep,
                useCenter = true,
                topLeft = center - Offset(discRadius, discRadius),
                size = Size(discRadius * 2, discRadius * 2),
            )
        }
        drawCircle(colors.background, discRadius * 0.06f, center)
    }
}

/**
 * Distance goal: a vertical bar. The remaining part is anchored at the top and shrinks
 * upward. Past the goal it fills from the bottom in the goal-passed colour, one bar
 * height per goal distance.
 */
@Composable
fun DistanceBar(goalM: Int, remainingM: Int, modifier: Modifier = Modifier) {
    val colors = LocalRowerColors.current
    val description =
        if (remainingM >= 0) "$remainingM metres left" else "${-remainingM} metres over"
    Canvas(modifier = modifier.semantics { contentDescription = description }) {
        val corner = 16.dp.toPx()
        val outline = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(corner)))
        }
        drawPath(outline, colors.surface)
        val goal = goalM.coerceAtLeast(1).toFloat()
        clipPath(outline) {
            if (remainingM > 0) {
                val height = size.height * (remainingM / goal).coerceIn(0f, 1f)
                drawRect(colors.goal, Offset.Zero, Size(size.width, height))
            } else if (remainingM < 0) {
                val height = size.height * (-remainingM / goal).coerceIn(0f, 1f)
                drawRect(
                    color = colors.goalPassed,
                    topLeft = Offset(0f, size.height - height),
                    size = Size(size.width, height),
                )
            }
        }
        drawPath(outline, colors.tick, style = Stroke(width = 2.dp.toPx()))
    }
}
