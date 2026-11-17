package io.github.docdru.rowerdisplay.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * One line of text at the largest font size that fits the available width and height.
 * It never wraps: a longer value shrinks instead. The size is found by measuring, so
 * it adapts to phone and tablet alike.
 */
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.CenterStart,
    maxFontSize: TextUnit = 400.sp,
    minFontSize: TextUnit = 8.sp,
) {
    val measurer = rememberTextMeasurer()
    val tightStyle = remember(style) {
        style.copy(
            lineHeight = 1.em,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
        )
    }
    BoxWithConstraints(modifier = modifier, contentAlignment = alignment) {
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val maxHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else Int.MAX_VALUE
        val fontSize = remember(text, tightStyle, maxWidth, maxHeight) {
            var fits = minFontSize.value
            var tooBig = maxFontSize.value
            repeat(SEARCH_STEPS) {
                val candidate = (fits + tooBig) / 2
                val size = measurer.measure(
                    text = text,
                    style = tightStyle.copy(fontSize = candidate.sp),
                    softWrap = false,
                    maxLines = 1,
                    constraints = Constraints(),
                ).size
                if (size.width <= maxWidth && size.height <= maxHeight) {
                    fits = candidate
                } else {
                    tooBig = candidate
                }
            }
            fits.sp
        }
        Box {
            BasicText(
                text = text,
                style = tightStyle.copy(fontSize = fontSize),
                softWrap = false,
                maxLines = 1,
            )
        }
    }
}

private const val SEARCH_STEPS = 10
