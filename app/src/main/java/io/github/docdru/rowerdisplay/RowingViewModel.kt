package io.github.docdru.rowerdisplay

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.docdru.rowerdisplay.ble.RowerSource
import io.github.docdru.rowerdisplay.ble.SimulatedRower
import io.github.docdru.rowerdisplay.session.Goal
import io.github.docdru.rowerdisplay.session.Metric
import io.github.docdru.rowerdisplay.session.SessionTracker
import io.github.docdru.rowerdisplay.session.TargetBand
import io.github.docdru.rowerdisplay.ui.RowingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Connects a [RowerSource] to a [SessionTracker] and exposes what the screen shows.
 * M2: always the simulated rower with a 20-minute time goal; M3 adds Bluetooth and M4
 * the start screen and settings.
 */
class RowingViewModel(
    private val source: RowerSource = SimulatedRower(),
    goal: Goal = Goal.Time(20 * 60),
) : ViewModel() {
    private val tracker = SessionTracker(goal)

    private val _state = MutableStateFlow(
        RowingUiState(
            connection = source.connection.value,
            session = tracker.state,
            middle = Metric.Pace,
            right = Metric.Distance,
            targets = TargetBand.defaults,
        ),
    )
    val state: StateFlow<RowingUiState> = _state.asStateFlow()

    init {
        source.start(viewModelScope)
        viewModelScope.launch {
            source.data.collect { data ->
                val session = tracker.update(data, SystemClock.elapsedRealtime())
                _state.update { it.copy(session = session) }
            }
        }
        viewModelScope.launch {
            source.connection.collect { connection ->
                _state.update { it.copy(connection = connection) }
            }
        }
    }

    override fun onCleared() {
        source.stop()
    }
}
