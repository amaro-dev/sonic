package dev.amaro.sonic

abstract class Processor<T>(private val stateManager: IStateManager<T>) : IProcessor<T> {
    override fun perform(action: IAction) {
        stateManager.perform(action)
    }
}



