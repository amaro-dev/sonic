package dev.amaro.sonic

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class StateManagerTest {

    @Test
    fun `Process delivers action to all middlewares`() {
        val middleware1: IMiddleware<String> = mockk(relaxed = true)
        val middleware2: IMiddleware<String> = mockk(relaxed = true)
        val processor: IReducer<String> = mockk(relaxed = true)

        val stateManager = createStateManager(processor, middleware1, middleware2)
        stateManager.perform(Action)

        verify {
            middleware1.process(eq(Action), eq(""), eq(stateManager))
            middleware2.process(eq(Action), eq(""), eq(stateManager))
        }
    }

    @Test
    fun `Process delivers action to added middlewares`() {
        val middleware1: IMiddleware<String> = mockk(relaxed = true)
        val middleware2: IMiddleware<String> = mockk(relaxed = true)
        val processor: IReducer<String> = mockk(relaxed = true)
        val stateManager = createStateManager(processor, middleware1)
        stateManager.addMiddleware(middleware2)

        stateManager.perform(Action)

        verify {
            middleware1.process(eq(Action), eq(""), eq(stateManager))
            middleware2.process(eq(Action), eq(""), eq(stateManager))
        }
    }

    object Action : IAction

    private fun createStateManager(
        reducer: IReducer<String>,
        vararg middleware: IMiddleware<String>
    ) =
        object : StateManager<String>("", middleware.asList()) {
            override val reducer: IReducer<String> = reducer
        }
}