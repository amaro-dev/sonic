package dev.amaro.sonic

/**
 * Composes multiple reducers into a single reducer that applies them sequentially.
 *
 * Each reducer in the chain receives the output state from the previous reducer.
 * This enables modular reducer design where different reducers handle different
 * aspects of the state or different action types.
 *
 * **Execution Order:** Reducers are applied left-to-right in the order provided.
 * If multiple reducers modify the same state property, the rightmost reducer wins.
 *
 * **Use Cases:**
 * - Split large monolithic reducers into focused domain-specific reducers
 * - Combine reducers handling different action type families
 * - Create reusable reducer components that can be composed
 *
 * Example:
 * ```kotlin
 * class MyStateManager : StateManager<AppState>(...) {
 *     override val reducer = CompositeReducer(
 *         AuthReducer(),      // Handles auth actions
 *         CartReducer(),      // Handles cart actions
 *         NotificationReducer() // Handles notification actions
 *     )
 * }
 * ```
 *
 * **Important:** All reducers receive all actions. Each reducer should return
 * `currentState` unchanged for actions it doesn't handle.
 *
 * @param T The state type managed by this reducer
 * @property reducers The list of reducers to compose, applied in order
 */
class CompositeReducer<T>(
    private val reducers: List<IReducer<T>>
) : IReducer<T> {

    /**
     * Convenience constructor accepting varargs for fluent composition.
     *
     * Example:
     * ```kotlin
     * val reducer = CompositeReducer(
     *     reducer1,
     *     reducer2,
     *     reducer3
     * )
     * ```
     */
    constructor(vararg reducers: IReducer<T>) : this(reducers.toList())

    /**
     * Applies all reducers sequentially to transform the state.
     *
     * The reduction is implemented as a left fold: each reducer receives the output
     * state from the previous reducer. The final state after all reducers have
     * processed the action is returned.
     *
     * @param action The action to process
     * @param currentState The current state before reduction
     * @return The new state after all reducers have been applied
     */
    override fun reduce(action: IAction, currentState: T): T {
        return reducers.fold(currentState) { state, reducer ->
            reducer.reduce(action, state)
        }
    }
}
