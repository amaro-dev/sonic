package dev.amaro.sonic

import dev.amaro.sonic.samples.Calculator
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.mockk.verifySequence
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test

class CalculatorTest {

    private val dispatcher = TestCoroutineDispatcher()

    @Test
    fun `Initial screen state`() = dispatcher.runBlockingTest {
        val renderer: IRenderer<Calculator.State> = mockk(relaxed = true)
        Calculator.SimpleScreen(renderer, dispatcher)
        verifyOrder {
            renderer.render(Calculator.State(), any())
        }
    }

    @Test
    fun `Enter first number`() = dispatcher.runBlockingTest {
        val renderer: IRenderer<Calculator.State> = mockk(relaxed = true)
        Calculator.SimpleScreen(renderer, dispatcher).run {
            perform(Calculator.Action.FirstNumber(1))
        }
        verify {
            renderer.render(Calculator.State(firstNumber = 1), any())
        }
    }

    @Test
    fun `Enter second number`() = dispatcher.runBlockingTest {
        val renderer: IRenderer<Calculator.State> = mockk(relaxed = true)
        Calculator.SimpleScreen(renderer, dispatcher).run {
            perform(Calculator.Action.SecondNumber(1))
        }
        verifySequence {
            renderer.render(Calculator.State(secondNumber = 1), any())
        }
    }
}
