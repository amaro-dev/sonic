package dev.amaro.sonic

/**
 * Reducer that operates on a specific slice (subset) of a larger state structure.
 *
 * This pattern enables type-safe domain separation by allowing reducers to work with
 * only their relevant portion of the state. The slice reducer extracts its slice,
 * applies a specialized reducer, and merges the result back into the full state.
 *
 * **Type Safety:** The reducer can only access and modify its declared slice type,
 * preventing accidental cross-domain modifications. The compiler enforces boundaries.
 *
 * **Benefits:**
 * - **Focused Reducers:** Each reducer handles only its domain (e.g., `UserReducer` only sees `UserState`)
 * - **Testability:** Test reducers with minimal state fixtures (just the slice)
 * - **Reusability:** Share reducers across different StateManagers
 * - **Scalability:** Manage complex state structures with clear boundaries
 *
 * Example:
 * ```kotlin
 * data class AppState(
 *     val user: UserState,
 *     val cart: CartState,
 *     val notifications: NotificationState
 * )
 *
 * class MyStateManager : StateManager<AppState>(...) {
 *     override val reducer = CompositeReducer(
 *         SliceReducer(
 *             selector = { it.user },
 *             updater = { state, user -> state.copy(user = user) },
 *             reducer = UserReducer()
 *         ),
 *         SliceReducer(
 *             selector = { it.cart },
 *             updater = { state, cart -> state.copy(cart = cart) },
 *             reducer = CartReducer()
 *         )
 *     )
 * }
 *
 * // UserReducer is type-safe: it can only access UserState, not CartState
 * class UserReducer : IReducer<UserState> {
 *     override fun reduce(action: IAction, currentState: UserState): UserState {
 *         return when (action) {
 *             is LoginAction -> currentState.copy(username = action.username)
 *             else -> currentState
 *         }
 *     }
 * }
 * ```
 *
 * **Performance Optimization:** Uses reference equality (===) to detect if the slice
 * changed. If the reducer returns the same slice instance, no state copy is performed.
 *
 * @param TState The full state type (e.g., `AppState`)
 * @param TSlice The slice type this reducer operates on (e.g., `UserState`)
 * @property selector Extracts the slice from the full state
 * @property updater Merges the updated slice back into the full state
 * @property reducer The reducer that operates on the slice
 */
class SliceReducer<TState, TSlice>(
    private val selector: (TState) -> TSlice,
    private val updater: (TState, TSlice) -> TState,
    private val reducer: IReducer<TSlice>
) : IReducer<TState> {

    /**
     * Applies the slice reducer to transform the state.
     *
     * Process:
     * 1. Extract the slice using the selector
     * 2. Apply the slice reducer to get the new slice
     * 3. If the slice changed (reference equality check), merge back using updater
     * 4. If unchanged, return original state (avoids unnecessary copies)
     *
     * @param action The action to process
     * @param currentState The current full state
     * @return The new full state with the slice potentially updated
     */
    override fun reduce(action: IAction, currentState: TState): TState {
        val slice = selector(currentState)
        val newSlice = reducer.reduce(action, slice)
        return if (newSlice !== slice) {
            updater(currentState, newSlice)
        } else {
            currentState
        }
    }
}
