package io.github.docdru.rowerdisplay.session

import io.github.docdru.rowerdisplay.ble.RowerData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class MetricTest {
    private val data = RowerData(
        strokeRateSpm = 24.5,
        strokeCount = 300,
        totalDistanceM = 1720,
        instantaneousPaceS = 128,
        instantaneousPowerW = 167,
        totalEnergyKcal = 90,
        elapsedTimeS = 440,
    )

    @Test
    fun slotListsAreSplitByKind() {
        assertEquals(listOf(Metric.Pace, Metric.Power, Metric.StrokeRate), Metric.momentary)
        assertEquals(
            listOf(Metric.Distance, Metric.Time, Metric.Calories, Metric.Strokes),
            Metric.accumulated,
        )
    }

    @Test
    fun valuesAndFormatting() {
        assertEquals("2:08", Metric.Pace.format(Metric.Pace.valueOf(data)))
        assertEquals("167", Metric.Power.format(Metric.Power.valueOf(data)))
        assertEquals("25", Metric.StrokeRate.format(Metric.StrokeRate.valueOf(data)))
        assertEquals("1 720", Metric.Distance.format(Metric.Distance.valueOf(data)))
        assertEquals("7:20", Metric.Time.format(Metric.Time.valueOf(data)))
        assertEquals("90", Metric.Calories.format(Metric.Calories.valueOf(data)))
        assertEquals("300", Metric.Strokes.format(Metric.Strokes.valueOf(data)))
    }

    @Test
    fun paceRoundingNeverShowsSixtySeconds() {
        assertEquals("2:00", Metric.Pace.format(119.6))
        assertEquals("1:59", Metric.Pace.format(119.4))
    }

    @Test
    fun missingOrIdleValuesShowADash() {
        assertNull(Metric.Pace.valueOf(RowerData(instantaneousPaceS = 0)))
        assertNull(Metric.Power.valueOf(RowerData()))
        assertEquals("–", Metric.Power.format(null))
    }

    @Test
    fun paceIsTheOnlyLowerIsBetterMetric() {
        assertFalse(Metric.Pace.higherIsBetter)
        assertEquals(listOf(Metric.Pace), Metric.entries.filter { !it.higherIsBetter })
    }
}
