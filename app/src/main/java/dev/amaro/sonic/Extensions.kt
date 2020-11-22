package dev.amaro.sonic

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

inline fun <T> Flow<T>.collectOnDefault(crossinline action: suspend (value: T) -> Unit): Unit {
    collectOn(Dispatchers.Default, action)
}

inline fun <T> Flow<T>.collectOn(
    dispatcher: CoroutineDispatcher,
    crossinline action: suspend (value: T) -> Unit
): Unit {
    GlobalScope.launch(dispatcher) {
        collect { action(it) }
    }
}