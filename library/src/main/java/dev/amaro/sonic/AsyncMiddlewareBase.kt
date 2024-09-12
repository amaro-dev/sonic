package dev.amaro.sonic

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class AsyncMiddlewareBase<T>(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : IMiddleware<T> {
    final override fun process(action: IAction, state: T, processor: IProcessor<T>) {
        coroutineScope.launch { asyncProcess(action, state, processor) }
    }

    abstract suspend fun asyncProcess(action: IAction, state: T, processor: IProcessor<T>)
}