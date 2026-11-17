package io.github.docdru.rowerdisplay.session

import io.github.docdru.rowerdisplay.ble.RowerData

/**
 * Turns the stream of rower data into a session: time spent rowing, distance and
 * other counters since the start, smoothed momentary values, a one-point-per-second
 * history for the graph, and the goal state.
 *
 * Session time follows the rower's own elapsed-time counter when it sends one: the
 * Woodrower stops that counter as soon as rowing stops, so pauses are exact. Without
 * it, time runs on the local clock while the rower makes progress and stops
 * [PAUSE_AFTER_MS] after the last progress. All rower counters are read as deltas,
 * which copes with the app starting mid-row or the rower resetting its counters.
 *
 * Not thread-safe: call [update] from one thread.
 */
class SessionTracker(val goal: Goal, private val smoothing: Double = DEFAULT_SMOOTHING) {
    var state: SessionState = SessionState(goal)
        private set

    private var previous: RowerData? = null
    private var lastUpdateMs: Long? = null
    private var lastProgressMs: Long? = null
    private var elapsedMs = 0L

    private var distanceM = 0
    private var energyKcal = 0
    private var strokes = 0
    private var sawEnergy = false
    private var sawStrokes = false

    private var smoothed: Map<Metric, Double> = emptyMap()
    private val history = ArrayList<HistoryPoint>()

    /** Folds in the latest rower data, received at monotonic time [nowMs]. */
    fun update(data: RowerData, nowMs: Long): SessionState {
        val prev = previous
        val distanceDelta = counterDelta(prev?.totalDistanceM, data.totalDistanceM)
        distanceM += distanceDelta
        energyKcal += counterDelta(prev?.totalEnergyKcal, data.totalEnergyKcal)
        strokes += counterDelta(prev?.strokeCount, data.strokeCount)
        sawEnergy = sawEnergy || data.totalEnergyKcal != null
        sawStrokes = sawStrokes || data.strokeCount != null

        if (distanceDelta > 0) lastProgressMs = nowMs
        val rowing = lastProgressMs?.let { nowMs - it <= PAUSE_AFTER_MS } ?: false
        val step = lastUpdateMs?.let { (nowMs - it).coerceIn(0L, MAX_STEP_MS) } ?: 0L
        if (prev?.elapsedTimeS != null && data.elapsedTimeS != null) {
            elapsedMs += counterDelta(prev.elapsedTimeS, data.elapsedTimeS) * 1000L
        } else if (rowing) {
            elapsedMs += step
        }
        previous = data
        lastUpdateMs = nowMs

        smoothed = Metric.momentary.mapNotNull { metric ->
            val raw = metric.valueOf(data)?.takeIf { rowing } ?: return@mapNotNull null
            val old = smoothed[metric]
            metric to if (old == null) raw else old + smoothing * (raw - old)
        }.toMap()

        val elapsedS = (elapsedMs / 1000).toInt()
        if (rowing && (history.isEmpty() || history.last().elapsedS < elapsedS)) {
            history.add(HistoryPoint(elapsedS, distanceM, smoothed))
        }

        val values = buildMap {
            putAll(smoothed)
            put(Metric.Time, elapsedS.toDouble())
            put(Metric.Distance, distanceM.toDouble())
            if (sawEnergy) put(Metric.Calories, energyKcal.toDouble())
            if (sawStrokes) put(Metric.Strokes, strokes.toDouble())
        }

        val next = SessionState(
            goal = goal,
            elapsedS = elapsedS,
            distanceM = distanceM,
            values = values,
            history = history.toList(),
            goalReached = state.goalReached,
            rowing = rowing,
        )
        val reachedNow = !state.goalReached && next.remaining <= 0
        state = next.copy(
            goalReached = state.goalReached || reachedNow,
            justReachedGoal = reachedNow,
        )
        return state
    }

    companion object {
        const val PAUSE_AFTER_MS = 4_000L

        /** A longer gap between updates (e.g. a dropped connection) is not rowing time. */
        const val MAX_STEP_MS = 2_000L

        const val DEFAULT_SMOOTHING = 0.35

        /** Growth of a rower counter. A counter that went backwards was reset. */
        internal fun counterDelta(old: Int?, new: Int?): Int {
            if (old == null || new == null) return 0
            val delta = new - old
            return if (delta >= 0) delta else new
        }
    }
}
