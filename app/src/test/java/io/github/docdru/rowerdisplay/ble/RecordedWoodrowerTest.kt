package io.github.docdru.rowerdisplay.ble

import io.github.docdru.rowerdisplay.session.Goal
import io.github.docdru.rowerdisplay.session.Metric
import io.github.docdru.rowerdisplay.session.SessionTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Real notifications from the owner's Woodrower (nRF Connect log, 17 Nov 2026):
 * 226 packets over 56 s of slow rowing, then about 7 s standing still.
 * See `src/test/resources/woodrower-2026-11-17.txt`.
 */
class RecordedWoodrowerTest {
    private class Packet(val atMs: Long, val bytes: ByteArray)

    private val packets: List<Packet> = run {
        val stream = checkNotNull(javaClass.getResourceAsStream("/woodrower-2026-11-17.txt"))
        stream.bufferedReader().readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val (time, hex) = line.split(" ")
                val (h, m, s) = time.split(":")
                val atMs = ((h.toLong() * 60 + m.toLong()) * 60 * 1000) +
                    (s.toDouble() * 1000).toLong()
                Packet(atMs, hex.split("-").map { it.toInt(16).toByte() }.toByteArray())
            }
    }

    private fun parse(packet: Packet) = checkNotNull(FtmsParser.parseRowerData(packet.bytes))

    @Test
    fun everyPacketParses() {
        assertEquals(226, packets.size)
        packets.forEach { assertNotNull(FtmsParser.parseRowerData(it.bytes)) }
    }

    @Test
    fun firstPacketMatchesWhatNrfConnectDecoded() {
        assertEquals(
            RowerData(
                strokeRateSpm = 8.0,
                strokeCount = 9,
                averageStrokeRateSpm = 8.0,
                totalDistanceM = 136,
                instantaneousPaceS = 362,
                instantaneousPowerW = 7,
                resistanceLevel = 15,
                totalEnergyKcal = 1,
                energyPerHourKcal = 32,
                energyPerMinuteKcal = 0,
                heartRateBpm = 0,
                elapsedTimeS = 92,
            ),
            parse(packets.first()),
        )
    }

    @Test
    fun stoppedRowerSendsZeroPaceWhichShowsAsNoValue() {
        val last = parse(packets.last())
        assertEquals(0, last.instantaneousPaceS)
        assertEquals(0.0, last.strokeRateSpm!!, 0.0)
        assertNull(Metric.Pace.valueOf(last))
    }

    @Test
    fun replayedSessionMatchesTheRowersOwnCounters() {
        val tracker = SessionTracker(Goal.Time(20 * 60))
        var data = RowerData()
        packets.forEach { packet ->
            data = data.mergedWith(parse(packet))
            tracker.update(data, packet.atMs)
        }
        val state = tracker.state
        // The rower's clock went from 92 s to 141 s and stopped when rowing stopped.
        assertEquals(141 - 92, state.elapsedS)
        assertEquals(201 - 136, state.distanceM)
        assertEquals(14.0 - 9.0, state.values[Metric.Strokes]!!, 0.0)
        assertFalse(state.rowing)
    }
}
