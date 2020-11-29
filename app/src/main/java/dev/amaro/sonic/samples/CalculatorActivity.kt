package dev.amaro.sonic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.amaro.sonic.samples.ActivityRenderer
import dev.amaro.sonic.samples.Calculator
import kotlinx.coroutines.Dispatchers

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Calculator.SimpleScreen(ActivityRenderer(this), Dispatchers.Main)
    }
}

