package dev.amaro.sonic

import kotlinx.coroutines.flow.MutableStateFlow

abstract class StateManager<T>(
    initialState: T,
    middlewareList: List<IMiddleware<T>> = listOf(DirectMiddleware())
) : IStateManager<T>, IProcessor<T> {
    protected val state = MutableStateFlow(initialState)
    private val middlewares: MutableList<IMiddleware<T>> = middlewareList.toMutableList()

    fun addMiddleware(middleware: IMiddleware<T>) {
        middlewares.add(middleware)
    }

    protected abstract val reducer: IReducer<T>

    override fun listen() = state

    override fun reduce(action: IAction) {
        state.value = reducer.reduce(action, state.value)
    }

    override fun perform(action: IAction) {
        middlewares.forEach { it.process(action, state.value, this) }
    }
}