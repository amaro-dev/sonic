package dev.amaro.sonic.app.samples.notes

import dev.amaro.sonic.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NoteStateManager(initialState: NoteState, middlewareList: List<IMiddleware<NoteState>>) :
    StateManager<NoteState>(initialState, middlewareList.plus(DirectMiddleware())),
    KoinComponent {
    private val storage: IStorage by inject()

    override val processor: IProcessor<NoteState> = object : Processor<NoteState>(this) {
        override fun reduce(action: IAction) {
            when (action) {
                is Action.Load -> {
                    state.value = NoteState(storage.list().sortedBy { it.title })
                }
                is Action.ToggleClosedNotes -> {
                    val flag = !state.value.showOnlyOpen
                    state.value = state.value.copy(
                        notes = storage.list().filter { !flag || !it.done }.sortedBy { it.title },
                        showOnlyOpen = flag
                    )
                }
                is Action.ToggleNote -> {
                    storage.update(action.note)
                    state.value = state.value.copy(
                        notes = storage.list().filter { !state.value.showOnlyOpen || !it.done }
                            .sortedBy { it.title },
                    )
                }
                is Action.AddNote -> {
                    storage.save(action.note)
                    state.value = state.value.copy(
                        notes = storage.list().filter { !state.value.showOnlyOpen || !it.done }
                            .sortedBy { it.title },
                    )
                }
                is Action.DeleteNote -> {
                    storage.delete(action.note)
                    state.value = state.value.copy(
                        notes = storage.list().filter { !state.value.showOnlyOpen || !it.done }
                            .sortedBy { it.title },
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
    object Cancel : Action()
    data class AddNote(val note: Note) : Action()
    data class DeleteNote(val note: Note) : Action()
    data class ToggleNote(val note: Note) : Action()
}