package io.github.docdru.rowerdisplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.docdru.rowerdisplay.ui.theme.LocalRowerColors
import io.github.docdru.rowerdisplay.ui.theme.RowerDisplayTheme

/**
 * M0 placeholder: shows the agreed screen split (three elements in the top two thirds,
 * graph band in the bottom third) with static sample values. Replaced in M2.
 */
@Composable
fun PlaceholderScreen(modifier: Modifier = Modifier) {
    val colors = LocalRowerColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slot(label = "Goal", value = formatMinutesSeconds(760))
            Slot(label = "Pace / 500 m", value = formatMinutesSeconds(128))
            Slot(label = "Distance m", value = formatMetres(1720))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                text = "Graph",
                style = TextStyle(color = colors.textMuted, fontSize = 14.sp),
            )
        }
    }
}

@Composable
private fun Slot(label: String, value: String) {
    val colors = LocalRowerColors.current
    Column(horizontalAlignment = Alignment.Start) {
        BasicText(
            text = label.uppercase(),
            style = TextStyle(
                color = colors.textMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicText(
            text = value,
            maxLines = 1,
            style = TextStyle(
                color = colors.text,
                fontSize = 72.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Preview(widthDp = 844, heightDp = 390)
@Composable
private fun PlaceholderScreenPreview() {
    RowerDisplayTheme { PlaceholderScreen() }
}
