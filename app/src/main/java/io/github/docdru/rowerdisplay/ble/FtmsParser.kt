package io.github.docdru.rowerdisplay.ble

/**
 * Parses Bluetooth FTMS packets (Fitness Machine Service 1.0).
 *
 * All fields are little-endian. A packet that ends before a field its flags announce
 * yields `null` rather than throwing, because the bytes come from a Bluetooth peer
 * and must never crash a GATT callback.
 */
object FtmsParser {
    // Rower Data flags (uint16). Bit 0 is inverted: 0 means stroke rate and count ARE present.
    private const val MORE_DATA = 1 shl 0
    private const val AVERAGE_STROKE_RATE = 1 shl 1
    private const val TOTAL_DISTANCE = 1 shl 2
    private const val INSTANTANEOUS_PACE = 1 shl 3
    private const val AVERAGE_PACE = 1 shl 4
    private const val INSTANTANEOUS_POWER = 1 shl 5
    private const val AVERAGE_POWER = 1 shl 6
    private const val RESISTANCE_LEVEL = 1 shl 7
    private const val EXPENDED_ENERGY = 1 shl 8
    private const val HEART_RATE = 1 shl 9
    private const val METABOLIC_EQUIVALENT = 1 shl 10
    private const val ELAPSED_TIME = 1 shl 11
    private const val REMAINING_TIME = 1 shl 12

    /** Rower Data, characteristic 0x2AD1. Returns null for a packet that is too short. */
    fun parseRowerData(bytes: ByteArray): RowerData? {
        val r = Reader(bytes)
        val flags = r.uint16() ?: return null
        fun has(bit: Int) = flags and bit != 0

        var data = RowerData()
        if (!has(MORE_DATA)) {
            val rate = r.uint8() ?: return null
            val count = r.uint16() ?: return null
            data = data.copy(strokeRateSpm = rate / 2.0, strokeCount = count)
        }
        if (has(AVERAGE_STROKE_RATE)) {
            data = data.copy(averageStrokeRateSpm = (r.uint8() ?: return null) / 2.0)
        }
        if (has(TOTAL_DISTANCE)) {
            data = data.copy(totalDistanceM = r.uint24() ?: return null)
        }
        if (has(INSTANTANEOUS_PACE)) {
            data = data.copy(instantaneousPaceS = r.uint16() ?: return null)
        }
        if (has(AVERAGE_PACE)) {
            data = data.copy(averagePaceS = r.uint16() ?: return null)
        }
        if (has(INSTANTANEOUS_POWER)) {
            data = data.copy(instantaneousPowerW = r.sint16() ?: return null)
        }
        if (has(AVERAGE_POWER)) {
            data = data.copy(averagePowerW = r.sint16() ?: return null)
        }
        if (has(RESISTANCE_LEVEL)) {
            data = data.copy(resistanceLevel = r.sint16() ?: return null)
        }
        if (has(EXPENDED_ENERGY)) {
            val total = r.uint16() ?: return null
            val perHour = r.uint16() ?: return null
            val perMinute = r.uint8() ?: return null
            data = data.copy(
                totalEnergyKcal = total.notAvailableAsNull(0xFFFF),
                energyPerHourKcal = perHour.notAvailableAsNull(0xFFFF),
                energyPerMinuteKcal = perMinute.notAvailableAsNull(0xFF),
            )
        }
        if (has(HEART_RATE)) {
            data = data.copy(heartRateBpm = r.uint8() ?: return null)
        }
        if (has(METABOLIC_EQUIVALENT)) {
            data = data.copy(metabolicEquivalent = (r.uint8() ?: return null) / 10.0)
        }
        if (has(ELAPSED_TIME)) {
            data = data.copy(elapsedTimeS = r.uint16() ?: return null)
        }
        if (has(REMAINING_TIME)) {
            data = data.copy(remainingTimeS = r.uint16() ?: return null)
        }
        return data
    }

    /** Training Status, characteristic 0x2AD3: flags (uint8) then status (uint8). */
    fun parseTrainingStatus(bytes: ByteArray): TrainingStatus? {
        if (bytes.size < 2) return null
        return when (bytes[1].toInt() and 0xFF) {
            0x01 -> TrainingStatus.Idle
            // 0x0D = manual mode (quick start); 0x02..0x0C = warm-up, intervals, cool-down etc.
            in 0x02..0x0D -> TrainingStatus.Active
            else -> TrainingStatus.Other
        }
    }

    /** FTMS marks "data not available" for energy fields with all bits set. */
    private fun Int.notAvailableAsNull(sentinel: Int): Int? = if (this == sentinel) null else this

    private class Reader(private val bytes: ByteArray) {
        private var pos = 0

        private fun byteAt(i: Int) = bytes[i].toInt() and 0xFF

        fun uint8(): Int? {
            if (pos + 1 > bytes.size) return null
            return byteAt(pos).also { pos += 1 }
        }

        fun uint16(): Int? {
            if (pos + 2 > bytes.size) return null
            return (byteAt(pos) or (byteAt(pos + 1) shl 8)).also { pos += 2 }
        }

        fun sint16(): Int? = uint16()?.let { if (it >= 0x8000) it - 0x10000 else it }

        fun uint24(): Int? {
            if (pos + 3 > bytes.size) return null
            val value = byteAt(pos) or (byteAt(pos + 1) shl 8) or (byteAt(pos + 2) shl 16)
            pos += 3
            return value
        }
    }
}
