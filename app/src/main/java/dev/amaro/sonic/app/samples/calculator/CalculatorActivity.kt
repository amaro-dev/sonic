package dev.amaro.sonic.app.samples.calculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.amaro.sonic.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MainActivity : AppCompatActivity() {
    private lateinit var scope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        Calculator.SimpleScreen(ActivityRenderer(this), scope)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

