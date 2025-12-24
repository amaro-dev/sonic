package dev.amaro.sonic

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * A state agent that automatically clears [Status] after configurable delays.
 *
 * This agent demonstrates the core pattern for building state agents that react to
 * state changes and dispatch actions. It automatically removes success and error
 * results from state after a specified duration, keeping the UI uncluttered.
 *
 * **Key Features**:
 * - Automatic clearing of results after configurable delays
 * - Different delays for success (3s) and error (5s) states
 * - Option to skip clearing non-retryable errors
 * - Loop prevention via change detection
 * - Job cancellation when results change
 * - Verification before clearing to prevent stale actions
 *
 * **Loop Prevention Pattern**:
 * This agent implements the core loop prevention pattern all agents must follow:
 * 1. Track the last result seen (`lastResult` field)
 * 2. Only process if result CHANGED (not equal to lastResult)
 * 3. Update tracking before dispatching
 * 4. This ensures agent doesn't dispatch repeatedly on same result
 *
 * **Job Cancellation**:
 * When a new result arrives, the previous clearing job is cancelled immediately.
 * This implements "timer reset" behavior - new result restarts the delay.
 *
 * **Verification Before Clearing**:
 * Before dispatching the clear action, the agent double-checks that the result
 * hasn't changed during the delay period. If it has, NOOP is dispatched instead.
 *
 * Usage:
 * ```kotlin
 * data class MyState(
 *     val status: Status? = null,
 *     val data: String = ""
 * )
 *
 * // ClearStatus is provided as a canonical action in sonic-result.
 *
 * stateManager.listen()
 *     .withAgent(StatusClearingAgent(
 *         stateManager = stateManager,
 *         config = ResultClearingConfig(
 *             successClearDelayMs = 3000,
 *             errorClearDelayMs = 5000,
 *             clearOnlyRetryableErrors = false
 *         ),
 *         extractResult = { it.status }
 *     ))
 *     .collectAsState()
 * ```
 *
 * **Thread Safety**: All operations are single-threaded via StateManager's
 * dispatcher. No external synchronization needed.
 *
 * **Performance**: Minimal overhead. Each state update is O(1). Jobs are
 * reused efficiently via cancellation.
 *
 * @param stateManager The StateManager that provides state and schedules actions
 * @param config Configuration for clearing delays and behavior
 * @param extractResult Function to extract Status from state (may return null)
 */
class StatusClearingAgent<T>(
    private val stateManager: IStateManager<T>,
    private val config: ResultClearingConfig,
    private val extractResult: (T) -> Status?
) : IStateAgent<T> {

    /**
     * Tracks the last result we've seen.
     * Used to detect when result CHANGES (loop prevention).
     */
    private var lastResult: Status? = null

    /**
     * Reference to the current clearing job.
     * Allows us to cancel it when a new result arrives.
     */
    private var clearJob: Job? = null

    companion object {
        private object NOOPAction : IAction

        private val NOOP: IAction = NOOPAction
    }

    override suspend fun process(state: T): T {
        val result = extractResult(state)

        // LOOP PREVENTION: Only process if result changed
        if (result != lastResult) {
            // Cancel any previous clearing job when result changes
            clearJob?.cancel()

            // Update tracking for next comparison
            lastResult = result

            // Only schedule clearing if result is not null
            if (result != null) {
                // Determine delay based on result type and configuration
                val delay = when (result) {
                    is Status.Running -> return state
                    is Status.Success -> config.successClearDelayMs
                    is Status.Failure -> {
                        // Skip clearing non-retryable errors if configured
                        if (config.clearOnlyRetryableErrors && !result.retryable) {
                            return state  // Return early, don't schedule
                        }
                        config.errorClearDelayMs
                    }
                }

                // Schedule clearing via scopedPerform for async execution
                clearJob = stateManager.scopedPerform {
                    // Wait the configured duration
                    delay(delay)

                    // VERIFICATION: Check result hasn't changed during delay
                    if (extractResult(stateManager.listen().value) == result) {
                        // Result unchanged, safe to clear
                        ClearStatus
                    } else {
                        // Result changed during delay, don't clear
                        NOOP
                    }
                }
            }
        }

        // Agent never modifies state - always return as-is
        return state
    }
}
