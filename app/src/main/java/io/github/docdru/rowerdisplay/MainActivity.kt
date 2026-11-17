package io.github.docdru.rowerdisplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.docdru.rowerdisplay.ui.RowingScreen
import io.github.docdru.rowerdisplay.ui.theme.RowerDisplayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RowerDisplayTheme {
                val viewModel: RowingViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                RowingScreen(state)
            }
        }
    }
}
