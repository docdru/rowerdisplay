package io.github.docdru.rowerdisplay.ble

import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A rower that rows by itself: a short start-up, then a pace drifting around 2:08/500 m.
 * Deterministic: [dataAt] gives the same values for the same second, which keeps
 * screenshot tests stable. [start] replays it in real time, one update per second.
 */
class SimulatedRower : RowerSource {
    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _data = MutableStateFlow(RowerData())
    override val data: StateFlow<RowerData> = _data.asStateFlow()

    private var job: Job? = null

    override fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        _connection.value = ConnectionState.Simulated
        job = scope.launch {
            var second = 0
            while (true) {
                _data.value = dataAt(second)
                delay(1_000)
                second++
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        _connection.value = ConnectionState.Idle
    }

    companion object {
        private const val CRUISE_PACE_S = 128.0

        /** Pace in s/500 m at [second]: slower during the first two minutes, then a gentle wave. */
        fun paceAt(second: Int): Double {
            val warmUp = if (second < 120) (120 - second) / 120.0 * 30.0 else 0.0
            val wave = 4.0 * sin(second / 45.0 * 2 * PI) + 1.5 * sin(second / 11.0 * 2 * PI)
            return CRUISE_PACE_S + warmUp + wave
        }

        /** Metres after [second] seconds: the integral of the pace curve, one step per second. */
        fun distanceAt(second: Int): Double {
            var metres = 0.0
            for (s in 0 until second) metres += 500.0 / paceAt(s)
            return metres
        }

        /** Rowing power for a pace, using the usual ergometer relation P = 2.8 / (s/m)^3. */
        fun powerFor(paceS: Double): Double {
            val secondsPerMetre = paceS / 500.0
            return 2.8 / (secondsPerMetre * secondsPerMetre * secondsPerMetre)
        }

        fun dataAt(second: Int): RowerData {
            val pace = paceAt(second)
            val strokeRate = 24.0 + (CRUISE_PACE_S - pace).coerceIn(-6.0, 6.0) / 2.0
            val distance = distanceAt(second)
            val power = powerFor(pace)
            val kcal = second * (power * 4 + 300) / 4184.0
            return RowerData(
                strokeRateSpm = (strokeRate * 2).roundToInt() / 2.0,
                strokeCount = (second * 24.0 / 60.0).toInt(),
                totalDistanceM = distance.toInt(),
                instantaneousPaceS = pace.roundToInt(),
                instantaneousPowerW = power.roundToInt(),
                resistanceLevel = 5,
                totalEnergyKcal = kcal.toInt(),
                elapsedTimeS = second,
            )
        }
    }
}
