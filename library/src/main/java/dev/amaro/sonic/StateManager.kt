package dev.amaro.sonic

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class StateManager<T>(
    initialState: T,
    private val scope: CoroutineScope,
    private val middlewares: MutableList<IMiddleware<T>> = mutableListOf(DirectMiddleware())
) : IStateManager<T>, IProcessor<T> {
    protected val state = MutableStateFlow(initialState)

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