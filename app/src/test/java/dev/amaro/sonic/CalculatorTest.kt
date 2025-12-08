package dev.amaro.sonic

import dev.amaro.sonic.app.samples.calculator.Calculator
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalculatorTest {

    @Test
    fun `Initial screen state`() = runTest {
        val renderer: IRenderer<Calculator.State> = mockk(relaxed = true)
        Calculator.SimpleScreen(renderer, this)
        advanceUntilIdle()
        verifyOrder {
            renderer.render(Calculator.State(), any())
        }
    }

    @Test
    fun `Enter first number`() = runTest {
        val renderer: IRenderer<Calculator.State> = mockk(relaxed = true)
        Calculator.SimpleScreen(renderer, this).run {
            perform(Calculator.Action.FirstNumber(1))
        }
        advanceUntilIdle()
        verify {
            renderer.render(Calculator.State(firstNumber = 1), any())
        }
    }

    @Test
    fun `Enter second number`() = runTest {
        val renderer: IRenderer<Calculator.State> = mockk(relaxed = true)
        Calculator.SimpleScreen(renderer, this).run {
            perform(Calculator.Action.SecondNumber(1))
        }
        advanceUntilIdle()
        verify {
            renderer.render(Calculator.State(secondNumber = 1), any())
        }
    }
}
