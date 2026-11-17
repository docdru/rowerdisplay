package io.github.docdru.rowerdisplay.session

import io.github.docdru.rowerdisplay.ble.RowerData
import io.github.docdru.rowerdisplay.ble.SimulatedRower
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTrackerTest {
    private fun replay(goal: Goal, seconds: Int): SessionTracker {
        val tracker = SessionTracker(goal)
        for (s in 0..seconds) tracker.update(SimulatedRower.dataAt(s), s * 1000L)
        return tracker
    }

    @Test
    fun timeAndDistanceFollowTheRower() {
        val state = replay(Goal.Time(1200), 440).state
        assertEquals(440, state.elapsedS)
        assertEquals(SimulatedRower.dataAt(440).totalDistanceM, state.distanceM)
        assertEquals(760, state.remaining)
        assertTrue(state.rowing)
        assertFalse(state.goalReached)
    }

    @Test
    fun historyHasOnePointPerSecond() {
        val history = replay(Goal.Time(1200), 60).state.history
        assertEquals((1..60).toList(), history.map { it.elapsedS })
        assertTrue(history.all { it.values[Metric.Pace] != null })
    }

    @Test
    fun goalIsReachedExactlyOnceAndCountingContinues() {
        val tracker = SessionTracker(Goal.Time(30))
        var reachedEvents = 0
        for (s in 0..60) {
            val state = tracker.update(SimulatedRower.dataAt(s), s * 1000L)
            if (state.justReachedGoal) {
                reachedEvents++
                assertEquals(30, state.elapsedS)
            }
        }
        assertEquals(1, reachedEvents)
        assertTrue(tracker.state.goalReached)
        assertFalse(tracker.state.justReachedGoal)
        assertEquals(60, tracker.state.elapsedS)
        assertEquals(-30, tracker.state.remaining)
    }

    @Test
    fun distanceGoal() {
        val state = replay(Goal.Distance(500), 200).state
        assertTrue(state.goalReached)
        assertEquals(500 - state.distanceM, state.remaining)
        assertTrue(state.remaining < 0)
    }

    @Test
    fun pausesWithTheRowersClock() {
        val tracker = replay(Goal.Time(1200), 100)
        val stopped = SimulatedRower.dataAt(100).copy(instantaneousPaceS = 0, strokeRateSpm = 0.0)
        // Like the Woodrower: distance and elapsed time both stand still for 20 s.
        for (s in 101..120) tracker.update(stopped, s * 1000L)
        val state = tracker.state
        assertFalse(state.rowing)
        assertEquals(100, state.elapsedS)
        assertNull(state.values[Metric.Pace])

        // Rowing again: time resumes from there.
        val again = stopped.copy(totalDistanceM = stopped.totalDistanceM!! + 4, elapsedTimeS = 101)
        tracker.update(again, 121_000L)
        assertTrue(tracker.state.rowing)
        assertEquals(101, tracker.state.elapsedS)
    }

    @Test
    fun pausesOnTheLocalClockWhenTheRowerHasNoClock() {
        val tracker = SessionTracker(Goal.Time(1200))
        for (s in 0..10) tracker.update(RowerData(totalDistanceM = s * 4), s * 1000L)
        for (s in 11..30) tracker.update(RowerData(totalDistanceM = 40), s * 1000L)
        // Up to PAUSE_AFTER_MS of standing still is counted, then time stops.
        assertEquals(14, tracker.state.elapsedS)
        assertFalse(tracker.state.rowing)
    }

    @Test
    fun firstUpdateIsNotRowingEvenWithDistance() {
        // Joining mid-row: the first packet only sets the baseline.
        val state = SessionTracker(Goal.Time(600)).update(RowerData(totalDistanceM = 1500), 0L)
        assertFalse(state.rowing)
        assertEquals(0, state.elapsedS)
        assertEquals(0, state.distanceM)
    }

    @Test
    fun longGapIsNotCountedAsRowingTime() {
        val tracker = SessionTracker(Goal.Time(600))
        tracker.update(RowerData(totalDistanceM = 0), 0L)
        tracker.update(RowerData(totalDistanceM = 4), 1_000L)
        tracker.update(RowerData(totalDistanceM = 8), 60_000L)
        assertEquals(1 + 2, tracker.state.elapsedS)
    }

    @Test
    fun countersStartAtZeroWhenJoiningMidRow() {
        val tracker = SessionTracker(Goal.Distance(2000))
        val joined = RowerData(totalDistanceM = 1500, strokeCount = 300, totalEnergyKcal = 80)
        tracker.update(joined, 0L)
        tracker.update(joined.copy(totalDistanceM = 1504, strokeCount = 301), 1_000L)
        val state = tracker.state
        assertEquals(4, state.distanceM)
        assertEquals(1.0, state.values[Metric.Strokes]!!, 0.0)
        assertEquals(0.0, state.values[Metric.Calories]!!, 0.0)
    }

    @Test
    fun rowerCounterResetKeepsTheSessionTotal() {
        val tracker = SessionTracker(Goal.Distance(2000))
        tracker.update(RowerData(totalDistanceM = 0), 0L)
        tracker.update(RowerData(totalDistanceM = 100), 1_000L)
        tracker.update(RowerData(totalDistanceM = 3), 2_000L)
        assertEquals(103, tracker.state.distanceM)
    }

    @Test
    fun caloriesAndStrokesOnlyWhenTheRowerSendsThem() {
        val tracker = SessionTracker(Goal.Time(600))
        tracker.update(RowerData(totalDistanceM = 0), 0L)
        assertNull(tracker.state.values[Metric.Calories])
        assertNull(tracker.state.values[Metric.Strokes])
        assertEquals(0.0, tracker.state.values[Metric.Distance]!!, 0.0)
    }

    @Test
    fun smoothingDampensASingleSpike() {
        val tracker = SessionTracker(Goal.Time(600))
        for (s in 0..10) {
            tracker.update(RowerData(totalDistanceM = s * 4, instantaneousPaceS = 120), s * 1000L)
        }
        tracker.update(RowerData(totalDistanceM = 44, instantaneousPaceS = 180), 11_000L)
        val pace = tracker.state.values[Metric.Pace]!!
        assertTrue("pace $pace", pace > 120.0 && pace < 150.0)
    }

    @Test
    fun counterDelta() {
        assertEquals(0, SessionTracker.counterDelta(null, 10))
        assertEquals(0, SessionTracker.counterDelta(10, null))
        assertEquals(5, SessionTracker.counterDelta(10, 15))
        assertEquals(3, SessionTracker.counterDelta(100, 3))
    }
}
