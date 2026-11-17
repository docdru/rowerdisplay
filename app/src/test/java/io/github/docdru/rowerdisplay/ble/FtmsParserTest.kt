package io.github.docdru.rowerdisplay.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtmsParserTest {
    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    /**
     * The field set the Woodrower announced in nRF Connect (owner's screenshot,
     * 17 Nov 2026): stroke rate + count, average stroke rate, total distance,
     * instantaneous pace, instantaneous power, resistance level, expended energy,
     * heart rate, elapsed time. Flags = bits 1,2,3,5,7,8,9,11 = 0x0BAE.
     * The values are a plausible mid-session packet built by hand to that layout;
     * replace or extend with bytes logged from the real rower when available.
     */
    @Test
    fun woodrowerFullPacket() {
        val packet = bytes(
            0xAE, 0x0B, // flags 0x0BAE
            0x30, // stroke rate 48 * 0.5 = 24.0 spm
            0x2C, 0x01, // stroke count 300
            0x2F, // average stroke rate 47 * 0.5 = 23.5 spm
            0xB8, 0x06, 0x00, // total distance 1720 m
            0x80, 0x00, // instantaneous pace 128 s = 2:08
            0xA7, 0x00, // instantaneous power 167 W
            0x05, 0x00, // resistance level 5
            0x5A, 0x00, // total energy 90 kcal
            0xBC, 0x02, // energy per hour 700 kcal
            0x0B, // energy per minute 11 kcal
            0x8C, // heart rate 140 bpm
            0xB8, 0x01, // elapsed time 440 s
        )

        val data = FtmsParser.parseRowerData(packet)

        assertEquals(
            RowerData(
                strokeRateSpm = 24.0,
                strokeCount = 300,
                averageStrokeRateSpm = 23.5,
                totalDistanceM = 1720,
                instantaneousPaceS = 128,
                instantaneousPowerW = 167,
                resistanceLevel = 5,
                totalEnergyKcal = 90,
                energyPerHourKcal = 700,
                energyPerMinuteKcal = 11,
                heartRateBpm = 140,
                elapsedTimeS = 440,
            ),
            data,
        )
    }

    /** The idle packet from the owner's screenshot: everything zero, resistance level 1. */
    @Test
    fun woodrowerIdlePacket() {
        val packet = bytes(
            0xAE, 0x0B,
            0x00, 0x00, 0x00,
            0x00,
            0x00, 0x00, 0x00,
            0x00, 0x00,
            0x00, 0x00,
            0x01, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00,
            0x00,
            0x00, 0x00,
        )

        val data = FtmsParser.parseRowerData(packet)!!

        assertEquals(0.0, data.strokeRateSpm!!, 0.0)
        assertEquals(0, data.instantaneousPaceS)
        assertEquals(1, data.resistanceLevel)
        assertEquals(0, data.elapsedTimeS)
    }

    @Test
    fun moreDataBitSetMeansNoStrokeFields() {
        // Flags 0x0005: More Data (bit 0) + total distance (bit 2).
        val data = FtmsParser.parseRowerData(bytes(0x05, 0x00, 0x10, 0x27, 0x00))!!

        assertNull(data.strokeRateSpm)
        assertNull(data.strokeCount)
        assertEquals(10_000, data.totalDistanceM)
    }

    @Test
    fun splitMeasurementMergesIntoOneState() {
        // First notification: stroke rate/count + distance. Second: More Data + pace + power.
        val first = FtmsParser.parseRowerData(
            bytes(0x04, 0x00, 0x30, 0x0A, 0x00, 0x64, 0x00, 0x00),
        )!!
        val second = FtmsParser.parseRowerData(bytes(0x29, 0x00, 0x82, 0x00, 0x96, 0x00))!!

        val merged = RowerData().mergedWith(first).mergedWith(second)

        assertEquals(24.0, merged.strokeRateSpm!!, 0.0)
        assertEquals(10, merged.strokeCount)
        assertEquals(100, merged.totalDistanceM)
        assertEquals(130, merged.instantaneousPaceS)
        assertEquals(150, merged.instantaneousPowerW)
    }

    @Test
    fun uint24DistanceUsesAllThreeBytes() {
        val data = FtmsParser.parseRowerData(bytes(0x05, 0x00, 0x01, 0x02, 0x03))!!
        assertEquals(0x030201, data.totalDistanceM)
    }

    @Test
    fun powerAndResistanceAreSigned() {
        // More Data + instantaneous power + resistance level.
        val data = FtmsParser.parseRowerData(bytes(0xA1, 0x00, 0xF6, 0xFF, 0xFF, 0x7F))!!
        assertEquals(-10, data.instantaneousPowerW)
        assertEquals(32767, data.resistanceLevel)
    }

    @Test
    fun negativeResistanceLevelIsParsedNotRejected() {
        // More Data + resistance level -3 (0xFFFD). FTMS allows negative levels.
        val data = FtmsParser.parseRowerData(bytes(0x81, 0x00, 0xFD, 0xFF))!!
        assertEquals(-3, data.resistanceLevel)
    }

    @Test
    fun unsignedFieldsAboveSignedRange() {
        // More Data + pace 0xFFFF must stay 65535, not become negative.
        val data = FtmsParser.parseRowerData(bytes(0x09, 0x00, 0xFF, 0xFF))!!
        assertEquals(65535, data.instantaneousPaceS)
    }

    @Test
    fun energyNotAvailableSentinelsBecomeNull() {
        // More Data + expended energy, all three "not available".
        val data = FtmsParser.parseRowerData(bytes(0x01, 0x01, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF))!!
        assertNull(data.totalEnergyKcal)
        assertNull(data.energyPerHourKcal)
        assertNull(data.energyPerMinuteKcal)
    }

    @Test
    fun metabolicEquivalentAndRemainingTime() {
        // More Data + MET (bit 10) + remaining time (bit 12).
        val data = FtmsParser.parseRowerData(bytes(0x01, 0x14, 0x2D, 0x58, 0x02))!!
        assertEquals(4.5, data.metabolicEquivalent!!, 1e-9)
        assertEquals(600, data.remainingTimeS)
    }

    @Test
    fun shortOrEmptyPacketsReturnNullWithoutThrowing() {
        assertNull(FtmsParser.parseRowerData(ByteArray(0)))
        assertNull(FtmsParser.parseRowerData(bytes(0xAE)))
        // Flags promise stroke fields but only one byte follows.
        assertNull(FtmsParser.parseRowerData(bytes(0x00, 0x00, 0x30)))
        // Full Woodrower flags, packet cut off in the middle.
        assertNull(FtmsParser.parseRowerData(bytes(0xAE, 0x0B, 0x30, 0x2C, 0x01, 0x2F, 0xB8)))
    }

    @Test
    fun everyTruncationOfAValidPacketIsHandled() {
        val packet = bytes(
            0xAE, 0x0B, 0x30, 0x2C, 0x01, 0x2F, 0xB8, 0x06, 0x00, 0x80, 0x00, 0xA7, 0x00,
            0x05, 0x00, 0x5A, 0x00, 0xBC, 0x02, 0x0B, 0x8C, 0xB8, 0x01,
        )
        for (length in 0 until packet.size) {
            assertNull("length $length", FtmsParser.parseRowerData(packet.copyOf(length)))
        }
    }

    @Test
    fun trainingStatus() {
        assertEquals(TrainingStatus.Idle, FtmsParser.parseTrainingStatus(bytes(0x00, 0x01)))
        assertEquals(TrainingStatus.Active, FtmsParser.parseTrainingStatus(bytes(0x00, 0x0D)))
        assertEquals(TrainingStatus.Other, FtmsParser.parseTrainingStatus(bytes(0x00, 0x0F)))
        assertNull(FtmsParser.parseTrainingStatus(bytes(0x00)))
    }
}
