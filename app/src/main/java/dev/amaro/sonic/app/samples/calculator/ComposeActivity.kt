package dev.amaro.sonic.app.samples.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class ComposeActivity: ComponentActivity() {

    private lateinit var stateManager: Calculator.SimpleStateManager
    private lateinit var scope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        stateManager = Calculator.SimpleStateManager(scope = scope)
        setContent {
            MaterialTheme {
                ComposeRenderer(stateManager)
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}