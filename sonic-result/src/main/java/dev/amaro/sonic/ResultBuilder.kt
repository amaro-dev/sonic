package dev.amaro.sonic

/**
 * Fluent builder for creating [ResultInfo] instances with type-safe construction.
 *
 * Provides convenient DSL-style creation for both success and failure results.
 *
 * Usage:
 * ```
 * // Success with source
 * success("CACHE").buildSuccess()
 *
 * // Failure with retryable flag
 * failure("NETWORK_ERROR", "Timeout").retryable(true).buildFailure()
 *
 * // Failure from exception
 * failure(exception).buildFailure()
 * ```
 */
class ResultBuilder {
    var source: String = "API"
    var metadata: MutableMap<String, Any> = mutableMapOf()
    var code: String = "UNKNOWN"
    var message: String = ""
    var retryable: Boolean = false
    var cause: Throwable? = null

    /**
     * Set the result source (API, CACHE, LOCAL, COMPUTED, etc.)
     */
    fun source(source: String): ResultBuilder {
        this.source = source
        return this
    }

    /**
     * Add metadata key-value pair
     */
    fun metadata(key: String, value: Any): ResultBuilder {
        metadata[key] = value
        return this
    }

    /**
     * Mark this failure as retryable
     */
    fun retryable(retryable: Boolean): ResultBuilder {
        this.retryable = retryable
        return this
    }

    /**
     * Build a [ResultInfo.Success] from current state
     */
    fun buildSuccess(): ResultInfo.Success = ResultInfo.Success(
        source = source,
        timestamp = System.currentTimeMillis(),
        metadata = metadata
    )

    /**
     * Build a [ResultInfo.Failure] from current state
     */
    fun buildFailure(): ResultInfo.Failure = ResultInfo.Failure(
        code = code,
        message = message,
        retryable = retryable,
        cause = cause
    )
}

/**
 * Convenience function to create a success result builder
 */
fun success(source: String = "API"): ResultBuilder = ResultBuilder().apply {
    this.source = source
}

/**
 * Convenience function to create a failure result builder from code and message
 */
fun failure(code: String, message: String): ResultBuilder = ResultBuilder().apply {
    this.code = code
    this.message = message
}

/**
 * Convenience function to create a failure result builder from exception
 */
fun failure(e: Exception): ResultBuilder = ResultBuilder().apply {
    this.code = e::class.simpleName ?: "EXCEPTION"
    this.message = e.message ?: "Unknown error"
    this.cause = e
}
