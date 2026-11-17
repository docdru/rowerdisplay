package io.github.docdru.rowerdisplay.ble

import java.util.UUID

/** Bluetooth UUIDs and the rule for recognising a rower in scan results. */
object RowerMatcher {
    val FTMS_SERVICE: UUID = uuid16(0x1826)
    val ROWER_DATA: UUID = uuid16(0x2AD1)
    val CLIENT_CONFIG: UUID = uuid16(0x2902)

    private const val NAME_PREFIX = "DOMYOS-ROW"

    /** FTMS service data: flags (uint8), then machine type (uint16); bit 4 = rower. */
    private const val MACHINE_TYPE_ROWER = 1 shl 4

    /**
     * True when an advertisement is a rower: a Domyos rower's name, FTMS service data
     * that says "rower", or, without service data, the advertised FTMS service.
     * A machine whose service data says it is something else (bike, treadmill) is skipped.
     */
    fun isRower(name: String?, serviceUuids: List<UUID>, ftmsServiceData: ByteArray?): Boolean {
        if (name?.startsWith(NAME_PREFIX) == true) return true
        if (ftmsServiceData != null && ftmsServiceData.size >= 3) {
            val type = (ftmsServiceData[1].toInt() and 0xFF) or
                ((ftmsServiceData[2].toInt() and 0xFF) shl 8)
            return type and MACHINE_TYPE_ROWER != 0
        }
        return FTMS_SERVICE in serviceUuids
    }

    private fun uuid16(short: Int): UUID {
        val hex = short.toString(16).padStart(4, '0')
        return UUID.fromString("0000$hex-0000-1000-8000-00805f9b34fb")
    }
}
