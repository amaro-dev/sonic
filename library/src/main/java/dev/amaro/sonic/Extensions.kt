package dev.amaro.sonic

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

inline fun <T> Flow<T>.collectOnDefault(crossinline action: suspend (value: T) -> Unit): Unit {
    collectOn(Dispatchers.Default, action)
}


inline fun <T> Flow<T>.collectOn(
    dispatcher: CoroutineDispatcher,
    crossinline action: suspend (value: T) -> Unit
) {
    CoroutineScope(dispatcher).launch {
        collect { action(it) }
    }
}