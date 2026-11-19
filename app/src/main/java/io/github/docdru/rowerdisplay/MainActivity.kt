package io.github.docdru.rowerdisplay

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.docdru.rowerdisplay.ble.BlePermissions
import io.github.docdru.rowerdisplay.ble.ConnectionState
import io.github.docdru.rowerdisplay.ui.RowingScreen
import io.github.docdru.rowerdisplay.ui.RowingUiState
import io.github.docdru.rowerdisplay.ui.SettingsScreen
import io.github.docdru.rowerdisplay.ui.StartScreen
import io.github.docdru.rowerdisplay.ui.theme.RowerDisplayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goFullscreen()
        setContent {
            RowerDisplayTheme {
                // The app draws under the hidden system bars and into the camera cutout;
                // only the cutout is kept free, so no control sits under the camera.
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)) {
                    val viewModel: AppViewModel = viewModel()
                    val state by viewModel.state.collectAsStateWithLifecycle()

                    // Asked once on start (not in demo mode). A tap on the status line asks
                    // again, and opens the app's system settings if Android no longer shows
                    // the dialog.
                    val firstRequest = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions(),
                    ) { }
                    val retryRequest = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions(),
                    ) { results -> if (results.values.any { !it }) openAppSettings() }
                    LaunchedEffect(state.settings.demoMode) {
                        val needsPermission = !BlePermissions.granted(this@MainActivity)
                        if (!state.settings.demoMode && needsPermission) {
                            firstRequest.launch(BlePermissions.required)
                        }
                    }
                    val onStatusClick = {
                        when (state.connection) {
                            ConnectionState.MissingPermission ->
                                retryRequest.launch(BlePermissions.required)
                            ConnectionState.BluetoothOff -> requestBluetoothOn()
                            else -> Unit
                        }
                    }

                    val session = state.session
                    when {
                        state.screen == Screen.Settings -> {
                            BackHandler(onBack = viewModel::closeSettings)
                            SettingsScreen(
                                settings = state.settings,
                                onChange = viewModel::updateSettings,
                                onBack = viewModel::closeSettings,
                            )
                        }
                        state.screen == Screen.Rowing && session != null -> {
                            KeepScreenOn()
                            var confirmEnd by remember { mutableStateOf(false) }
                            BackHandler { confirmEnd = true }
                            RowingScreen(
                                state = RowingUiState(
                                    connection = state.connection,
                                    session = session,
                                    middle = state.settings.middle,
                                    right = state.settings.right,
                                    targets = state.settings.targets,
                                ),
                                onStatusClick = onStatusClick,
                                onPickMiddle = { metric ->
                                    viewModel.updateSettings { it.copy(middle = metric) }
                                },
                                onPickRight = { metric ->
                                    viewModel.updateSettings { it.copy(right = metric) }
                                },
                                onSettings = viewModel::openSettings,
                                onEnd = { confirmEnd = true },
                            )
                            if (confirmEnd) {
                                EndSessionDialog(
                                    onConfirm = {
                                        confirmEnd = false
                                        viewModel.endSession()
                                    },
                                    onDismiss = { confirmEnd = false },
                                )
                            }
                        }
                        else -> StartScreen(
                            goal = state.settings.goal,
                            connection = state.connection,
                            onGoalChange = { g -> viewModel.updateSettings { it.copy(goal = g) } },
                            onStart = viewModel::startSession,
                            onStatusClick = onStatusClick,
                            onSettings = viewModel::openSettings,
                        )
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Dialogs, the permission prompt and other apps can bring the bars back.
        if (hasFocus) goFullscreen()
    }

    /** Hides the status and navigation bars; a swipe from the edge shows them briefly. */
    private fun goFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
        runCatching { startActivity(intent) }
    }

    // Only reachable in the BluetoothOff state, which the source reports after the
    // Bluetooth permissions were granted; a SecurityException is caught anyway.
    @SuppressLint("MissingPermission")
    private fun requestBluetoothOn() {
        try {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } catch (_: ActivityNotFoundException) {
            runCatching { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
        } catch (_: SecurityException) {
            runCatching { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
        }
    }
}

@Composable
private fun EndSessionDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("End this session?") },
        text = { Text("You go back to the start screen. The session is not saved yet.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("End session") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep rowing") } },
    )
}

/** Keeps the display on while the rowing screen is shown, and only then. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
