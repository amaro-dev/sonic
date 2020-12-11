package dev.amaro.sonic.app.samples.notes

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

    private val router = object : Router {
        private val map = HashMap<Int, View?>()
        override fun routeTo(destination: ViewDestination): View? = routeTo(destination.layoutId)

        override fun routeTo(destinationId: Int): View? {
            map[destinationId] = map[destinationId] ?: mapping(destinationId)
            return map[destinationId]
        }

        private fun mapping(destinationId: Int): View? = when (destinationId) {
            R.layout.screen_note_list -> NoteListScreen(this@NoteActivity)
            R.layout.screen_new_note -> CreateNoteScreen(this@NoteActivity)
            else -> null
        }
    }

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
        navigationHostView.router = if (intent.getBooleanExtra("NO_ROUTER", false))
            Router.Empty else router
        navController = Navigation.findNavController(navigationHostView)
        Navigation.setViewNavController(navigationHostView, navController)
    }
}