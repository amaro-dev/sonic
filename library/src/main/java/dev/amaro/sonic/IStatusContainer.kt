package dev.amaro.sonic

/**
 * Optional interface for type-safe extraction of result information from state.
 *
 * This is an optional mixin interface that state classes can implement to provide
 * a standardized way to extract [ResultInfo] from state structures. This is NOT
 * required - state classes can manage results in any way they prefer.
 *
 * Useful when building state agents (like Result Clearing Agent) that need to
 * generically extract result information from different state types without
 * knowing the state structure details.
 *
 * Implementation example:
 * ```
 * data class MyState(
 *     override val resultInfo: ResultInfo? = null,
 *     val data: String = ""
 * ) : IStatusContainer {
 *     // resultInfo property inherited from interface
 * }
 * ```
 *
 * Usage in state agents:
 * ```
 * override suspend fun process(state: AppState): AppState {
 *     // Only works if state implements IStatusContainer
 *     val resultInfo = (state as? IStatusContainer)?.resultInfo
 *
 *     if (resultInfo != null) {
 *         // Handle the result
 *         // Schedule clearing, notify UI, etc.
 *     }
 *     return state
 * }
 * ```
 *
 * Note: This interface is completely optional. State classes that don't need
 * result tracking or use alternative result storage patterns need not implement it.
 */
interface IStatusContainer {
    /**
     * The result information from the current state.
     *
     * @return The [ResultInfo] from state, or null if no result is present.
     *         Implementing classes should return the current/latest result.
     */
    val resultInfo: ResultInfo?
}