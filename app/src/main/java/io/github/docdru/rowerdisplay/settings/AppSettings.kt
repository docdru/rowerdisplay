package io.github.docdru.rowerdisplay.settings

import io.github.docdru.rowerdisplay.session.Goal
import io.github.docdru.rowerdisplay.session.Metric
import io.github.docdru.rowerdisplay.session.MetricKind
import io.github.docdru.rowerdisplay.session.TargetBand

/** Everything the owner can choose. Stored on the device by [SettingsStore]. */
data class AppSettings(
    val goal: Goal = Goal.Time(20 * 60),
    val middle: Metric = Metric.Pace,
    val right: Metric = Metric.Distance,
    val targets: Map<Metric, TargetBand> = TargetBand.defaults,
    val demoMode: Boolean = false,
)

/** Goal presets offered on the start screen. */
object GoalPresets {
    val times: List<Goal> = listOf(10, 20, 30, 45, 60).map { Goal.Time(it * 60) }
    val distances: List<Goal> = listOf(500, 1000, 2000, 5000, 10_000).map { Goal.Distance(it) }

    val defaultTime: Goal = Goal.Time(20 * 60)
    val defaultDistance: Goal = Goal.Distance(2000)

    const val MIN_TIME_S = 60

    /** The disc shows at most one hour. */
    const val MAX_TIME_S = 3600
    const val MIN_DISTANCE_M = 100
    const val MAX_DISTANCE_M = 100_000
}

/** One step up or down: a minute for time; 100 m up to 2 000 m, 500 m above. */
fun Goal.stepped(up: Boolean): Goal = when (this) {
    is Goal.Time -> Goal.Time(
        (seconds + if (up) 60 else -60).coerceIn(GoalPresets.MIN_TIME_S, GoalPresets.MAX_TIME_S),
    )
    is Goal.Distance -> {
        val step = if (metres < 2000 || (!up && metres == 2000)) 100 else 500
        val next = metres + if (up) step else -step
        Goal.Distance(next.coerceIn(GoalPresets.MIN_DISTANCE_M, GoalPresets.MAX_DISTANCE_M))
    }
}

/** Step and allowed range for a metric's target band. */
data class TargetLimits(val step: Double, val min: Double, val max: Double)

val Metric.targetLimits: TargetLimits
    get() = when (this) {
        Metric.Pace -> TargetLimits(step = 1.0, min = 60.0, max = 300.0)
        Metric.Power -> TargetLimits(step = 5.0, min = 10.0, max = 1000.0)
        Metric.StrokeRate -> TargetLimits(step = 1.0, min = 10.0, max = 60.0)
        else -> TargetLimits(step = 1.0, min = 0.0, max = 1e6)
    }

/** Moves one edge of the band by one step, keeping low below high and both in range. */
fun TargetBand.adjusted(metric: Metric, lowEdge: Boolean, up: Boolean): TargetBand {
    val limits = metric.targetLimits
    val delta = if (up) limits.step else -limits.step
    return if (lowEdge) {
        copy(low = (low + delta).coerceIn(limits.min, high - limits.step))
    } else {
        copy(high = (high + delta).coerceIn(low + limits.step, limits.max))
    }
}

/** Keeps a loaded value usable: slot metrics of the right kind, goals within limits. */
internal fun AppSettings.sanitized(): AppSettings {
    val defaults = AppSettings()
    val goal = when (goal) {
        is Goal.Time -> Goal.Time(
            goal.seconds.coerceIn(GoalPresets.MIN_TIME_S, GoalPresets.MAX_TIME_S),
        )
        is Goal.Distance -> Goal.Distance(
            goal.metres.coerceIn(GoalPresets.MIN_DISTANCE_M, GoalPresets.MAX_DISTANCE_M),
        )
    }
    return copy(
        goal = goal,
        middle = middle.takeIf { it.kind == MetricKind.Momentary } ?: defaults.middle,
        right = right.takeIf { it.kind == MetricKind.Accumulated } ?: defaults.right,
        targets = Metric.momentary.associateWith { metric ->
            val band = targets[metric]
            val limits = metric.targetLimits
            val valid = band != null && band.low >= limits.min && band.high <= limits.max &&
                band.low < band.high
            if (valid && band != null) {
                band
            } else {
                TargetBand.defaults.getValue(metric)
            }
        },
    )
}
