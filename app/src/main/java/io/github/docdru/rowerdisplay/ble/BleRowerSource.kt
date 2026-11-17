package io.github.docdru.rowerdisplay.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The real rower over Bluetooth LE: scans for it, connects, subscribes to Rower Data
 * and reconnects by itself when the connection drops.
 *
 * All control flow runs in one coroutine on the main thread. GATT callbacks arrive on
 * binder threads and only write thread-safe state ([MutableStateFlow],
 * [CompletableDeferred]) or make GATT calls, and never throw.
 */
// Every Bluetooth call below runs only after BlePermissions.granted() returned true.
@SuppressLint("MissingPermission")
class BleRowerSource(context: Context) : RowerSource {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(BluetoothManager::class.java)

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _data = MutableStateFlow(RowerData())
    override val data: StateFlow<RowerData> = _data.asStateFlow()

    private var job: Job? = null

    override fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.Main) { connectLoop() }
    }

    override fun stop() {
        job?.cancel()
        job = null
        _connection.value = ConnectionState.Idle
    }

    private suspend fun CoroutineScope.connectLoop() {
        while (isActive) {
            if (!BlePermissions.granted(appContext)) {
                _connection.value = ConnectionState.MissingPermission
                delay(POLL_MS)
                continue
            }
            val adapter = manager?.adapter
            if (adapter == null || !adapter.isEnabled) {
                _connection.value = ConnectionState.BluetoothOff
                delay(POLL_MS)
                continue
            }
            _connection.value = ConnectionState.Scanning
            val found = withTimeoutOrNull(SCAN_TIMEOUT_MS) { scanForRower() } ?: continue
            _connection.value = ConnectionState.Connecting(found.second)
            connectAndListen(found.first, found.second)
            delay(RECONNECT_DELAY_MS)
        }
    }

    /** Suspends until a rower is seen; returns it with a display name. */
    private suspend fun scanForRower(): Pair<BluetoothDevice, String>? {
        val scanner = manager?.adapter?.bluetoothLeScanner ?: return null
        return suspendCancellableCoroutine { cont ->
            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val record = result.scanRecord
                    val name = record?.deviceName
                    val uuids = record?.serviceUuids?.map { it.uuid }.orEmpty()
                    val serviceData = record?.getServiceData(FTMS_PARCEL_UUID)
                    if (RowerMatcher.isRower(name, uuids, serviceData) && cont.isActive) {
                        runCatching { scanner.stopScan(this) }
                        cont.resume(result.device to (name ?: "rower"))
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    if (cont.isActive) cont.resume(null)
                }
            }
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            // Unfiltered: the rower's advertisement may carry only its name.
            runCatching { scanner.startScan(null, settings, callback) }
                .onFailure { if (cont.isActive) cont.resume(null) }
            cont.invokeOnCancellation { runCatching { scanner.stopScan(callback) } }
        }
    }

    /** Connects and stays subscribed until the rower disconnects or the coroutine is cancelled. */
    private suspend fun connectAndListen(device: BluetoothDevice, name: String) {
        val subscribed = CompletableDeferred<Unit>()
        val disconnected = CompletableDeferred<Unit>()
        val callback = RowerGattCallback(name, subscribed, disconnected)
        val gatt = runCatching {
            device.connectGatt(appContext, false, callback, BluetoothDevice.TRANSPORT_LE)
        }.getOrNull() ?: return
        try {
            // A rower that went out of range may never answer: give up and scan again.
            val ready = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
                select {
                    subscribed.onAwait { true }
                    disconnected.onAwait { false }
                }
            }
            if (ready == true) disconnected.await()
        } finally {
            runCatching { gatt.disconnect() }
            runCatching { gatt.close() }
        }
    }

    private inner class RowerGattCallback(
        private val name: String,
        private val subscribed: CompletableDeferred<Unit>,
        private val disconnected: CompletableDeferred<Unit>,
    ) : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val connected = newState == BluetoothProfile.STATE_CONNECTED
            if (status == BluetoothGatt.GATT_SUCCESS && connected) {
                if (!gatt.discoverServices()) disconnected.complete(Unit)
            } else {
                disconnected.complete(Unit)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val characteristic = gatt.getService(RowerMatcher.FTMS_SERVICE)
                ?.getCharacteristic(RowerMatcher.ROWER_DATA)
            val descriptor = characteristic?.getDescriptor(RowerMatcher.CLIENT_CONFIG)
            if (status != BluetoothGatt.GATT_SUCCESS || descriptor == null) {
                runCatching { gatt.disconnect() }
                disconnected.complete(Unit)
                return
            }
            val ok = gatt.setCharacteristicNotification(characteristic, true) &&
                writeDescriptor(gatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            if (!ok) {
                runCatching { gatt.disconnect() }
                disconnected.complete(Unit)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connection.value = ConnectionState.Connected(name)
                subscribed.complete(Unit)
            } else {
                runCatching { gatt.disconnect() }
                disconnected.complete(Unit)
            }
        }

        // Android 13+.
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) = onValue(characteristic, value)

        // Android 8–12.
        @Deprecated("Replaced by the overload with a value on Android 13")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                onValue(characteristic, characteristic.value ?: return)
            }
        }

        private fun onValue(characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (characteristic.uuid != RowerMatcher.ROWER_DATA) return
            val parsed = FtmsParser.parseRowerData(value) ?: return
            _data.update { it.mergedWith(parsed) }
        }
    }

    @Suppress("DEPRECATION")
    private fun writeDescriptor(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray,
    ): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        gatt.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
    } else {
        descriptor.value = value
        gatt.writeDescriptor(descriptor)
    }

    private companion object {
        const val POLL_MS = 1_000L
        const val SCAN_TIMEOUT_MS = 20_000L
        const val RECONNECT_DELAY_MS = 2_000L
        const val CONNECT_TIMEOUT_MS = 15_000L
        val FTMS_PARCEL_UUID = ParcelUuid(RowerMatcher.FTMS_SERVICE)
    }
}
