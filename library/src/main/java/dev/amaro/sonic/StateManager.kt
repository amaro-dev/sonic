package dev.amaro.sonic

import kotlinx.coroutines.flow.MutableStateFlow

interface IAction

interface IProcessor<T> {
    fun perform(action: IAction)
    fun reduce(action: IAction)
}

interface IStateManager<T> {
    fun listen(): MutableStateFlow<T>
    fun process(action: IAction)
}

abstract class Processor<T>(private val stateManager: IStateManager<T>) : IProcessor<T> {
    override fun perform(action: IAction) {
        stateManager.process(action)
    }
}

abstract class StateManager<T>(
    initialState: T,
    private val middlewareList: List<IMiddleware<T>> = listOf(DirectMiddleware())
) : IStateManager<T> {
    protected val state = MutableStateFlow(initialState)

    protected abstract val processor: IProcessor<T>

    override fun listen() = state

    override fun process(action: IAction) {
        middlewareList.forEach { it.process(action, state.value, processor) }
    }
}