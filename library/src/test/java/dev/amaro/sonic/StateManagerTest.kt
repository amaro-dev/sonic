package dev.amaro.sonic

import io.mockk.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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

    @Test
    fun `Reduce produces new state`() {
        val reducer: IReducer<String> = mockk(relaxed = true)
        every { reducer.reduce(any(), any()) } returns "new state"
        val stateManager = createStateManager(reducer)

        stateManager.reduce(Action)

        assertEquals("new state", stateManager.listen().value)
    }

    @Test
    fun `Reducing an ISideEffect action makes it process the side-effect`() {
        val reducer: IReducer<String> = mockk(relaxed = true)
        val middleware1: IMiddleware<String> = mockk(relaxed = true)
        every { reducer.reduce(any(), any()) } returns "new state"
        val stateManager = spyk(createStateManager(reducer, middleware1))

        stateManager.reduce(ActionWithSideEffect)

        verify {
            stateManager.perform(SideEffectAction)
            middleware1.process(eq(SideEffectAction), eq("new state"), eq(stateManager))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Using async middleware`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val middleware1: AsyncMiddlewareBase<String> = spyk(
            object : AsyncMiddlewareBase<String>(CoroutineScope(dispatcher)) {
                override suspend fun asyncProcess(action: IAction, state: String, processor: IProcessor<String>) = Unit
            })
        val reducer: IReducer<String> = mockk(relaxed = true)
        val stateManager = spyk(createStateManager(reducer, middleware1))

        stateManager.perform(Action)
        advanceUntilIdle()
        coVerify { middleware1.asyncProcess(any(), any(), any()) }
    }


    object Action : IAction
    object SideEffectAction : IAction
    object ActionWithSideEffect : IAction, ISideEffectAction {
        override val sideEffect: IAction = SideEffectAction
    }

    private fun createStateManager(
        reducer: IReducer<String>,
        vararg middleware: IMiddleware<String>
    ) =
        object : StateManager<String>("", middleware.asList()) {
            override val reducer: IReducer<String> = reducer
        }
}