package dev.amaro.sonic

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class StateManagerTest {

    @Test
    fun `Process delivers action to all middlewares`() {
        val middleware1: IMiddleware<String> = mockk(relaxed = true)
        val middleware2: IMiddleware<String> = mockk(relaxed = true)
        val processor: IProcessor<String> = mockk(relaxed = true)

        val stateManager = createStateManager(processor, middleware1, middleware2)
        stateManager.process(Action)

        verify {
            middleware1.process(eq(Action), eq(""), eq(processor))
            middleware2.process(eq(Action), eq(""), eq(processor))
        }
    }

    @Test
    fun `Process delivers action to added middlewares`() {
        val middleware1: IMiddleware<String> = mockk(relaxed = true)
        val middleware2: IMiddleware<String> = mockk(relaxed = true)
        val processor: IProcessor<String> = mockk(relaxed = true)
        val stateManager = createStateManager(processor, middleware1)
        stateManager.addMiddleware(middleware2)

        stateManager.process(Action)

        verify {
            middleware1.process(eq(Action), eq(""), eq(processor))
            middleware2.process(eq(Action), eq(""), eq(processor))
        }
    }

    object Action: IAction

    private fun createStateManager(processor: IProcessor<String> , vararg middleware: IMiddleware<String>) =
        object : StateManager<String>("", middleware.asList()) {
            override val processor: IProcessor<String> = processor
        }
}