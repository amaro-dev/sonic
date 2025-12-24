package dev.amaro.sonic

/**
 * Unified status representation for running, successful, and failed operations.
 *
 * Status provides a rich model for tracking operation outcomes across concurrent
 * operations. Use per-category in state to prevent overwrites:
 *
 * ```
 * data class MyState(
 *     val results: Map<String, Status> = emptyMap()  // "network", "auth", etc.
 * )
 * ```
 *
 * Running example:
 * ```
 * Status.Running(source = "API", timestamp = System.currentTimeMillis())
 * ```
 *
 * Success example:
 * ```
 * Status.Success(source = "CACHE", timestamp = System.currentTimeMillis())
 * ```
 *
 * Failure example:
 * ```
 * Status.Failure(
 *     code = "NETWORK_ERROR",
 *     message = "Connection timeout",
 *     retryable = true,
 *     cause = exception
 * )
 * ```
 */
sealed class Status(
    open val source: String,
    open val timestamp: Long,
    open val metadata: Map<String, Any>
) {
    protected abstract fun copyBase(
        source: String = this.source,
        timestamp: Long = this.timestamp,
        metadata: Map<String, Any> = this.metadata
    ): Status

    fun withMetadata(metadata: Map<String, Any>): Status = copyBase(metadata = metadata)

    fun addMetadata(key: String, value: Any): Status =
        copyBase(metadata = metadata + (key to value))

    fun withSource(source: String): Status = copyBase(source = source)

    fun withTimestamp(timestamp: Long): Status = copyBase(timestamp = timestamp)

    /**
     * Represents an ongoing operation.
     *
     * @param source Where the status originated: "API", "CACHE", "LOCAL", "COMPUTED", etc.
     * @param timestamp When the status was produced (milliseconds since epoch)
     * @param metadata Additional context (cache age, data version, etc.)
     */
    data class Running(
        override val source: String = "",
        override val timestamp: Long = System.currentTimeMillis(),
        override val metadata: Map<String, Any> = emptyMap()
    ) : Status(source, timestamp, metadata) {
        override fun copyBase(
            source: String,
            timestamp: Long,
            metadata: Map<String, Any>
        ): Status = copy(source = source, timestamp = timestamp, metadata = metadata)
    }

    /**
     * Represents a successful operation result.
     *
     * @param source Where the result came from: "API", "CACHE", "LOCAL", "COMPUTED", etc.
     * @param timestamp When the result was produced (milliseconds since epoch)
     * @param metadata Additional context (cache age, data version, etc.)
     */
    data class Success(
        override val source: String = "",
        override val timestamp: Long = System.currentTimeMillis(),
        override val metadata: Map<String, Any> = emptyMap()
    ) : Status(source, timestamp, metadata) {
        override fun copyBase(
            source: String,
            timestamp: Long,
            metadata: Map<String, Any>
        ): Status = copy(source = source, timestamp = timestamp, metadata = metadata)
    }

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
        override val source: String = "",
        override val timestamp: Long = System.currentTimeMillis(),
        override val metadata: Map<String, Any> = emptyMap(),
        val retryable: Boolean = false,
        val cause: Throwable? = null
    ) : Status(source, timestamp, metadata) {
        override fun copyBase(
            source: String,
            timestamp: Long,
            metadata: Map<String, Any>
        ): Status = copy(source = source, timestamp = timestamp, metadata = metadata)

        fun withCode(code: String): Failure = copy(code = code)

        fun withMessage(message: String): Failure = copy(message = message)

        fun causeBy(cause: Throwable?): Failure = copy(cause = cause)

        fun retryable(retryable: Boolean): Failure = copy(retryable = retryable)
    }
}
