package dev.amaro.sonic

interface IScreen<T> {
    fun render(state: T)
    fun perform(action: IAction)
}

abstract class Screen<T>(state: StateManager<T>) : IScreen<T> {
    protected val stateManager: StateManager<T> = state

    init {
        stateManager.listen().collectOnDefault { render(it) }
    }

    override fun perform(action: IAction) {
        stateManager.process(action)
    }
}