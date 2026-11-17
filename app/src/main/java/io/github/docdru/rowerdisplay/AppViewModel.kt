package io.github.docdru.rowerdisplay

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.docdru.rowerdisplay.ble.BleRowerSource
import io.github.docdru.rowerdisplay.ble.ConnectionState
import io.github.docdru.rowerdisplay.ble.RowerSource
import io.github.docdru.rowerdisplay.ble.SimulatedRower
import io.github.docdru.rowerdisplay.session.GoalAlert
import io.github.docdru.rowerdisplay.session.SessionState
import io.github.docdru.rowerdisplay.session.SessionTracker
import io.github.docdru.rowerdisplay.settings.AppSettings
import io.github.docdru.rowerdisplay.settings.SettingsStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Screen { Start, Rowing, Settings }

data class AppState(
    val screen: Screen = Screen.Start,
    val settings: AppSettings = AppSettings(),
    val connection: ConnectionState = ConnectionState.Idle,
    /** The running session; null on the start screen. */
    val session: SessionState? = null,
    /** Where the settings screen returns to. */
    val settingsReturnTo: Screen = Screen.Start,
)

/**
 * The app's single source of truth: settings, the connection (made already on the start
 * screen, so the rower is ready), and the session once started.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SettingsStore(application)
    private val alert = GoalAlert(application)

    private val _state = MutableStateFlow(AppState(settings = store.load()))
    val state: StateFlow<AppState> = _state.asStateFlow()

    private var source: RowerSource = createSource(_state.value.settings.demoMode)
    private var sourceJobs: List<Job> = emptyList()
    private var tracker: SessionTracker? = null

    init {
        attach(source)
        // Also update once a second without new data, so a pause or a lost connection
        // stops the clock instead of freezing the last state.
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                updateSession()
            }
        }
    }

    fun startSession() {
        tracker = SessionTracker(_state.value.settings.goal)
        _state.update { it.copy(screen = Screen.Rowing) }
        updateSession()
    }

    fun endSession() {
        tracker = null
        _state.update { it.copy(screen = Screen.Start, session = null) }
    }

    fun openSettings() {
        _state.update { it.copy(screen = Screen.Settings, settingsReturnTo = it.screen) }
    }

    fun closeSettings() {
        _state.update { it.copy(screen = it.settingsReturnTo) }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val old = _state.value.settings
        val new = transform(old)
        if (new == old) return
        store.save(new)
        _state.update { it.copy(settings = new) }
        if (new.demoMode != old.demoMode) attach(createSource(new.demoMode))
    }

    private fun createSource(demo: Boolean): RowerSource =
        if (demo) SimulatedRower() else BleRowerSource(getApplication())

    private fun attach(newSource: RowerSource) {
        sourceJobs.forEach { it.cancel() }
        if (newSource !== source) source.stop()
        source = newSource
        newSource.start(viewModelScope)
        sourceJobs = listOf(
            viewModelScope.launch { newSource.data.collect { updateSession() } },
            viewModelScope.launch {
                newSource.connection.collect { c -> _state.update { it.copy(connection = c) } }
            },
        )
    }

    private fun updateSession() {
        val tracker = tracker ?: return
        val session = tracker.update(source.data.value, SystemClock.elapsedRealtime())
        if (session.justReachedGoal) alert.play()
        _state.update { it.copy(session = session) }
    }

    override fun onCleared() {
        source.stop()
    }
}
