package io.github.docdru.rowerdisplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.docdru.rowerdisplay.session.Metric
import io.github.docdru.rowerdisplay.session.TargetBand
import io.github.docdru.rowerdisplay.settings.AppSettings
import io.github.docdru.rowerdisplay.settings.adjusted
import io.github.docdru.rowerdisplay.ui.theme.LocalRowerColors

/** Target ranges for the graph and the demo mode. Changes are saved immediately. */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalRowerColors.current
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.background)) {
        val dims = Dimensions.forHeight(maxHeight)
        val textSize = dims.labelSize * 1.2f
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = dims.padding, vertical = dims.padding / 4),
            verticalArrangement = Arrangement.spacedBy(dims.gap / 2),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Settings", fontSize = textSize * 1.3f, fontWeight = FontWeight.SemiBold)
            }

            Text("Target ranges", fontSize = textSize, fontWeight = FontWeight.SemiBold)
            Text(
                "Shaded in the graph for the value in the middle slot.",
                fontSize = dims.labelSize,
                color = colors.textMuted,
            )
            Metric.momentary.forEach { metric ->
                val band = settings.targets[metric] ?: TargetBand.defaults.getValue(metric)
                TargetRow(metric, band, textSize) { lowEdge, up ->
                    onChange { s ->
                        s.copy(targets = s.targets + (metric to band.adjusted(metric, lowEdge, up)))
                    }
                }
            }

            HorizontalDivider(color = colors.outline)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Demo mode", fontSize = textSize, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Use a simulated rower instead of Bluetooth, e.g. to try the app.",
                        fontSize = dims.labelSize,
                        color = colors.textMuted,
                    )
                }
                Switch(
                    checked = settings.demoMode,
                    onCheckedChange = { on -> onChange { it.copy(demoMode = on) } },
                )
            }
        }
    }
}

@Composable
private fun TargetRow(
    metric: Metric,
    band: TargetBand,
    textSize: TextUnit,
    onStep: (lowEdge: Boolean, up: Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(metric.title, fontSize = textSize, modifier = Modifier.weight(1f))
        Stepper(
            value = metric.format(band.low),
            textSize = textSize,
            onDown = { onStep(true, false) },
            onUp = { onStep(true, true) },
        )
        Text("–", fontSize = textSize)
        Stepper(
            value = metric.format(band.high),
            textSize = textSize,
            onDown = { onStep(false, false) },
            onUp = { onStep(false, true) },
        )
    }
}

@Composable
private fun Stepper(value: String, textSize: TextUnit, onDown: () -> Unit, onUp: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onDown, modifier = Modifier.height(48.dp)) {
            Text("−", fontSize = 20.sp)
        }
        Text(
            text = value,
            fontSize = textSize,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 72.dp).width(textSize.value.dp * 4.5f),
        )
        OutlinedButton(onClick = onUp, modifier = Modifier.height(48.dp)) {
            Text("+", fontSize = 20.sp)
        }
    }
}
