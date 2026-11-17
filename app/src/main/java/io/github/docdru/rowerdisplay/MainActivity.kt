package io.github.docdru.rowerdisplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.docdru.rowerdisplay.ui.PlaceholderScreen
import io.github.docdru.rowerdisplay.ui.theme.RowerDisplayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RowerDisplayTheme {
                PlaceholderScreen()
            }
        }
    }
}
