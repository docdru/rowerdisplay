package io.github.docdru.rowerdisplay.ble

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulatedRowerTest {
    @Test
    fun isDeterministic() {
        assertEquals(SimulatedRower.dataAt(440), SimulatedRower.dataAt(440))
    }

    @Test
    fun paceSettlesAroundTwoOhEight() {
        for (s in 120..1200) {
            val pace = SimulatedRower.paceAt(s)
            assertTrue("pace $pace at $s", pace in 121.0..135.0)
        }
        assertTrue(SimulatedRower.paceAt(0) > 150.0)
    }

    @Test
    fun distanceGrowsAndMatchesPace() {
        var previous = -1
        for (s in 0..600 step 10) {
            val d = SimulatedRower.dataAt(s).totalDistanceM!!
            assertTrue(d >= previous)
            previous = d
        }
        // 20 minutes at roughly 2:08-2:10 is about 4 600-4 700 m.
        assertTrue(SimulatedRower.dataAt(1200).totalDistanceM!! in 4500..4800)
    }

    @Test
    fun powerFormula() {
        // 2:00/500 m is the textbook 203 W.
        assertEquals(203.0, SimulatedRower.powerFor(120.0), 1.0)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun startEmitsOneUpdatePerSecond() = runTest {
        val rower = SimulatedRower()
        rower.start(backgroundScope)
        runCurrent()
        assertEquals(ConnectionState.Simulated, rower.connection.value)
        assertEquals(0, rower.data.value.elapsedTimeS)

        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(5, rower.data.value.elapsedTimeS)

        rower.stop()
        assertEquals(ConnectionState.Idle, rower.connection.value)
    }
}
