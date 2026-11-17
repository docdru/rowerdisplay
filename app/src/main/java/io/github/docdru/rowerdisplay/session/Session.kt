package io.github.docdru.rowerdisplay.session

/** What the session aims for. Chosen at the start (M4); the screen shows how much is left. */
sealed interface Goal {
    /** The amount in the goal's own unit: seconds or metres. */
    val amount: Int

    data class Time(val seconds: Int) : Goal {
        override val amount: Int get() = seconds
    }

    data class Distance(val metres: Int) : Goal {
        override val amount: Int get() = metres
    }
}

/** A target range for a momentary metric, shaded in the graph. [low] <= [high]. */
data class TargetBand(val low: Double, val high: Double) {
    companion object {
        /** Defaults until settings exist (M4). Pace in s/500 m: 2:05–2:15. */
        val defaults: Map<Metric, TargetBand> = mapOf(
            Metric.Pace to TargetBand(125.0, 135.0),
            Metric.Power to TargetBand(150.0, 180.0),
            Metric.StrokeRate to TargetBand(22.0, 26.0),
        )
    }
}

/** One second of the session, for the graph. */
data class HistoryPoint(
    val elapsedS: Int,
    val distanceM: Int,
    /** Smoothed momentary values at that second. */
    val values: Map<Metric, Double>,
)

data class SessionState(
    val goal: Goal,
    /** Seconds spent rowing; pauses are not counted. */
    val elapsedS: Int = 0,
    /** Metres since the session started. */
    val distanceM: Int = 0,
    /** Every metric known so far: momentary ones smoothed, accumulated ones since the start. */
    val values: Map<Metric, Double> = emptyMap(),
    val history: List<HistoryPoint> = emptyList(),
    val goalReached: Boolean = false,
    /** True only for the single update in which the goal was reached. */
    val justReachedGoal: Boolean = false,
    val rowing: Boolean = false,
) {
    /** Progress in the goal's unit: seconds or metres. */
    val progress: Int
        get() = when (goal) {
            is Goal.Time -> elapsedS
            is Goal.Distance -> distanceM
        }

    /** Seconds or metres still to go; negative once the goal is passed. */
    val remaining: Int get() = goal.amount - progress
}
