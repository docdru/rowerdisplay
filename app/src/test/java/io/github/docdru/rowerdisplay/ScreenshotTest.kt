package io.github.docdru.rowerdisplay

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.docdru.rowerdisplay.ui.PlaceholderScreen
import io.github.docdru.rowerdisplay.ui.theme.RowerDisplayTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders screens on the JVM. CI uploads the images from build/outputs/roborazzi
 * so every pull request shows the phone and tablet layout.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = PHONE)
    fun phone() = capture("phone")

    @Test
    @Config(qualifiers = TABLET)
    fun tablet() = capture("tablet")

    private fun capture(name: String) {
        composeRule.setContent { RowerDisplayTheme { PlaceholderScreen() } }
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private companion object {
        const val PHONE = "w844dp-h390dp-land-xxhdpi"
        const val TABLET = "w1280dp-h800dp-land-mdpi"
    }
}
