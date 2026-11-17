package io.github.docdru.rowerdisplay.ble

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Runtime permissions needed to scan for and connect to the rower. */
object BlePermissions {
    /**
     * Android 12+: scan (declared with neverForLocation) and connect.
     * Android 8–11: Bluetooth scans need the fine location permission.
     */
    val required: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun granted(context: Context): Boolean = required.all {
        context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }
}
