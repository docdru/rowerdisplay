package io.github.docdru.rowerdisplay.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.docdru.rowerdisplay.ble.ConnectionState
import io.github.docdru.rowerdisplay.session.Goal
import io.github.docdru.rowerdisplay.settings.GoalPresets
import io.github.docdru.rowerdisplay.settings.stepped
import io.github.docdru.rowerdisplay.ui.components.DistanceBar
import io.github.docdru.rowerdisplay.ui.components.StatusLine
import io.github.docdru.rowerdisplay.ui.components.TimerDisc
import io.github.docdru.rowerdisplay.ui.components.goalLabel
import io.github.docdru.rowerdisplay.ui.theme.LocalRowerColors

/**
 * Before a session: pick a time or distance goal, then Start. The rower connects in the
 * background meanwhile, so it is ready when Start is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    goal: Goal,
    connection: ConnectionState,
    onGoalChange: (Goal) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    onStatusClick: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val colors = LocalRowerColors.current
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.background)) {
        val dims = Dimensions.forHeight(maxHeight)
        val numberSize = 56.sp * (dims.labelSize.value / 13f)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dims.padding, vertical = dims.padding / 4),
        ) {
            StatusLine(
                connection = connection,
                textSize = dims.labelSize,
                height = dims.labelHeight,
                onClick = onStatusClick,
            ) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(dims.gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.weight(0.6f).fillMaxHeight().padding(vertical = dims.gap),
                    contentAlignment = Alignment.Center,
                ) {
                    when (goal) {
                        is Goal.Time -> TimerDisc(
                            remainingS = goal.seconds,
                            modifier = Modifier.aspectRatio(1f, true),
                        )
                        is Goal.Distance -> DistanceBar(
                            goalM = goal.metres,
                            remainingM = goal.metres,
                            modifier = Modifier.aspectRatio(0.42f, true),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1.4f),
                    verticalArrangement = Arrangement.spacedBy(dims.gap / 2),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = goal is Goal.Time,
                            onClick = {
                                if (goal !is Goal.Time) onGoalChange(GoalPresets.defaultTime)
                            },
                            label = { Text("Time", fontSize = dims.labelSize) },
                        )
                        FilterChip(
                            selected = goal is Goal.Distance,
                            onClick = {
                                if (goal !is Goal.Distance) {
                                    onGoalChange(GoalPresets.defaultDistance)
                                }
                            },
                            label = { Text("Distance", fontSize = dims.labelSize) },
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dims.gap / 2),
                    ) {
                        OutlinedButton(
                            onClick = { onGoalChange(goal.stepped(up = false)) },
                            modifier = Modifier.height(56.dp),
                        ) { Text("−", fontSize = 24.sp) }
                        Text(
                            text = goalLabel(goal),
                            fontSize = numberSize,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = colors.text,
                            maxLines = 1,
                            modifier = Modifier.widthIn(min = numberSize.value.dp * 3.2f),
                        )
                        OutlinedButton(
                            onClick = { onGoalChange(goal.stepped(up = true)) },
                            modifier = Modifier.height(56.dp),
                        ) { Text("+", fontSize = 24.sp) }
                    }
                    val presets =
                        if (goal is Goal.Time) GoalPresets.times else GoalPresets.distances
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets.forEach { preset ->
                            FilterChip(
                                selected = preset == goal,
                                onClick = { onGoalChange(preset) },
                                label = { Text(goalLabel(preset), fontSize = dims.labelSize) },
                            )
                        }
                    }
                }
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(0.7f).height(dims.labelHeight * 2),
                ) {
                    Text(
                        text = "Start",
                        fontSize = dims.labelSize * 1.6f,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
