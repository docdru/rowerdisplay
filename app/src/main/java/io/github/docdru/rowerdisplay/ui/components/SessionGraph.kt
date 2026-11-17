package io.github.docdru.rowerdisplay.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.docdru.rowerdisplay.session.Goal
import io.github.docdru.rowerdisplay.session.Metric
import io.github.docdru.rowerdisplay.session.SessionState
import io.github.docdru.rowerdisplay.session.TargetBand
import io.github.docdru.rowerdisplay.ui.formatClock
import io.github.docdru.rowerdisplay.ui.formatMetres
import io.github.docdru.rowerdisplay.ui.theme.LocalRowerColors
import kotlin.math.max

/** Points of [metric] over the session, x in the goal's unit (seconds or metres). */
internal fun graphPoints(state: SessionState, metric: Metric): List<Pair<Float, Float>> =
    state.history.mapNotNull { point ->
        val value = point.values[metric] ?: return@mapNotNull null
        val x = when (state.goal) {
            is Goal.Time -> point.elapsedS
            is Goal.Distance -> point.distanceM
        }
        x.toFloat() to value.toFloat()
    }

/**
 * The bottom band: [metric] over the whole goal, with its target band shaded. Past the
 * goal the x axis stretches and a marker shows where the goal was.
 */
@Composable
fun SessionGraph(
    state: SessionState,
    metric: Metric,
    target: TargetBand?,
    labelSize: TextUnit,
    padding: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRowerColors.current
    val points = remember(state.history, metric, state.goal) { graphPoints(state, metric) }
    val labelStyle = TextStyle(color = colors.textMuted, fontSize = labelSize)
    Box(
        modifier = modifier
            .background(colors.surface)
            .semantics { contentDescription = "${metric.label} over the session" },
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(padding)) {
            val goalSpan = state.goal.amount.toFloat().coerceAtLeast(1f)
            val span = max(goalSpan, state.progress.toFloat())
            var dataLow = points.minOfOrNull { it.second } ?: target?.low?.toFloat()
            var dataHigh = points.maxOfOrNull { it.second } ?: target?.high?.toFloat()
            if (dataLow == null || dataHigh == null) return@Canvas
            if (target != null) {
                dataLow = minOf(dataLow, target.low.toFloat())
                dataHigh = maxOf(dataHigh, target.high.toFloat())
            }
            val margin = ((dataHigh - dataLow) * 0.12f).coerceAtLeast(1f)
            val low = dataLow - margin
            val range = dataHigh + margin - low

            fun xPx(x: Float) = x / span * size.width
            fun yPx(v: Float): Float {
                val fraction = (v - low) / range
                // Higher is better draws upward; for pace (lower is faster) faster is up.
                return size.height * if (metric.higherIsBetter) 1 - fraction else fraction
            }

            if (target != null) {
                val top = minOf(yPx(target.low.toFloat()), yPx(target.high.toFloat()))
                val bottom = maxOf(yPx(target.low.toFloat()), yPx(target.high.toFloat()))
                drawRect(colors.targetBand, Offset(0f, top), Size(size.width, bottom - top))
            }

            if (state.progress > state.goal.amount) {
                val x = xPx(goalSpan)
                drawLine(
                    color = colors.goalPassed,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
                )
            }

            if (points.isNotEmpty()) {
                val path = Path()
                points.forEachIndexed { i, (x, v) ->
                    if (i == 0) path.moveTo(xPx(x), yPx(v)) else path.lineTo(xPx(x), yPx(v))
                }
                drawPath(
                    path = path,
                    color = colors.text,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
                val (lastX, lastV) = points.last()
                drawCircle(colors.text, 4.5.dp.toPx(), Offset(xPx(lastX), yPx(lastV)))
            }
        }
        if (target != null) {
            BasicText(
                text = "Target ${metric.format(target.low)}–${metric.format(target.high)}",
                style = labelStyle,
                modifier = Modifier.align(Alignment.TopStart).padding(padding / 2),
            )
        }
        BasicText(
            text = "Goal ${goalLabel(state.goal)}",
            style = labelStyle,
            modifier = Modifier.align(Alignment.BottomEnd).padding(padding / 2),
        )
    }
}

internal fun goalLabel(goal: Goal): String = when (goal) {
    is Goal.Time -> formatClock(goal.seconds)
    is Goal.Distance -> "${formatMetres(goal.metres)} m"
}
