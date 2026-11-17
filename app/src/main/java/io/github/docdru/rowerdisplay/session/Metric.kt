package io.github.docdru.rowerdisplay.session

import io.github.docdru.rowerdisplay.ble.RowerData
import io.github.docdru.rowerdisplay.ui.formatClock
import io.github.docdru.rowerdisplay.ui.formatMetres
import kotlin.math.roundToInt

/** Momentary values change stroke by stroke; accumulated values only grow. */
enum class MetricKind { Momentary, Accumulated }

/**
 * Every value the app can show, described once so the value slots, the graph and
 * the settings all work from the same list.
 */
enum class Metric(
    val label: String,
    val unit: String,
    val kind: MetricKind,
    /** False for pace: a lower number is faster, so the graph draws it upside down. */
    val higherIsBetter: Boolean = true,
) {
    Pace("Pace", "/500 m", MetricKind.Momentary, higherIsBetter = false),
    Power("Power", "W", MetricKind.Momentary),
    StrokeRate("Stroke rate", "spm", MetricKind.Momentary),
    Distance("Distance", "m", MetricKind.Accumulated),
    Time("Time", "", MetricKind.Accumulated),
    Calories("Calories", "kcal", MetricKind.Accumulated),
    Strokes("Strokes", "", MetricKind.Accumulated),
    ;

    /** The rower's raw value for this metric, or null if the rower has not sent it. */
    fun valueOf(data: RowerData): Double? = when (this) {
        // A pace of 0 means "not rowing"; there is no meaningful value to show.
        Pace -> data.instantaneousPaceS?.takeIf { it > 0 }?.toDouble()
        Power -> data.instantaneousPowerW?.toDouble()
        StrokeRate -> data.strokeRateSpm
        Distance -> data.totalDistanceM?.toDouble()
        Time -> data.elapsedTimeS?.toDouble()
        Calories -> data.totalEnergyKcal?.toDouble()
        Strokes -> data.strokeCount?.toDouble()
    }

    /** The value as the big number shows it, or a dash when unknown. */
    fun format(value: Double?): String {
        if (value == null) return "–"
        return when (this) {
            Pace, Time -> formatClock(value.roundToInt())
            Distance -> formatMetres(value.toInt())
            StrokeRate -> value.roundToInt().toString()
            Power, Calories, Strokes -> value.roundToInt().toString()
        }
    }

    /** "Pace /500 m", "Power W", "Time". */
    val title: String get() = if (unit.isEmpty()) label else "$label $unit"

    companion object {
        val momentary: List<Metric> = entries.filter { it.kind == MetricKind.Momentary }
        val accumulated: List<Metric> = entries.filter { it.kind == MetricKind.Accumulated }
    }
}
