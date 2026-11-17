package io.github.docdru.rowerdisplay.ble

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/** Where rower data comes from: the real machine over Bluetooth, or a simulation. */
interface RowerSource {
    val connection: StateFlow<ConnectionState>

    /** Latest merged values. Starts empty; partial packets are merged in. */
    val data: StateFlow<RowerData>

    /** Starts producing data in [scope] until [stop] is called or the scope ends. */
    fun start(scope: CoroutineScope)

    fun stop()
}

sealed interface ConnectionState {
    data object Idle : ConnectionState

    data object MissingPermission : ConnectionState

    data object BluetoothOff : ConnectionState

    data object Scanning : ConnectionState

    data class Connecting(val deviceName: String) : ConnectionState

    data class Connected(val deviceName: String) : ConnectionState

    data object Simulated : ConnectionState
}
