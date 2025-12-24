package dev.amaro.sonic

/**
 * Optional interface for type-safe extraction of status information from state.
 *
 * This is an optional mixin interface that state classes can implement to provide
 * a standardized way to extract [Status] from state structures. This is NOT
 * required - state classes can manage results in any way they prefer.
 *
 * Useful when building state agents (like Result Clearing Agent) that need to
 * generically extract result information from different state types without
 * knowing the state structure details.
 *
 * Implementation example:
 * ```
 * data class MyState(
 *     override val status: Status? = null,
 *     val data: String = ""
 * ) : IStatusContainer {
 *     // status property inherited from interface
 * }
 * ```
 *
 * Usage in state agents:
 * ```
 * override suspend fun process(state: AppState): AppState {
 *     // Only works if state implements IStatusContainer
 *     val status = (state as? IStatusContainer)?.status
 *
 *     if (status != null) {
 *         // Handle the status
 *         // Schedule clearing, notify UI, etc.
 *     }
 *     return state
 * }
 * ```
 *
 * Note: This interface is completely optional. State classes that don't need
 * status tracking or use alternative status storage patterns need not implement it.
 */
interface IStatusContainer {
    /**
     * The status information from the current state.
     *
     * @return The [Status] from state, or null if no status is present.
     *         Implementing classes should return the current/latest status.
     */
    val status: Status?
}
