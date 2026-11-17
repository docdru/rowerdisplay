package io.github.docdru.rowerdisplay

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.docdru.rowerdisplay.ble.ConnectionState
import io.github.docdru.rowerdisplay.ble.SimulatedRower
import io.github.docdru.rowerdisplay.session.Goal
import io.github.docdru.rowerdisplay.session.Metric
import io.github.docdru.rowerdisplay.session.SessionTracker
import io.github.docdru.rowerdisplay.session.TargetBand
import io.github.docdru.rowerdisplay.settings.AppSettings
import io.github.docdru.rowerdisplay.ui.RowingScreen
import io.github.docdru.rowerdisplay.ui.RowingUiState
import io.github.docdru.rowerdisplay.ui.SettingsScreen
import io.github.docdru.rowerdisplay.ui.StartScreen
import io.github.docdru.rowerdisplay.ui.theme.RowerDisplayTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the rowing screen on the JVM with data from the simulated rower. CI publishes
 * the images in the pull request, so every change can be judged on a phone.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = PHONE)
    fun phoneTimeGoal() = capture("phone_time_goal", timeGoal(atSecond = 440))

    @Test
    @Config(qualifiers = PHONE)
    fun phoneTimeGoalPassed() = capture("phone_time_goal_passed", timeGoal(atSecond = 1335))

    @Test
    @Config(qualifiers = PHONE)
    fun phoneDistanceGoal() = capture("phone_distance_goal", distanceGoal(atSecond = 470))

    @Test
    @Config(qualifiers = PHONE)
    fun phoneDistanceGoalPassed() =
        capture("phone_distance_goal_passed", distanceGoal(atSecond = 1400))

    @Test
    @Config(qualifiers = TABLET)
    fun tabletTimeGoalPassed() = capture("tablet_time_goal_passed", timeGoal(atSecond = 1335))

    @Test
    @Config(qualifiers = TABLET)
    fun tabletDistanceGoalPassed() =
        capture("tablet_distance_goal_passed", distanceGoal(atSecond = 1400))

    @Test
    @Config(qualifiers = TABLET)
    fun tabletTimeGoal() = capture("tablet_time_goal", timeGoal(atSecond = 440))

    @Test
    @Config(qualifiers = TABLET)
    fun tabletDistanceGoal() = capture("tablet_distance_goal", distanceGoal(atSecond = 470))

    @Test
    @Config(qualifiers = PHONE)
    fun phoneStartTime() = captureStart("phone_start_time", Goal.Time(20 * 60))

    @Test
    @Config(qualifiers = PHONE)
    fun phoneStartDistance() = captureStart("phone_start_distance", Goal.Distance(2000))

    @Test
    @Config(qualifiers = TABLET)
    fun tabletStartTime() = captureStart("tablet_start_time", Goal.Time(20 * 60))

    @Test
    @Config(qualifiers = PHONE)
    fun phoneSettings() = captureSettings("phone_settings")

    @Test
    @Config(qualifiers = TABLET)
    fun tabletSettings() = captureSettings("tablet_settings")

    private fun timeGoal(atSecond: Int) =
        uiState(Goal.Time(20 * 60), atSecond, Metric.Pace, Metric.Distance)

    private fun distanceGoal(atSecond: Int) =
        uiState(Goal.Distance(5000), atSecond, Metric.Power, Metric.Time)

    private fun uiState(goal: Goal, atSecond: Int, middle: Metric, right: Metric): RowingUiState {
        val tracker = SessionTracker(goal)
        for (s in 0..atSecond) tracker.update(SimulatedRower.dataAt(s), s * 1000L)
        return RowingUiState(
            connection = ConnectionState.Connected("DOMYOS-ROW-1580"),
            session = tracker.state,
            middle = middle,
            right = right,
            targets = TargetBand.defaults,
        )
    }

    private fun capture(name: String, state: RowingUiState) =
        render(name) { RowingScreen(state) }

    private fun captureStart(name: String, goal: Goal) = render(name) {
        StartScreen(
            goal = goal,
            connection = ConnectionState.Scanning,
            onGoalChange = {},
            onStart = {},
        )
    }

    private fun captureSettings(name: String) = render(name) {
        SettingsScreen(settings = AppSettings(), onChange = {}, onBack = {})
    }

    private fun render(name: String, content: @Composable () -> Unit) {
        composeRule.setContent { RowerDisplayTheme { content() } }
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private companion object {
        const val PHONE = "w844dp-h390dp-land-xxhdpi"
        const val TABLET = "w1280dp-h800dp-land-mdpi"
    }
}
