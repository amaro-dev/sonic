package dev.amaro.sonic

import junit.framework.TestCase.assertEquals
import org.junit.Test

class BindingTest {

    @Test
    fun `StateBinding creates with correct values`() {
        // Arrange
        val testAction = object : IAction {}
        var actionPerformed = false
        val performer: (IAction) -> Unit = { actionPerformed = true }

        // Act
        val binding = StateBinding("test", performer)

        // Assert
        assertEquals("test", binding.value)
        binding.perform(testAction)
        assertEquals(true, actionPerformed)
    }

    @Test
    fun `StateBinding value property is accessible`() {
        // Arrange & Act
        val binding = StateBinding(42, {})

        // Assert
        assertEquals(42, binding.value)
    }

    @Test
    fun `StateBinding perform executes callback`() {
        // Arrange
        var callCount = 0
        val binding = StateBinding("state") { callCount++ }

        // Act
        binding.perform(object : IAction {})
        binding.perform(object : IAction {})
        binding.perform(object : IAction {})

        // Assert
        assertEquals(3, callCount)
    }
}
