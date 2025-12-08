package dev.amaro.sonic.app.samples.converter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.amaro.sonic.app.R
import dev.amaro.sonic.app.clicks
import dev.amaro.sonic.collectOnDefault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.onEach

class ConverterActivity : AppCompatActivity() {

    private lateinit var scope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_converter)
        setSupportActionBar(findViewById(R.id.toolbar))

        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        findViewById<FloatingActionButton>(R.id.fab).clicks()
            .onEach { goToSettings() }
            .collectOnDefault(scope) { }

    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun goToSettings() {
        val navController = supportFragmentManager.findFragmentById(R.id.main_navigation_host)
            ?.findNavController() ?: return
        navController.navigate(R.id.action_FirstFragment_to_SecondFragment)
    }
}