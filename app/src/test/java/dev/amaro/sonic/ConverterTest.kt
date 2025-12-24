package dev.amaro.sonic

import dev.amaro.sonic.app.samples.converter.Converter
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConverterTest {

    @Test
    fun `Set source currency`() = runMainTest {
        val renderer : IRenderer<Converter.State> = mockk(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        try {
            val screen = Converter.Screen(renderer, scope = scope)
            screen.perform(Converter.Action.SetSource("MXN"))
            advanceUntilIdle()
            verify {
                renderer.render(Converter.State(source = "MXN"), any())
            }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `Set amount currency`() = runMainTest {
        val renderer : IRenderer<Converter.State> = mockk(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        try {
            val screen = Converter.Screen(renderer, scope = scope)
            screen.perform(Converter.Action.SetAmount("100,00"))
            advanceUntilIdle()
            verify {
                renderer.render(Converter.State(amount = BigDecimal(100).setScale(2)), any())
            }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `Set empty amount currency`() = runMainTest {
        val renderer : IRenderer<Converter.State> = mockk(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        try {
            val screen = Converter.Screen(renderer, scope = scope)
            screen.perform(Converter.Action.SetAmount(""))
            advanceUntilIdle()
            verify {
                renderer.render(Converter.State(amount = BigDecimal.ONE), any())
            }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `Set switch target and source currencies`() = runMainTest {
        val renderer : IRenderer<Converter.State> = mockk(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + Job())
        try {
            val screen = Converter.Screen(renderer, Converter.State(source = "MXN", target = "BRL"), scope)
            screen.perform(Converter.Action.SwitchCurrencies)
            advanceUntilIdle()
            verify {
                renderer.render(match { it.source == "BRL" && it.target == "MXN" }, any())
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
