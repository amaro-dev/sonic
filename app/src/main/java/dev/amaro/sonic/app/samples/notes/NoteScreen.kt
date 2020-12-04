package dev.amaro.sonic.app.samples.notes

import dev.amaro.sonic.IRenderer
import dev.amaro.sonic.Screen
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class NoteScreen(
    renderer: IRenderer<NoteState>,
    initialState: NoteState = NoteState(),
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) : Screen<NoteState>(NoteStateManager(initialState), renderer, dispatcher) {
    init {
        perform(Action.Load)
    }
}