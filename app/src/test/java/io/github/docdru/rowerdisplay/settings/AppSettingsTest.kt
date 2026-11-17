package io.github.docdru.rowerdisplay.settings

import io.github.docdru.rowerdisplay.session.Goal
import io.github.docdru.rowerdisplay.session.Metric
import io.github.docdru.rowerdisplay.session.TargetBand
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {
    @Test
    fun timeGoalStepsByAMinuteWithinTheDisc() {
        assertEquals(Goal.Time(21 * 60), Goal.Time(20 * 60).stepped(up = true))
        assertEquals(Goal.Time(19 * 60), Goal.Time(20 * 60).stepped(up = false))
        assertEquals(Goal.Time(3600), Goal.Time(3600).stepped(up = true))
        assertEquals(Goal.Time(60), Goal.Time(60).stepped(up = false))
    }

    @Test
    fun distanceGoalSteps() {
        assertEquals(Goal.Distance(600), Goal.Distance(500).stepped(up = true))
        assertEquals(Goal.Distance(2500), Goal.Distance(2000).stepped(up = true))
        assertEquals(Goal.Distance(1900), Goal.Distance(2000).stepped(up = false))
        assertEquals(Goal.Distance(4500), Goal.Distance(5000).stepped(up = false))
        assertEquals(Goal.Distance(100), Goal.Distance(100).stepped(up = false))
    }

    @Test
    fun targetBandEdgesMoveByOneStepAndNeverCross() {
        val pace = TargetBand(125.0, 135.0)
        assertEquals(TargetBand(126.0, 135.0), pace.adjusted(Metric.Pace, true, up = true))
        assertEquals(TargetBand(125.0, 134.0), pace.adjusted(Metric.Pace, false, up = false))

        val tight = TargetBand(130.0, 131.0)
        assertEquals(tight, tight.adjusted(Metric.Pace, lowEdge = true, up = true))
        assertEquals(tight, tight.adjusted(Metric.Pace, lowEdge = false, up = false))

        val power = TargetBand(150.0, 180.0)
        assertEquals(TargetBand(155.0, 180.0), power.adjusted(Metric.Power, true, up = true))
        // The upper limit for power is 1000 W.
        val top = TargetBand(995.0, 1000.0)
        assertEquals(top, top.adjusted(Metric.Power, lowEdge = false, up = true))
    }

    @Test
    fun sanitizedRepairsBadValues() {
        val bad = AppSettings(
            goal = Goal.Time(10 * 3600),
            middle = Metric.Distance,
            right = Metric.Pace,
            targets = mapOf(Metric.Pace to TargetBand(200.0, 100.0)),
        ).sanitized()

        assertEquals(Goal.Time(3600), bad.goal)
        assertEquals(Metric.Pace, bad.middle)
        assertEquals(Metric.Distance, bad.right)
        assertEquals(TargetBand.defaults, bad.targets)
    }

    @Test
    fun defaultsAreAlreadySane() {
        assertEquals(AppSettings(), AppSettings().sanitized())
    }
}
