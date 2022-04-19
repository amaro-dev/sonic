package dev.amaro.sonic.app.samples.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme

class ComposeActivity: ComponentActivity() {

    private lateinit var stateManager: Calculator.SimpleStateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateManager = Calculator.SimpleStateManager()
        setContent {
            MaterialTheme {
                ComposeRenderer(stateManager)
            }
        }

    }
}