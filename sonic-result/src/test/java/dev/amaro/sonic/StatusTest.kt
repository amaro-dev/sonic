package dev.amaro.sonic

import junit.framework.TestCase.*
import org.junit.Test

class StatusTest {

    @Test
    fun `Running variant has correct defaults`() {
        // Arrange & Act
        val running = Status.Running()

        // Assert
        assertEquals("", running.source)
        assertTrue(running.timestamp > 0)
        assertTrue(running.metadata.isEmpty())
    }

    @Test
    fun `Success variant has correct defaults`() {
        // Arrange & Act
        val success = Status.Success()

        // Assert
        assertEquals("", success.source)
        assertTrue(success.timestamp > 0)
        assertTrue(success.metadata.isEmpty())
    }

    @Test
    fun `Success variant preserves all fields`() {
        // Arrange
        val timestamp = 123456789L
        val metadata = mapOf("key" to "value", "count" to 42)

        // Act
        val success = Status.Success(
            source = "CACHE",
            timestamp = timestamp,
            metadata = metadata
        )

        // Assert
        assertEquals("CACHE", success.source)
        assertEquals(timestamp, success.timestamp)
        assertEquals(metadata, success.metadata)
        assertEquals("value", success.metadata["key"])
        assertEquals(42, success.metadata["count"])
    }

    @Test
    fun `Failure variant has correct defaults`() {
        // Arrange & Act
        val failure = Status.Failure(
            code = "ERROR",
            message = "Something went wrong"
        )

        // Assert
        assertEquals("ERROR", failure.code)
        assertEquals("Something went wrong", failure.message)
        assertEquals("", failure.source)
        assertTrue(failure.timestamp > 0)
        assertTrue(failure.metadata.isEmpty())
        assertFalse(failure.retryable)
        assertNull(failure.cause)
    }

    @Test
    fun `Failure variant preserves all fields`() {
        // Arrange
        val timestamp = 987654321L
        val exception = RuntimeException("Test exception")
        val metadata = mapOf("trace" to "abc123")

        // Act
        val failure = Status.Failure(
            code = "NETWORK_ERROR",
            message = "Connection timeout",
            source = "API",
            timestamp = timestamp,
            metadata = metadata,
            retryable = true,
            cause = exception
        )

        // Assert
        assertEquals("NETWORK_ERROR", failure.code)
        assertEquals("Connection timeout", failure.message)
        assertEquals("API", failure.source)
        assertEquals(timestamp, failure.timestamp)
        assertEquals(metadata, failure.metadata)
        assertTrue(failure.retryable)
        assertEquals(exception, failure.cause)
    }

    @Test
    fun `Failure with cause preserves exception details`() {
        // Arrange
        val exception = IllegalArgumentException("Invalid input")

        // Act
        val failure = Status.Failure(
            code = "VALIDATION_ERROR",
            message = "Invalid data",
            cause = exception
        )

        // Assert
        assertEquals(exception, failure.cause)
        assertEquals("Invalid input", failure.cause?.message)
    }

    @Test
    fun `Running data class contract - equals and hashCode`() {
        // Arrange
        val metadata = mapOf("key" to "value")
        val running1 = Status.Running(source = "API", timestamp = 100L, metadata = metadata)
        val running2 = Status.Running(source = "API", timestamp = 100L, metadata = metadata)
        val running3 = Status.Running(source = "CACHE", timestamp = 100L, metadata = metadata)

        // Assert
        assertEquals(running1, running2)
        assertEquals(running1.hashCode(), running2.hashCode())
        assertFalse(running1 == running3)
    }

    @Test
    fun `Success data class contract - equals and hashCode`() {
        // Arrange
        val metadata = mapOf("key" to "value")
        val success1 = Status.Success(source = "CACHE", timestamp = 100L, metadata = metadata)
        val success2 = Status.Success(source = "CACHE", timestamp = 100L, metadata = metadata)
        val success3 = Status.Success(source = "LOCAL", timestamp = 100L, metadata = metadata)

        // Assert
        assertEquals(success1, success2)
        assertEquals(success1.hashCode(), success2.hashCode())
        assertFalse(success1 == success3)
    }

    @Test
    fun `Failure data class contract - equals and hashCode`() {
        // Arrange
        val failure1 = Status.Failure(code = "ERROR", message = "Failed", timestamp = 100L)
        val failure2 = Status.Failure(code = "ERROR", message = "Failed", timestamp = 100L)
        val failure3 = Status.Failure(code = "OTHER", message = "Failed", timestamp = 100L)

        // Assert
        assertEquals(failure1, failure2)
        assertEquals(failure1.hashCode(), failure2.hashCode())
        assertFalse(failure1 == failure3)
    }

    @Test
    fun `Sealed class exhaustive when expression`() {
        // Arrange
        val runningResult: Status = Status.Running(source = "API")
        val successResult: Status = Status.Success(source = "API")
        val failureResult: Status = Status.Failure(code = "ERROR", message = "Failed")

        // Act
        val runningMessage = when (runningResult) {
            is Status.Running -> "Running from ${runningResult.source}"
            is Status.Success -> "Success from ${runningResult.source}"
            is Status.Failure -> "Failure: ${runningResult.message}"
        }

        val successMessage = when (successResult) {
            is Status.Running -> "Running from ${successResult.source}"
            is Status.Success -> "Success from ${successResult.source}"
            is Status.Failure -> "Failure: ${successResult.message}"
        }

        val failureMessage = when (failureResult) {
            is Status.Running -> "Running from ${failureResult.source}"
            is Status.Success -> "Success from ${failureResult.source}"
            is Status.Failure -> "Failure: ${failureResult.message}"
        }

        // Assert
        assertEquals("Running from API", runningMessage)
        assertEquals("Success from API", successMessage)
        assertEquals("Failure: Failed", failureMessage)
    }

    @Test
    fun `Success with empty metadata`() {
        // Arrange & Act
        val success = Status.Success(metadata = emptyMap())

        // Assert
        assertTrue(success.metadata.isEmpty())
    }

    @Test
    fun `Success with complex metadata types`() {
        // Arrange
        val metadata = mapOf(
            "string" to "value",
            "number" to 42,
            "boolean" to true,
            "list" to listOf(1, 2, 3),
            "map" to mapOf("nested" to "data")
        )

        // Act
        val success = Status.Success(metadata = metadata)

        // Assert
        assertEquals("value", success.metadata["string"])
        assertEquals(42, success.metadata["number"])
        assertEquals(true, success.metadata["boolean"])
        assertEquals(listOf(1, 2, 3), success.metadata["list"])
        assertEquals(mapOf("nested" to "data"), success.metadata["map"])
    }

    @Test
    fun `Failure with retryable flag true`() {
        // Arrange & Act
        val failure = Status.Failure(
            code = "TIMEOUT",
            message = "Request timeout",
            retryable = true
        )

        // Assert
        assertTrue(failure.retryable)
    }

    @Test
    fun `Failure with retryable flag false`() {
        // Arrange & Act
        val failure = Status.Failure(
            code = "AUTH_FAILED",
            message = "Invalid credentials",
            retryable = false
        )

        // Assert
        assertFalse(failure.retryable)
    }
}
