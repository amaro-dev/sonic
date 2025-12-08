package dev.amaro.sonic

/**
 * Unified result representation for both successful and failed operations.
 *
 * ResultInfo provides a rich model for tracking operation outcomes across concurrent
 * operations. Use per-category in state to prevent overwrites:
 *
 * ```
 * data class MyState(
 *     val results: Map<String, ResultInfo> = emptyMap()  // "network", "auth", etc.
 * )
 * ```
 *
 * Success example:
 * ```
 * ResultInfo.Success(source = "CACHE", timestamp = System.currentTimeMillis())
 * ```
 *
 * Failure example:
 * ```
 * ResultInfo.Failure(
 *     code = "NETWORK_ERROR",
 *     message = "Connection timeout",
 *     retryable = true,
 *     cause = exception
 * )
 * ```
 */
sealed class ResultInfo {
    /**
     * Represents a successful operation result.
     *
     * @param source Where the result came from: "API", "CACHE", "LOCAL", "COMPUTED", etc.
     * @param timestamp When the result was produced (milliseconds since epoch)
     * @param metadata Additional context (cache age, data version, etc.)
     */
    data class Success(
        val source: String = "API",
        val timestamp: Long = System.currentTimeMillis(),
        val metadata: Map<String, Any> = emptyMap()
    ) : ResultInfo()

    /**
     * Represents a failed operation result.
     *
     * @param code Error code for categorization ("NETWORK_ERROR", "AUTH_FAILED", etc.)
     * @param message Human-readable error message
     * @param timestamp When the error occurred (milliseconds since epoch)
     * @param retryable Whether this error might succeed on retry
     * @param cause Original exception, if any
     */
    data class Failure(
        val code: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis(),
        val retryable: Boolean = false,
        val cause: Throwable? = null
    ) : ResultInfo()
}
