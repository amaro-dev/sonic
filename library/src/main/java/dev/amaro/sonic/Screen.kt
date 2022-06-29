package dev.amaro.sonic

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

abstract class Screen<T>(
    state: StateManager<T>,
    renderer: IRenderer<T> = IRenderer.Nothing(),
    collectScope: CoroutineDispatcher = Dispatchers.Main
) : IPerformer<T>, IRenderer<T> {
    private val stateManager: IStateManager<T> = state
    private val scopeJob: Job

    init {
        val renderTo = renderer.let {
            if (it is IRenderer.Nothing) ::render else it::render
        }
        scopeJob = CoroutineScope(collectScope).launch {
            stateManager.listen().collect { renderTo(it, this@Screen) }
        }
    }

    override fun render(state: T, performer: IPerformer<T>) = Unit

    override fun perform(action: IAction) {
        stateManager.perform(action)
    }

    fun dispose() {
        scopeJob.cancel()
    }
}

