package dev.amaro.sonic.app.samples.notes

import dev.amaro.sonic.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NoteStateManager(initialState: NoteState, middlewareList: List<IMiddleware<NoteState>>) :
    StateManager<NoteState>(initialState, middlewareList.plus(DirectMiddleware())),
    KoinComponent {
    private val storage: IStorage by inject()

    override val reducer: IReducer<NoteState> = object : IReducer<NoteState> {
        override fun reduce(action: IAction, currentState: NoteState): NoteState {
            return when (action) {
                is Action.Load -> {
                    NoteState(storage.list().sortedBy { it.title })
                }
                is Action.ToggleClosedNotes -> {
                    val flag = !currentState.showOnlyOpen
                    currentState.copy(
                        notes = storage.list().filter { !flag || !it.done }.sortedBy { it.title },
                        showOnlyOpen = flag
                    )
                }
                is Action.ToggleNote -> {
                    storage.update(action.note)
                    currentState.copy(
                        notes = storage.list().filter { !currentState.showOnlyOpen || !it.done }
                            .sortedBy { it.title },
                    )
                }
                is Action.AddNote -> {
                    storage.save(action.note)
                    currentState.copy(
                        notes = storage.list().filter { !currentState.showOnlyOpen || !it.done }
                            .sortedBy { it.title },
                    )
                }
                is Action.DeleteNote -> {
                    storage.delete(action.note)
                    currentState.copy(
                        notes = storage.list().filter { !currentState.showOnlyOpen || !it.done }
                            .sortedBy { it.title },
                    )
                }
                else -> currentState
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