package dev.amaro.sonic

import dev.amaro.sonic.app.samples.converter.Converter
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import java.math.BigDecimal

class ConverterTest {
    private val dispatcher = TestCoroutineDispatcher()

    @Test
    fun `Set source currency`() = runBlockingTest{
        val renderer : IRenderer<Converter.State> = mockk(relaxed = true)
        val screen = Converter.Screen(renderer, dispatcher = dispatcher)
        screen.perform(Converter.Action.SetSource("MXN"))
        verify {
            renderer.render(Converter.State( source = "MXN"), any())
        }
    }

    @Test
    fun `Set amount currency`() = runBlockingTest{
        val renderer : IRenderer<Converter.State> = mockk(relaxed = true)
        val screen = Converter.Screen(renderer, dispatcher = dispatcher)
        screen.perform(Converter.Action.SetAmount("100,00"))
        verify {
            renderer.render(Converter.State( amount = BigDecimal(100).setScale(2)), any())
        }
    }

    @Test
    fun `Set empty amount currency`() = runBlockingTest{
        val renderer : IRenderer<Converter.State> = mockk(relaxed = true)
        val screen = Converter.Screen(renderer, dispatcher = dispatcher)
        screen.perform(Converter.Action.SetAmount(""))
        verify {
            renderer.render(Converter.State( amount = BigDecimal.ONE), any())
        }
    }

    @Test
    fun `Set switch target and source currencies`() = runBlockingTest{
        val renderer : IRenderer<Converter.State> = mockk(relaxed = true)
        val screen = Converter.Screen(renderer, Converter.State(source = "MXN", target = "BRL"), dispatcher)
        screen.perform(Converter.Action.SwitchCurrencies)
        verify {
            renderer.render(Converter.State( source = "BRL", target = "MXN"), any())
        }
    }
}