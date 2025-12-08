package dev.amaro.sonic

import junit.framework.TestCase.*
import org.junit.Test

class ResultBuilderTest {

    @Test
    fun `success() function creates correct ResultInfo Success`() {
        // Arrange & Act
        val result = success().buildSuccess()

        // Assert
        assertTrue(result is ResultInfo.Success)
        assertEquals("API", result.source)
        assertTrue(result.timestamp > 0)
        assertTrue(result.metadata.isEmpty())
    }

    @Test
    fun `success() with custom source`() {
        // Arrange & Act
        val result = success("CACHE").buildSuccess()

        // Assert
        assertEquals("CACHE", result.source)
        assertTrue(result.timestamp > 0)
        assertTrue(result.metadata.isEmpty())
    }

    @Test
    fun `success() with LOCAL source`() {
        // Arrange & Act
        val result = success("LOCAL").buildSuccess()

        // Assert
        assertEquals("LOCAL", result.source)
    }

    @Test
    fun `failure() function with code and message`() {
        // Arrange & Act
        val result = failure("NETWORK_ERROR", "Connection failed").buildFailure()

        // Assert
        assertTrue(result is ResultInfo.Failure)
        assertEquals("NETWORK_ERROR", result.code)
        assertEquals("Connection failed", result.message)
        assertTrue(result.timestamp > 0)
        assertFalse(result.retryable)
        assertNull(result.cause)
    }

    @Test
    fun `failure() function from exception`() {
        // Arrange
        val exception = RuntimeException("Something went wrong")

        // Act
        val result = failure(exception).buildFailure()

        // Assert
        assertTrue(result is ResultInfo.Failure)
        assertEquals("RuntimeException", result.code)
        assertEquals("Something went wrong", result.message)
        assertEquals(exception, result.cause)
    }

    @Test
    fun `failure() extracts exception class name as code`() {
        // Arrange
        val ioException = java.io.IOException("File not found")
        val illegalArgException = IllegalArgumentException("Invalid input")

        // Act
        val ioResult = failure(ioException).buildFailure()
        val illegalArgResult = failure(illegalArgException).buildFailure()

        // Assert
        assertEquals("IOException", ioResult.code)
        assertEquals("File not found", ioResult.message)
        assertEquals("IllegalArgumentException", illegalArgResult.code)
        assertEquals("Invalid input", illegalArgResult.message)
    }

    @Test
    fun `failure() handles exception with null message`() {
        // Arrange
        val exception = RuntimeException()

        // Act
        val result = failure(exception).buildFailure()

        // Assert
        assertEquals("Unknown error", result.message)
    }

    @Test
    fun `ResultBuilder source() method is chainable`() {
        // Arrange
        val builder = ResultBuilder()

        // Act
        val result = builder.source("CACHE").buildSuccess()

        // Assert
        assertEquals("CACHE", result.source)
    }

    @Test
    fun `ResultBuilder metadata() method adds to map`() {
        // Arrange
        val builder = ResultBuilder()

        // Act
        val result = builder
            .metadata("key1", "value1")
            .metadata("key2", 42)
            .buildSuccess()

        // Assert
        assertEquals("value1", result.metadata["key1"])
        assertEquals(42, result.metadata["key2"])
        assertEquals(2, result.metadata.size)
    }

    @Test
    fun `ResultBuilder retryable() flag is set correctly`() {
        // Arrange
        val builder = ResultBuilder()

        // Act
        val result = builder
            .retryable(true)
            .buildFailure()

        // Assert
        assertTrue(result.retryable)
    }

    @Test
    fun `ResultBuilder retryable() false is set correctly`() {
        // Arrange
        val builder = ResultBuilder()

        // Act
        val result = builder
            .retryable(false)
            .buildFailure()

        // Assert
        assertFalse(result.retryable)
    }

