package dev.amaro.sonic.app.samples.notes

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import dev.amaro.navigation.NavigationHostView
import dev.amaro.navigation.Router
import dev.amaro.navigation.ViewDestination

import dev.amaro.sonic.app.R
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class NoteActivity : AppCompatActivity() {
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)
        startKoin {
            androidContext(this@NoteActivity)
            modules(
                module { factory { findNavController(R.id.main_navigation_host) } },
                NoteModule.Instance
            )
        }
        val navigationHostView = findViewById<NavigationHostView>(R.id.main_navigation_host)
        navigationHostView.router = Direct(this)
        navController = Navigation.findNavController(navigationHostView)
        Navigation.setViewNavController(navigationHostView, navController)
    }
}

class Direct(private val context: Context) : Router {
    override fun routeTo(destination: ViewDestination): View? {
        if (destination.className == null) return null
        return Class.forName(destination.className!!)
            .getConstructor(Context::class.java)
            .newInstance(context) as View?
    }
}