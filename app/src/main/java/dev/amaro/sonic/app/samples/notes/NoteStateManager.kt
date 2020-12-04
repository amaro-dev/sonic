package dev.amaro.sonic.app.samples.notes

import dev.amaro.sonic.IAction
import dev.amaro.sonic.IProcessor
import dev.amaro.sonic.Processor
import dev.amaro.sonic.StateManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NoteStateManager(initialState: NoteState) : StateManager<NoteState>(initialState),
    KoinComponent {
    private val storage: IStorage by inject()

    override val processor: IProcessor<NoteState> = object : Processor<NoteState>(this) {
        override fun reduce(action: IAction) {
            when (action) {
                is Action.Load -> {
                    state.value = NoteState(storage.list())
                }
                is Action.ToggleClosedNotes -> {
                    val flag = !state.value.showOnlyOpen
                    state.value = state.value.copy(
                        notes = storage.list().filter { !flag || !it.done },
                        showOnlyOpen = flag
                    )
                }
                is Action.ToggleNote -> {
                    storage.update(action.note)
                    state.value = state.value.copy(
                        notes = storage.list().filter { !state.value.showOnlyOpen || !it.done },
                    )
                }
                is Action.AddNote -> {
                    storage.save(action.note)
                    state.value = state.value.copy(
                        notes = storage.list().filter { !state.value.showOnlyOpen || !it.done },
                    )
                }
            }
        }
    }
}

sealed class Action : IAction {
    object Load : Action()
    object ToggleClosedNotes : Action()
    object NewNote : Action()
    data class AddNote(val note: Note) : Action()
    data class ToggleNote(val note: Note) : Action()
}