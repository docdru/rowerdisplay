package io.github.docdru.rowerdisplay

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.docdru.rowerdisplay.ble.BleRowerSource
import io.github.docdru.rowerdisplay.ble.RowerSource
import io.github.docdru.rowerdisplay.session.Goal
import io.github.docdru.rowerdisplay.session.GoalAlert
import io.github.docdru.rowerdisplay.session.Metric
import io.github.docdru.rowerdisplay.session.SessionTracker
import io.github.docdru.rowerdisplay.session.TargetBand
import io.github.docdru.rowerdisplay.ui.RowingUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Connects a [RowerSource] to a [SessionTracker] and exposes what the screen shows.
 * M3: the real rower over Bluetooth with a 20-minute time goal; M4 adds the start
 * screen, settings and the simulated rower as a demo mode.
 */
class RowingViewModel(application: Application) : AndroidViewModel(application) {
    private val source: RowerSource = BleRowerSource(application)
    private val tracker = SessionTracker(Goal.Time(20 * 60))
    private val alert = GoalAlert(application)

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
            source.data.collect { update() }
        }
        // Also update once a second without new data, so a pause or a lost connection
        // stops the clock instead of freezing the last state.
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                update()
            }
        }
        viewModelScope.launch {
            source.connection.collect { connection ->
                _state.update { it.copy(connection = connection) }
            }
        }
    }

    private fun update() {
        val session = tracker.update(source.data.value, SystemClock.elapsedRealtime())
        if (session.justReachedGoal) alert.play()
        _state.update { it.copy(session = session) }
    }

    override fun onCleared() {
        source.stop()
    }
}