    @Test
    fun `Chainable methods work together - success case`() {
        // Arrange & Act
        val result = success("CACHE")
            .metadata("cacheAge", 3600)
            .metadata("version", "1.0")
            .buildSuccess()

        // Assert
        assertEquals("CACHE", result.source)
        assertEquals(3600, result.metadata["cacheAge"])
        assertEquals("1.0", result.metadata["version"])
    }

    @Test
    fun `Chainable methods work together - failure case`() {
        // Arrange
        val exception = RuntimeException("Test")

        // Act
        val result = failure("NETWORK_ERROR", "Timeout")
            .retryable(true)
            .buildFailure()

        // Assert
        assertEquals("NETWORK_ERROR", result.code)
        assertEquals("Timeout", result.message)
        assertTrue(result.retryable)
    }

    @Test
    fun `ResultBuilder can be reused for multiple results`() {
        // Arrange
        val builder = ResultBuilder()

        // Act
        builder.source("API")
        val result1 = builder.buildSuccess()

        builder.source("CACHE")
        val result2 = builder.buildSuccess()

        // Assert
        assertEquals("API", result1.source)
        assertEquals("CACHE", result2.source)
    }

    @Test
    fun `buildSuccess creates new instance each time`() {
        // Arrange
        val builder = success("API")

        // Act
        val result1 = builder.buildSuccess()
        Thread.sleep(1) // Ensure different timestamps
        val result2 = builder.buildSuccess()

        // Assert
        assertTrue(result1.timestamp <= result2.timestamp)
    }

    @Test
    fun `buildFailure creates new instance each time`() {
        // Arrange
        val builder = failure("ERROR", "Failed")

        // Act
        val result1 = builder.buildFailure()
        Thread.sleep(1) // Ensure different timestamps
        val result2 = builder.buildFailure()

        // Assert
        assertTrue(result1.timestamp <= result2.timestamp)
    }

    @Test
    fun `Fluent API with all success options`() {
        // Arrange & Act
        val result = ResultBuilder()
            .source("COMPUTED")
            .metadata("duration", 150)
            .metadata("algorithm", "quicksort")
            .metadata("iterations", 5)
            .buildSuccess()

        // Assert
        assertEquals("COMPUTED", result.source)
        assertEquals(150, result.metadata["duration"])
        assertEquals("quicksort", result.metadata["algorithm"])
        assertEquals(5, result.metadata["iterations"])
    }

    @Test
    fun `Fluent API with all failure options`() {
        // Arrange
        val exception = IllegalStateException("Invalid state")

        // Act
        val builder = ResultBuilder()
        builder.code = "STATE_ERROR"
        builder.message = "Invalid state detected"
        builder.cause = exception
        val result = builder.retryable(false).buildFailure()

        // Assert
        assertEquals("STATE_ERROR", result.code)
        assertEquals("Invalid state detected", result.message)
        assertFalse(result.retryable)
        assertEquals(exception, result.cause)
    }

    @Test
    fun `Multiple metadata calls accumulate values`() {
        // Arrange
        val builder = success("API")

        // Act
        val result = builder
            .metadata("a", 1)
            .metadata("b", 2)
            .metadata("c", 3)
            .metadata("d", 4)
            .buildSuccess()

        // Assert
        assertEquals(4, result.metadata.size)
        assertEquals(1, result.metadata["a"])
        assertEquals(2, result.metadata["b"])
        assertEquals(3, result.metadata["c"])
        assertEquals(4, result.metadata["d"])
    }

    @Test
    fun `Metadata supports different value types`() {
        // Arrange & Act
        val result = ResultBuilder()
            .metadata("string", "text")
            .metadata("int", 42)
            .metadata("boolean", true)
            .metadata("list", listOf(1, 2, 3))
            .buildSuccess()

        // Assert
        assertEquals("text", result.metadata["string"])
        assertEquals(42, result.metadata["int"])
        assertEquals(true, result.metadata["boolean"])
        assertEquals(listOf(1, 2, 3), result.metadata["list"])
    }
}
