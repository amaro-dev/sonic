package dev.amaro.sonic

import junit.framework.TestCase.*
import org.junit.Test

class ResultInfoTest {

    @Test
    fun `Success variant has correct defaults`() {
        // Arrange & Act
        val success = ResultInfo.Success()

        // Assert
        assertEquals("API", success.source)
        assertTrue(success.timestamp > 0)
        assertTrue(success.metadata.isEmpty())
    }

    @Test
    fun `Success variant preserves all fields`() {
        // Arrange
        val timestamp = 123456789L
        val metadata = mapOf("key" to "value", "count" to 42)

        // Act
        val success = ResultInfo.Success(
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
        val failure = ResultInfo.Failure(
            code = "ERROR",
            message = "Something went wrong"
        )

        // Assert
        assertEquals("ERROR", failure.code)
        assertEquals("Something went wrong", failure.message)
        assertTrue(failure.timestamp > 0)
        assertFalse(failure.retryable)
        assertNull(failure.cause)
    }

    @Test
    fun `Failure variant preserves all fields`() {
        // Arrange
        val timestamp = 987654321L
        val exception = RuntimeException("Test exception")

        // Act
        val failure = ResultInfo.Failure(
            code = "NETWORK_ERROR",
            message = "Connection timeout",
            timestamp = timestamp,
            retryable = true,
            cause = exception
        )

        // Assert
        assertEquals("NETWORK_ERROR", failure.code)
        assertEquals("Connection timeout", failure.message)
        assertEquals(timestamp, failure.timestamp)
        assertTrue(failure.retryable)
        assertEquals(exception, failure.cause)
    }

    @Test
    fun `Failure with cause preserves exception details`() {
        // Arrange
        val exception = IllegalArgumentException("Invalid input")

        // Act
        val failure = ResultInfo.Failure(
            code = "VALIDATION_ERROR",
            message = "Invalid data",
            cause = exception
        )

        // Assert
        assertEquals(exception, failure.cause)
        assertEquals("Invalid input", failure.cause?.message)
    }

    @Test
    fun `Success data class contract - equals and hashCode`() {
        // Arrange
        val metadata = mapOf("key" to "value")
        val success1 = ResultInfo.Success(source = "CACHE", timestamp = 100L, metadata = metadata)
        val success2 = ResultInfo.Success(source = "CACHE", timestamp = 100L, metadata = metadata)
        val success3 = ResultInfo.Success(source = "API", timestamp = 100L, metadata = metadata)

        // Assert
        assertEquals(success1, success2)
        assertEquals(success1.hashCode(), success2.hashCode())
        assertFalse(success1 == success3)
    }

    @Test
    fun `Failure data class contract - equals and hashCode`() {
        // Arrange
        val failure1 = ResultInfo.Failure(code = "ERROR", message = "Failed", timestamp = 100L)
        val failure2 = ResultInfo.Failure(code = "ERROR", message = "Failed", timestamp = 100L)
        val failure3 = ResultInfo.Failure(code = "OTHER", message = "Failed", timestamp = 100L)

        // Assert
        assertEquals(failure1, failure2)
        assertEquals(failure1.hashCode(), failure2.hashCode())
        assertFalse(failure1 == failure3)
    }

    @Test
    fun `Sealed class type safety - Success is ResultInfo`() {
        // Arrange & Act
        val result: ResultInfo = ResultInfo.Success()

        // Assert
        assertTrue(result is ResultInfo)
        assertTrue(result is ResultInfo.Success)
    }

    @Test
    fun `Sealed class type safety - Failure is ResultInfo`() {
        // Arrange & Act
        val result: ResultInfo = ResultInfo.Failure(code = "ERROR", message = "Failed")

        // Assert
        assertTrue(result is ResultInfo)
        assertTrue(result is ResultInfo.Failure)
    }

    @Test
    fun `Sealed class exhaustive when expression`() {
        // Arrange
        val successResult: ResultInfo = ResultInfo.Success(source = "API")
        val failureResult: ResultInfo = ResultInfo.Failure(code = "ERROR", message = "Failed")

        // Act
        val successMessage = when (successResult) {
            is ResultInfo.Success -> "Success from ${successResult.source}"
            is ResultInfo.Failure -> "Failure: ${successResult.message}"
        }

        val failureMessage = when (failureResult) {
            is ResultInfo.Success -> "Success from ${failureResult.source}"
            is ResultInfo.Failure -> "Failure: ${failureResult.message}"
        }

        // Assert
        assertEquals("Success from API", successMessage)
        assertEquals("Failure: Failed", failureMessage)
    }

    @Test
    fun `Success with empty metadata`() {
        // Arrange & Act
        val success = ResultInfo.Success(metadata = emptyMap())

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
        val success = ResultInfo.Success(metadata = metadata)

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
        val failure = ResultInfo.Failure(
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
        val failure = ResultInfo.Failure(
            code = "AUTH_FAILED",
            message = "Invalid credentials",
            retryable = false
        )

        // Assert
        assertFalse(failure.retryable)
    }
}
