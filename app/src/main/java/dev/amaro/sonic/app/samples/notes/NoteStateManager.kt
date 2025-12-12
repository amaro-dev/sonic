package dev.amaro.sonic.app.samples.notes

import dev.amaro.sonic.*
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
        val navController = runCatching { getKoin().get<androidx.navigation.NavController>() }.getOrNull()
        navController?.let { addMiddleware(Navigator(it)) }
    }

    private fun operationSucceeded(operation: String): ResultInfo.Success =
        success("STORAGE")
            .metadata("operation", operation)
            .buildSuccess()

    private fun storageFailure(message: String, cause: Throwable? = null): ResultInfo.Failure {
        val builder = failure("STORAGE_ERROR", message)
        builder.retryable(true)
        builder.cause = cause
        return builder.buildFailure()
    }

    override val reducer: IReducer<NoteState> = object : IReducer<NoteState> {
        override fun reduce(action: IAction, currentState: NoteState): NoteState {
            return when (action) {
                is Action.Load -> {
                    runCatching { storage.list().sortedBy { it.title } }
                        .fold(
                            onSuccess = {
                                NoteState(
                                    notes = it,
                                    result = operationSucceeded("load")
                                )
                            },
                            onFailure = {
                                currentState.copy(result = storageFailure("Failed to load notes", it))
                            }
                        )
                }
                is Action.LoadSuccess -> {
                    currentState.copy(
                        notes = storage.list().sortedBy { it.title },
                        result = action.result
                    )
                }

                is Action.LoadFailed -> {
                    currentState.copy(result = action.result)
                }
                is Action.ToggleClosedNotes -> {
                    val flag = !currentState.showOnlyOpen
                    currentState.copy(
                        notes = storage.list().filter { !flag || !it.done }.sortedBy { it.title },
                        showOnlyOpen = flag,
                        result = operationSucceeded("toggle_filter")
                    )
                }
                is Action.ToggleNote -> {
                    runCatching { storage.update(action.note) }
                        .fold(
                            onSuccess = {
                                currentState.copy(
                                    notes = storage.list()
                                        .filter { !currentState.showOnlyOpen || !it.done }
                                        .sortedBy { it.title },
                                    result = operationSucceeded("toggle_note")
                                )
                            },
                            onFailure = {
                                currentState.copy(result = storageFailure("Failed to update note", it))
                            }
                        )
                }
                is Action.AddNote -> {
                    if (action.note.title.isBlank()) {
                        currentState.copy(
                            result = failure("VALIDATION", "Note title cannot be empty")
                                .retryable(false)
                                .buildFailure()
                        )
                    } else {
                        runCatching { storage.save(action.note) }
                            .fold(
                                onSuccess = {
                                    currentState.copy(
                                        notes = storage.list()
                                            .filter { !currentState.showOnlyOpen || !it.done }
                                            .sortedBy { it.title },
                                        result = operationSucceeded("add_note")
                                    )
                                },
                                onFailure = {
                                    currentState.copy(result = storageFailure("Failed to add note", it))
                                }
                            )
                    }
                }
                is Action.DeleteNote -> {
                    runCatching { storage.delete(action.note) }
                        .fold(
                            onSuccess = {
                                currentState.copy(
                                    notes = storage.list()
                                        .filter { !currentState.showOnlyOpen || !it.done }
                                        .sortedBy { it.title },
                                    result = operationSucceeded("delete_note")
                                )
                            },
                            onFailure = {
                                currentState.copy(result = storageFailure("Failed to delete note", it))
                            }
                        )
                }

                is Action.ClearResult -> {
                    currentState.copy(result = null)
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
    data class LoadSuccess(val result: ResultInfo.Success) : Action()
    data class LoadFailed(val result: ResultInfo.Failure) : Action()
    object ClearResult : Action()
}
