package dev.amaro.sonic.app.samples.notes

import androidx.navigation.NavController
import dev.amaro.sonic.IAction
import dev.amaro.sonic.IReducer
import dev.amaro.sonic.StateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NoteStateManager(
    initialState: NoteState = NoteState(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) :
    StateManager<NoteState>(initialState, scope),
    KoinComponent {
    private val storage: IStorage by inject()

    init {
        val navController: NavController by inject()
        addMiddleware(Navigator(navController))
    }

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