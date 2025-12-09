package dev.amaro.sonic

/**
 * Configuration for result clearing behavior in the Result Clearing Agent.
 *
 * Defines timing and conditions for automatic removal of [ResultInfo] from state
 * after successful or failed operations complete. Use this config to control how
 * long results remain visible to the user before being cleared.
 *
 * By default: successes clear after 3 seconds, errors after 5 seconds, and all
 * error types are cleared regardless of retryability.
 *
 * Usage in state management:
 * ```
 * val config = ResultClearingConfig(
 *     successClearDelayMs = 2000,      // Clear successes faster
 *     errorClearDelayMs = 8000,        // Keep errors longer for visibility
 *     clearOnlyRetryableErrors = true  // Only auto-clear retryable errors
 * )
 *
 * // Pass to Result Clearing Agent during initialization
 * val resultClearer = ResultClearingAgent(stateManager, config)
 * ```
 *
 * @param successClearDelayMs Duration in milliseconds to wait before clearing successful results.
 *                            Default 3000ms (3 seconds). Set to 0 to clear immediately.
 * @param errorClearDelayMs Duration in milliseconds to wait before clearing error results.
 *                          Default 5000ms (5 seconds). Longer than success to give users
 *                          time to read and respond to errors. Set to 0 to clear immediately.
 * @param clearOnlyRetryableErrors If true, only automatically clear errors marked as retryable.
 *                                 Critical/non-retryable errors remain until explicitly cleared.
 *                                 Default false (clear all error types). Useful when distinguishing
 *                                 between temporary failures and permanent issues.
 */
data class ResultClearingConfig(
    val successClearDelayMs: Long = 3000L,
    val errorClearDelayMs: Long = 5000L,
    val clearOnlyRetryableErrors: Boolean = false
)