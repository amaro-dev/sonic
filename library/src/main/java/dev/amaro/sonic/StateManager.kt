package dev.amaro.sonic

import kotlinx.coroutines.flow.MutableStateFlow

interface IAction

interface IReducer<T> {
    fun reduce(action: IAction, currentState: T): T
}

interface IProcessor<T> {
    fun perform(action: IAction)
    fun reduce(action: IAction)
}

interface IStateManager<T> {
    fun listen(): MutableStateFlow<T>
    fun perform(action: IAction)
}

abstract class Processor<T>(private val stateManager: IStateManager<T>) : IProcessor<T> {
    override fun perform(action: IAction) {
        stateManager.perform(action)
    }
}

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