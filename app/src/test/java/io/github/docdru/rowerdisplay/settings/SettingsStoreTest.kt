package io.github.docdru.rowerdisplay.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.docdru.rowerdisplay.session.Goal
import io.github.docdru.rowerdisplay.session.Metric
import io.github.docdru.rowerdisplay.session.TargetBand
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun firstStartGivesDefaults() {
        assertEquals(AppSettings(), SettingsStore(context).load())
    }

    @Test
    fun savedSettingsComeBack() {
        val settings = AppSettings(
            goal = Goal.Distance(5000),
            middle = Metric.Power,
            right = Metric.Time,
            targets = TargetBand.defaults + (Metric.Power to TargetBand(160.0, 200.0)),
            demoMode = true,
        )
        SettingsStore(context).save(settings)
        assertEquals(settings, SettingsStore(context).load())
    }
}
