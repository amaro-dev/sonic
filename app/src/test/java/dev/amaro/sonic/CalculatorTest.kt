package dev.amaro.sonic

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
        val renderer: IRenderer<Example2.State> = mockk(relaxed = true)
        Example2.SimpleScreen(renderer, dispatcher)
        verifyOrder {
            renderer.render(Example2.State(), any())
        }
    }

    @Test
    fun `Enter first number`() = dispatcher.runBlockingTest {
        val renderer: IRenderer<Example2.State> = mockk(relaxed = true)
        Example2.SimpleScreen(renderer, dispatcher).run {
            perform(Example2.Action.FirstNumber(1))
        }
        verify {
            renderer.render(Example2.State(firstNumber = 1), any())
        }
    }

    @Test
    fun `Enter second number`() = dispatcher.runBlockingTest {
        val renderer: IRenderer<Example2.State> = mockk(relaxed = true)
        Example2.SimpleScreen(renderer, dispatcher).run {
            perform(Example2.Action.SecondNumber(1))
        }
        verifySequence {
            renderer.render(Example2.State(secondNumber = 1), any())
        }
    }
}
