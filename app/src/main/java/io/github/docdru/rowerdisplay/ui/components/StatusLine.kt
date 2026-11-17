package io.github.docdru.rowerdisplay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.docdru.rowerdisplay.ble.ConnectionState
import io.github.docdru.rowerdisplay.ui.theme.LocalRowerColors

/** Text for the connection state, and whether data is flowing. */
fun connectionText(connection: ConnectionState): Pair<String, Boolean> = when (connection) {
    ConnectionState.Idle -> "Not connected" to false
    ConnectionState.MissingPermission -> "Bluetooth permission needed – tap to allow" to false
    ConnectionState.BluetoothOff -> "Bluetooth is off – tap to turn on" to false
    ConnectionState.Scanning -> "Searching for the rower…" to false
    is ConnectionState.Connecting -> "Connecting to ${connection.deviceName}…" to false
    is ConnectionState.Connected -> "Connected to ${connection.deviceName}" to true
    ConnectionState.Simulated -> "Demo mode: simulated rower" to true
}

/**
 * The top line of every screen: a dot and the connection state, tappable when the user
 * can fix something (permission, Bluetooth off), and [actions] on the right.
 */
@Composable
fun StatusLine(
    connection: ConnectionState,
    textSize: TextUnit,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalRowerColors.current
    val (text, live) = connectionText(connection)
    val actionable = connection == ConnectionState.MissingPermission ||
        connection == ConnectionState.BluetoothOff
    Row(
        modifier = modifier.fillMaxWidth().height(height),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(if (actionable) Modifier.clickable(onClick = onClick) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(textSize.value.dp * 0.7f)
                    .background(if (live) colors.target else colors.tick, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            BasicText(
                text = text,
                maxLines = 1,
                style = TextStyle(color = colors.textMuted, fontSize = textSize),
            )
        }
        actions()
    }
}
