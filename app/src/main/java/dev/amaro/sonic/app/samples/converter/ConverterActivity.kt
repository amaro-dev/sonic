package dev.amaro.sonic.app.samples.converter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dev.amaro.sonic.app.R
import dev.amaro.sonic.app.clicks
import dev.amaro.sonic.collectOnDefault
import kotlinx.coroutines.flow.onEach

class ConverterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_converter)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).clicks()
            .onEach { goToSettings() }
            .collectOnDefault { }

    }

    private fun goToSettings() {
        findNavController(this, R.id.main_navigation_host)
            .navigate(R.id.action_FirstFragment_to_SecondFragment)
    }
}