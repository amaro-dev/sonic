package dev.amaro.sonic

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

fun <T> Flow<T>.collectOnDefault(
    scope: CoroutineScope,
    action: suspend (T) -> Unit
): Job = collectOn(scope, Dispatchers.Default, action)


fun <T> Flow<T>.collectOn(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    action: suspend (T) -> Unit
): Job = scope.launch(dispatcher) {
    collect { action(it) }
}