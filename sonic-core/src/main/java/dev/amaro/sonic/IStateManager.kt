package dev.amaro.sonic

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

interface IStateManager<T> {
    /**
     * Listen to state changes.
     *
     * @return StateFlow that emits initial state and all subsequent state updates
     */
    fun listen(): StateFlow<T>

    /**
     * Perform an action (async).
     *
     * The action is passed through all middlewares. Middlewares may dispatch
     * additional actions as side effects. After all middlewares process the
     * action, nothing happens (unless middlewares dispatch side effects).
     *
     * @param action The action to perform (will be processed by all middlewares)
     */
    fun perform(action: IAction)

    /**
     * Perform an action asynchronously in StateManager's scope.
     *
     * The block is executed in StateManager's scope (launched asynchronously).
     * The action returned from block is then performed.
     *
     * Primarily used by state agents that need to:
     * - Schedule delayed actions (e.g., clearing results after timeout)
     * - Perform async work before dispatching
     * - Control job lifecycle (cancel previous work)
     *
     * Example (in state agent):
     * ```kotlin
     * private var clearJob: Job? = null
     *
     * override suspend fun process(state: AppState): AppState {
     *     val result = extractResult(state)
     *
     *     if (result != lastResult) {
     *         clearJob?.cancel()  // Cancel previous clearing job
     *         lastResult = result
     *
     *         if (result != null) {
     *             // Launch new job and track it for potential cancellation
     *             clearJob = stateManager.scopedPerform {
     *                 delay(3000)  // Wait before clearing
     *                 ClearStatus
     *             }
     *         }
     *     }
     *     return state
     * }
     * ```
     *
     * @param block Suspend block that performs async work and returns an action to perform
     * @return Job reference for cancellation or monitoring
     */
    suspend fun scopedPerform(block: suspend () -> IAction): Job
}
