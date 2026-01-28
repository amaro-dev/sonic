package dev.amaro.sonic

/**
 * Builder functions and DSL for creating composite and slice reducers with less boilerplate.
 */

/**
 * Creates a slice reducer with a more concise syntax.
 *
 * Convenience function that wraps [SliceReducer] construction for better readability.
 *
 * Example:
 * ```kotlin
 * override val reducer = CompositeReducer(
 *     sliceReducer(
 *         get = { it.user },
 *         set = { state, user -> state.copy(user = user) },
 *         reducer = UserReducer()
 *     ),
 *     sliceReducer(
 *         get = { it.cart },
 *         set = { state, cart -> state.copy(cart = cart) },
 *         reducer = CartReducer()
 *     )
 * )
 * ```
 *
 * @param TState The full state type
 * @param TSlice The slice type
 * @param get Function to extract the slice from the full state
 * @param set Function to update the full state with a new slice
 * @param reducer The reducer that operates on the slice
 * @return A configured [SliceReducer]
 */
fun <TState, TSlice> sliceReducer(
    get: (TState) -> TSlice,
    set: (TState, TSlice) -> TState,
    reducer: IReducer<TSlice>
): SliceReducer<TState, TSlice> {
    return SliceReducer(
        selector = get,
        updater = set,
        reducer = reducer
    )
}

/**
 * DSL builder for creating composite reducers with a more fluent syntax.
 *
 * Provides a scoped context for building complex composite reducers with
 * a mix of regular and slice reducers.
 *
 * Example:
 * ```kotlin
 * override val reducer = buildCompositeReducer<AppState> {
 *     // Add regular reducers
 *     add(GlobalReducer())
 *
 *     // Add slice reducers with inline configuration
 *     slice(
 *         get = { it.user },
 *         set = { state, user -> state.copy(user = user) },
 *         reducer = UserReducer()
 *     )
 *
 *     slice(
 *         get = { it.cart },
 *         set = { state, cart -> state.copy(cart = cart) },
 *         reducer = CartReducer()
 *     )
 * }
 * ```
 *
 * @param TState The state type managed by the composite reducer
 * @param block DSL block for configuring reducers
 * @return A configured [CompositeReducer]
 */
fun <TState> buildCompositeReducer(
    block: CompositeReducerBuilder<TState>.() -> Unit
): CompositeReducer<TState> {
    val builder = CompositeReducerBuilder<TState>()
    builder.block()
    return builder.build()
}

/**
 * Builder context for constructing composite reducers with a DSL.
 *
 * @param TState The state type managed by the composite reducer
 */
class CompositeReducerBuilder<TState> {
    private val reducers = mutableListOf<IReducer<TState>>()

    /**
     * Adds a regular reducer to the composite.
     *
     * @param reducer The reducer to add
     */
    fun add(reducer: IReducer<TState>) {
        reducers.add(reducer)
    }

    /**
     * Adds a slice reducer to the composite.
     *
     * @param TSlice The slice type
     * @param get Function to extract the slice from the full state
     * @param set Function to update the full state with a new slice
     * @param reducer The reducer that operates on the slice
     */
    fun <TSlice> slice(
        get: (TState) -> TSlice,
        set: (TState, TSlice) -> TState,
        reducer: IReducer<TSlice>
    ) {
        reducers.add(SliceReducer(get, set, reducer))
    }

    /**
     * Builds the composite reducer from the accumulated reducers.
     *
     * @return A configured [CompositeReducer]
     */
    internal fun build(): CompositeReducer<TState> {
        return CompositeReducer(reducers)
    }
}
