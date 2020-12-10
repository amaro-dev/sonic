package dev.amaro.sonic.app.samples.notes

import androidx.navigation.NavController
import dev.amaro.sonic.IAction
import dev.amaro.sonic.IMiddleware
import dev.amaro.sonic.IProcessor
import dev.amaro.sonic.app.R

class Navigator(private val navController: NavController) : IMiddleware<NoteState> {
    override fun process(action: IAction, state: NoteState, processor: IProcessor<NoteState>) {
        when (action) {
            is Action.NewNote -> navController.navigate(R.id.createNoteScreen)
            is Action.AddNote -> navController.navigate(R.id.noteListScreen)
            is Action.Cancel -> navController.navigate(R.id.noteListScreen)
        }
    }
}