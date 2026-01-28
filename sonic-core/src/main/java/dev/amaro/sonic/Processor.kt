package dev.amaro.sonic

abstract class Processor<T>(private val stateManager: IStateManager<T>) : IProcessor<T> {
    override fun perform(action: IAction) {
        stateManager.perform(action)
    }

    override fun reduce(action: IAction) {
        (stateManager as? StateManager<T>)?.reduce(action)
            ?: throw UnsupportedOperationException("StateManager does not support reduce()")
    }

    override fun schedule(action: IAction) {
        (stateManager as? StateManager<T>)?.schedule(action)
            ?: throw UnsupportedOperationException("StateManager does not support schedule()")
    }
}



