package io.github.docdru.rowerdisplay.settings

import android.content.Context
import io.github.docdru.rowerdisplay.session.Goal
import io.github.docdru.rowerdisplay.session.Metric
import io.github.docdru.rowerdisplay.session.TargetBand

/** Saves [AppSettings] in the app's private SharedPreferences. Nothing leaves the device. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val defaults = AppSettings()
        val amount = prefs.getInt(KEY_GOAL_AMOUNT, -1)
        val goal = when (prefs.getString(KEY_GOAL_TYPE, null)) {
            TYPE_TIME -> if (amount > 0) Goal.Time(amount) else GoalPresets.defaultTime
            TYPE_DISTANCE -> if (amount > 0) Goal.Distance(amount) else GoalPresets.defaultDistance
            else -> defaults.goal
        }
        val targets = Metric.momentary.associateWith { metric ->
            val low = prefs.getFloat(targetKey(metric, "low"), Float.NaN)
            val high = prefs.getFloat(targetKey(metric, "high"), Float.NaN)
            if (low.isNaN() || high.isNaN()) {
                TargetBand.defaults.getValue(metric)
            } else {
                TargetBand(low.toDouble(), high.toDouble())
            }
        }
        return AppSettings(
            goal = goal,
            middle = metricNamed(prefs.getString(KEY_MIDDLE, null)) ?: defaults.middle,
            right = metricNamed(prefs.getString(KEY_RIGHT, null)) ?: defaults.right,
            targets = targets,
            demoMode = prefs.getBoolean(KEY_DEMO, false),
        ).sanitized()
    }

    fun save(settings: AppSettings) {
        val editor = prefs.edit()
        when (val goal = settings.goal) {
            is Goal.Time -> {
                editor.putString(KEY_GOAL_TYPE, TYPE_TIME)
                editor.putInt(KEY_GOAL_AMOUNT, goal.seconds)
            }
            is Goal.Distance -> {
                editor.putString(KEY_GOAL_TYPE, TYPE_DISTANCE)
                editor.putInt(KEY_GOAL_AMOUNT, goal.metres)
            }
        }
        editor.putString(KEY_MIDDLE, settings.middle.name)
        editor.putString(KEY_RIGHT, settings.right.name)
        settings.targets.forEach { (metric, band) ->
            editor.putFloat(targetKey(metric, "low"), band.low.toFloat())
            editor.putFloat(targetKey(metric, "high"), band.high.toFloat())
        }
        editor.putBoolean(KEY_DEMO, settings.demoMode)
        editor.apply()
    }

    private fun metricNamed(name: String?): Metric? = Metric.entries.firstOrNull { it.name == name }

    private fun targetKey(metric: Metric, edge: String) = "target_${metric.name}_$edge"

    private companion object {
        const val FILE = "settings"
        const val KEY_GOAL_TYPE = "goal_type"
        const val KEY_GOAL_AMOUNT = "goal_amount"
        const val KEY_MIDDLE = "slot_middle"
        const val KEY_RIGHT = "slot_right"
        const val KEY_DEMO = "demo_mode"
        const val TYPE_TIME = "time"
        const val TYPE_DISTANCE = "distance"
    }
}
