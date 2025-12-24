package dev.amaro.sonic

import dev.amaro.sonic.app.samples.calculator.Calculator
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CalculatorTest {

    @Test
    fun `Initial screen state`() = runMainTest {
        val renderer: IRenderer<Calculator.State> = mockk(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        try {
            Calculator.SimpleScreen(renderer, scope)
            advanceUntilIdle()
            verifyOrder {
                renderer.render(Calculator.State(), any())
            }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `Enter first number`() = runMainTest {
        val renderer: IRenderer<Calculator.State> = mockk(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        try {
            Calculator.SimpleScreen(renderer, scope).run {
                perform(Calculator.Action.FirstNumber(1))
            }
            advanceUntilIdle()
            verify {
                renderer.render(Calculator.State(firstNumber = 1f), any())
            }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `Enter second number`() = runMainTest {
        val renderer: IRenderer<Calculator.State> = mockk(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        try {
            Calculator.SimpleScreen(renderer, scope).run {
                perform(Calculator.Action.SecondNumber(1))
            }
            advanceUntilIdle()
            verify {
                renderer.render(Calculator.State(secondNumber = 1f), any())
            }
        } finally {
            scope.cancel()
        }
    }

    private fun runMainTest(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }
}
