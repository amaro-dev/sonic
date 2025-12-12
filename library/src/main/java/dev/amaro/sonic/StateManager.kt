package dev.amaro.sonic

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Core orchestrator of Sonic's unidirectional data flow.
 *
 * Responsibilities:
 * - Hold the authoritative state inside a [MutableStateFlow].
 * - Execute middleware in order when [perform] is called.
 * - Invoke the reducer to produce the next state.
 * - Dispatch side-effect actions declared via [ISideEffectAction].
 *
 * Provide the manager with a [CoroutineScope] tied to your feature's lifecycle (fragment,
 * activity, desktop window, CLI job). Callers typically expose the manager via DI or
 * create it alongside the scope.
 */
abstract class StateManager<T>(
    initialState: T,
    private val scope: CoroutineScope,
    private val middlewares: MutableList<IMiddleware<T>> = mutableListOf(DirectMiddleware())
) : IStateManager<T>, IProcessor<T> {
    protected val state = MutableStateFlow(initialState)

    /**
     * Appends middleware to the processing pipeline. Called during initialization.
     */
    fun addMiddleware(middleware: IMiddleware<T>) {
        middlewares.add(middleware)
    }

    protected abstract val reducer: IReducer<T>

    override fun listen() = state

    override fun reduce(action: IAction) {
        state.value = reducer.reduce(action, state.value)
        if (action is ISideEffectAction) {
            perform(action.sideEffect)
        }
    }

    override fun perform(action: IAction) {
        scope.launch {
            middlewares.forEach { it.process(action, state.value, this@StateManager) }
        }
    }

    override suspend fun scopedPerform(block: suspend () -> IAction): Job {
        return scope.launch {
            perform(block())
        }
    }
}
