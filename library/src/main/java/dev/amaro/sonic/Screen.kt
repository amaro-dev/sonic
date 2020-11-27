package dev.amaro.sonic

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface IPerformer<T> {
    fun perform(action: IAction)
}

abstract class Screen<T>(
    state: StateManager<T>,
    renderer: IRenderer<T> = IRenderer.Nothing(),
    collectScope: CoroutineDispatcher = Dispatchers.Main
) : IPerformer<T>, IRenderer<T> {
    private val stateManager: IStateManager<T> = state

    init {
        val renderTo = renderer.let {
            if (it is IRenderer.Nothing) ::render else it::render
        }
        stateManager.listen().collectOn(collectScope) { renderTo(it, this) }
    }

    override fun render(state: T, performer: IPerformer<T>) = Unit

    override fun perform(action: IAction) {
        stateManager.process(action)
    }
}

interface IRenderer<T> {
    fun render(state: T, performer: IPerformer<T>)
    class Nothing<T> : IRenderer<T> {
        override fun render(state: T, performer: IPerformer<T>) = Unit
    }
}
