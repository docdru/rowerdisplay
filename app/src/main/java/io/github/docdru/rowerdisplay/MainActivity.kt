package io.github.docdru.rowerdisplay

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.docdru.rowerdisplay.ble.BlePermissions
import io.github.docdru.rowerdisplay.ble.ConnectionState
import io.github.docdru.rowerdisplay.ui.RowingScreen
import io.github.docdru.rowerdisplay.ui.theme.RowerDisplayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RowerDisplayTheme {
                val viewModel: RowingViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()

                // Asked once on start. A tap on the status line asks again, and opens the
                // app's system settings if Android no longer shows the dialog.
                val firstRequest = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { }
                val retryRequest = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                ) { results -> if (results.values.any { !it }) openAppSettings() }
                LaunchedEffect(Unit) {
                    if (!BlePermissions.granted(this@MainActivity)) {
                        firstRequest.launch(BlePermissions.required)
                    }
                }

                KeepScreenOn()
                RowingScreen(
                    state = state,
                    onStatusClick = {
                        when (state.connection) {
                            ConnectionState.MissingPermission ->
                                retryRequest.launch(BlePermissions.required)
                            ConnectionState.BluetoothOff -> requestBluetoothOn()
                            else -> Unit
                        }
                    },
                )
            }
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

/** Keeps the display on while the rowing screen is shown. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
