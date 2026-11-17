package io.github.docdru.rowerdisplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.docdru.rowerdisplay.ble.ConnectionState
import io.github.docdru.rowerdisplay.session.Goal
import io.github.docdru.rowerdisplay.session.Metric
import io.github.docdru.rowerdisplay.session.SessionState
import io.github.docdru.rowerdisplay.session.TargetBand
import io.github.docdru.rowerdisplay.ui.components.AutoSizeText
import io.github.docdru.rowerdisplay.ui.components.DistanceBar
import io.github.docdru.rowerdisplay.ui.components.SessionGraph
import io.github.docdru.rowerdisplay.ui.components.StatusLine
import io.github.docdru.rowerdisplay.ui.components.TimerDisc
import io.github.docdru.rowerdisplay.ui.theme.LocalRowerColors
import io.github.docdru.rowerdisplay.ui.theme.RowerColors
import kotlin.math.abs

/** Everything the rowing screen shows. */
data class RowingUiState(
    val connection: ConnectionState,
    val session: SessionState,
    val middle: Metric,
    val right: Metric,
    val targets: Map<Metric, TargetBand>,
)

/** Sizes that grow with the screen, so a tablet is not a stretched phone. */
internal data class Dimensions(
    val padding: Dp,
    val gap: Dp,
    val labelSize: TextUnit,
    val labelHeight: Dp,
) {
    companion object {
        fun forHeight(height: Dp): Dimensions {
            val scale = (height / 390.dp).coerceIn(1f, 2f)
            return Dimensions(
                padding = 20.dp * scale,
                gap = 24.dp * scale,
                labelSize = 13.sp * scale,
                labelHeight = 44.dp * scale.coerceAtMost(1.3f),
            )
        }
    }
}

/**
 * The one screen: goal (left), momentary value (middle) and accumulated value (right)
 * in the top two thirds; the graph of the middle value in the bottom third. Tapping a
 * value's label picks another value for that slot.
 */
@Composable
fun RowingScreen(
    state: RowingUiState,
    modifier: Modifier = Modifier,
    onStatusClick: () -> Unit = {},
    onPickMiddle: (Metric) -> Unit = {},
    onPickRight: (Metric) -> Unit = {},
    onSettings: () -> Unit = {},
    onEnd: () -> Unit = {},
) {
    val colors = LocalRowerColors.current
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.background)) {
        val dims = Dimensions.forHeight(maxHeight)
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f)
                    .padding(horizontal = dims.padding, vertical = dims.padding / 4),
            ) {
                StatusLine(
                    connection = state.connection,
                    textSize = dims.labelSize,
                    height = dims.labelHeight,
                    onClick = onStatusClick,
                ) {
                    TextButton(onClick = onEnd) {
                        Text("End", fontSize = dims.labelSize)
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(dims.gap),
                ) {
                    GoalElement(state.session, dims, Modifier.weight(0.8f).fillMaxHeight())
                    ValueSlot(
                        metric = state.middle,
                        value = state.session.values[state.middle],
                        choices = Metric.momentary,
                        onPick = onPickMiddle,
                        dims = dims,
                        modifier = Modifier.weight(1.2f).fillMaxHeight(),
                    )
                    ValueSlot(
                        metric = state.right,
                        value = state.session.values[state.right],
                        choices = Metric.accumulated,
                        onPick = onPickRight,
                        dims = dims,
                        modifier = Modifier.weight(1.2f).fillMaxHeight(),
                    )
                }
            }
            SessionGraph(
                state = state.session,
                metric = state.middle,
                target = state.targets[state.middle],
                labelSize = dims.labelSize,
                padding = dims.padding,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

@Composable
private fun SlotLabel(text: String, dims: Dimensions, modifier: Modifier = Modifier) {
    val colors = LocalRowerColors.current
    Box(modifier = modifier.height(dims.labelHeight), contentAlignment = Alignment.CenterStart) {
        BasicText(
            text = text.uppercase(),
            maxLines = 1,
            style = TextStyle(
                color = colors.textMuted,
                fontSize = dims.labelSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (dims.labelSize.value * 0.08f).sp,
            ),
        )
    }
}

private fun bigNumberStyle(colors: RowerColors, passed: Boolean) = TextStyle(
    color = if (passed) colors.goalPassed else colors.text,
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum",
)

@Composable
private fun ValueSlot(
    metric: Metric,
    value: Double?,
    choices: List<Metric>,
    onPick: (Metric) -> Unit,
    dims: Dimensions,
    modifier: Modifier,
) {
    val colors = LocalRowerColors.current
    var menuOpen by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Box {
            Row(
                modifier = Modifier.clickable { menuOpen = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SlotLabel(metric.title, dims)
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Choose the value shown here",
                    tint = colors.textMuted,
                    modifier = Modifier.size(dims.labelSize.value.dp * 1.6f),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                choices.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(choice.title) },
                        onClick = {
                            menuOpen = false
                            onPick(choice)
                        },
                    )
                }
            }
        }
        AutoSizeText(
            text = metric.format(value),
            style = bigNumberStyle(colors, passed = false),
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}

@Composable
private fun GoalElement(session: SessionState, dims: Dimensions, modifier: Modifier) {
    val colors = LocalRowerColors.current
    val remaining = session.remaining
    val passed = remaining < 0
    val sign = if (passed) "+" else ""
    val label = when (session.goal) {
        is Goal.Time -> if (passed) "Time over" else "Time left"
        is Goal.Distance -> if (passed) "Distance over" else "Distance left"
    }
    val number = when (session.goal) {
        is Goal.Time -> sign + formatClock(abs(remaining))
        is Goal.Distance -> sign + formatMetres(abs(remaining)) + " m"
    }
    Column(modifier = modifier) {
        SlotLabel(label, dims)
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = dims.gap / 4),
            contentAlignment = Alignment.Center,
        ) {
            when (val goal = session.goal) {
                is Goal.Time -> TimerDisc(
                    remainingS = remaining,
                    modifier = Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true),
                )
                is Goal.Distance -> DistanceBar(
                    goalM = goal.metres,
                    remainingM = remaining,
                    modifier = Modifier.aspectRatio(0.42f, matchHeightConstraintsFirst = true),
                )
            }
        }
        AutoSizeText(
            text = number,
            style = bigNumberStyle(colors, passed),
            alignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().weight(0.32f),
        )
    }
}
