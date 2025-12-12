package dev.amaro.sonic

/**
 * Pure function that transforms state based on an [action].
 *
 * Reducers must be deterministic and side-effect free: the same input pair
 * `(action, currentState)` always returns the same output state without touching
 * databases, network, or dispatchers. Keep mutations out of reducers—delegate them to
 * middleware or state agents.
 *
 * Example:
 * ```kotlin
 * class CounterReducer : IReducer<CounterState> {
 *     override fun reduce(action: IAction, currentState: CounterState): CounterState =
 *         when (action) {
 *             CounterAction.Increment -> currentState.copy(value = currentState.value + 1)
 *             else -> currentState
 *         }
 * }
 * ```
 */
interface IReducer<T> {
    fun reduce(action: IAction, currentState: T): T
}
