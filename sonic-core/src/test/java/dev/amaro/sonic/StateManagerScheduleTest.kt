package dev.amaro.sonic

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StateManagerScheduleTest {

    @Test
    fun `Scheduled action executes after reduction completes`() = runTest {
        val reducer = object : IReducer<Int> {
            override fun reduce(action: IAction, state: Int): Int {
                return when (action) {
                    is TriggerAction -> 1
                    is ScheduledAction -> 2
                    else -> state
                }
            }
        }

        val middleware: IMiddleware<Int> = spyk(object : IMiddleware<Int> {
            override suspend fun process(action: IAction, state: Int, processor: IProcessor<Int>) {
                if (action is TriggerAction) {
                    processor.reduce(action)
                    processor.schedule(ScheduledAction)
                } else {
                    processor.reduce(action)
                }
            }
        })

        val stateManager = spyk(createStateManager(reducer, this, middleware))

        stateManager.perform(TriggerAction)
        advanceUntilIdle()

        // Verify both actions were processed
        coVerify {
            middleware.process(TriggerAction, 0, stateManager)
            middleware.process(ScheduledAction, 1, stateManager)
        }

        // Verify final state reflects both reductions
        assertEquals(2, stateManager.listen().value)
    }

    @Test
    fun `Scheduled action sees updated state`() = runTest {
        val reducer = object : IReducer<Int> {
            override fun reduce(action: IAction, state: Int): Int {
                return when (action) {
                    is TriggerAction -> 42
                    else -> state
                }
            }
        }

        var stateSeenByScheduled: Int? = null
        var triggerState: Int? = null

        val middleware: IMiddleware<Int> = object : IMiddleware<Int> {
            override suspend fun process(action: IAction, state: Int, processor: IProcessor<Int>) {
                when (action) {
                    is TriggerAction -> {
                        triggerState = state
                        processor.reduce(action)
                        processor.schedule(ScheduledAction)
                    }

                    is ScheduledAction -> {
                        stateSeenByScheduled = state
                        processor.reduce(action)
                    }

                    else -> processor.reduce(action)
                }
            }
        }

        val stateManager = createStateManager(reducer, this, middleware)

        stateManager.perform(TriggerAction)
        advanceUntilIdle()

        assertEquals(42, stateSeenByScheduled)
    }

    @Test
    fun `Multiple scheduled actions execute in FIFO order`() = runTest {
        val reducer: IReducer<String> = mockk(relaxed = true)
        every { reducer.reduce(any(), any()) } answers {
            val action = firstArg<IAction>()
            val currentState = secondArg<String>()
            when (action) {
                is ActionWithName -> currentState + action.name
                is TriggerAction -> currentState + "Trigger"
                else -> currentState
            }
        }

        val middleware: IMiddleware<String> = object : IMiddleware<String> {
            override suspend fun process(action: IAction, state: String, processor: IProcessor<String>) {
                if (action is TriggerAction) {
                    processor.reduce(action)
                    processor.schedule(ActionWithName("A"))
                    processor.schedule(ActionWithName("B"))
                    processor.schedule(ActionWithName("C"))
                } else {
                    processor.reduce(action)
                }
            }
        }

        val stateManager = createStateManager(reducer, this, middleware)

        stateManager.perform(TriggerAction)
        advanceUntilIdle()

        assertEquals("TriggerABC", stateManager.listen().value)
    }

    @Test
    fun `Scheduled actions go through middleware pipeline`() = runTest {
        val reducer: IReducer<String> = mockk(relaxed = true)
        every { reducer.reduce(any(), any()) } returns ""

        val interceptingMiddleware: IMiddleware<String> = mockk(relaxed = true)
        val schedulingMiddleware: IMiddleware<String> = object : IMiddleware<String> {
            override suspend fun process(action: IAction, state: String, processor: IProcessor<String>) {
                if (action is TriggerAction) {
                    processor.reduce(action)
                    processor.schedule(ScheduledAction)
                } else {
                    processor.reduce(action)
                }
            }
        }

        val stateManager = spyk(createStateManager(reducer, this, interceptingMiddleware, schedulingMiddleware))

        stateManager.perform(TriggerAction)
        advanceUntilIdle()

        coVerify {
            interceptingMiddleware.process(TriggerAction, "", stateManager)
            interceptingMiddleware.process(ScheduledAction, "", stateManager)
        }
    }

    @Test
    fun `Batch execution - multiple middleware scheduling during same reduction`() = runTest {
        val reducer: IReducer<String> = mockk(relaxed = true)
        every { reducer.reduce(any(), any()) } answers {
            val action = firstArg<IAction>()
            val currentState = secondArg<String>()
            when (action) {
                is ActionWithName -> currentState + action.name
                is TriggerAction -> currentState + "Trigger"
                else -> currentState
            }
        }

        val middleware1: IMiddleware<String> = object : IMiddleware<String> {
            override suspend fun process(action: IAction, state: String, processor: IProcessor<String>) {
                if (action is TriggerAction) {
                    processor.schedule(ActionWithName("M1"))
                }
                processor.reduce(action)
            }
        }

        val middleware2: IMiddleware<String> = object : IMiddleware<String> {
            override suspend fun process(action: IAction, state: String, processor: IProcessor<String>) {
                if (action is TriggerAction) {
                    processor.schedule(ActionWithName("M2"))
                }
                // Don't reduce here, let middleware1 do it
            }
        }

        val stateManager = createStateManager(reducer, this, middleware1, middleware2)

        stateManager.perform(TriggerAction)
        advanceUntilIdle()

        // Both middleware scheduled actions should execute
        val finalState = stateManager.listen().value
        assert(finalState.contains("M1"))
        assert(finalState.contains("M2"))
    }

    @Test
    fun `Scheduled actions and side effects both execute`() = runTest {
        val reducer = object : IReducer<String> {
            override fun reduce(action: IAction, state: String): String {
                return ""
            }
        }

        val executionOrder = mutableListOf<String>()

        val middleware: IMiddleware<String> = object : IMiddleware<String> {
            override suspend fun process(action: IAction, state: String, processor: IProcessor<String>) {
                when (action) {
                    is ActionWithSideEffect -> {
                        executionOrder.add("TriggerProcessed")
                        processor.reduce(action)
                        processor.schedule(ScheduledAction)
                    }

                    is ScheduledAction -> {
                        executionOrder.add("ScheduledExecuted")
                        processor.reduce(action)
                    }

                    is SideEffectAction -> {
                        executionOrder.add("SideEffectExecuted")
                        processor.reduce(action)
                    }

                    else -> processor.reduce(action)
                }
            }
        }

        val stateManager = createStateManager(reducer, this, middleware)

        stateManager.perform(ActionWithSideEffect)
        advanceUntilIdle()

        // Verify all three actions were processed (order may vary due to async execution)
        assert(executionOrder.contains("TriggerProcessed"))
        assert(executionOrder.contains("ScheduledExecuted"))
        assert(executionOrder.contains("SideEffectExecuted"))
        assertEquals(3, executionOrder.size)
    }

    @Test
    fun `Nested scheduled actions execute in next cycle`() = runTest {
        val reducer: IReducer<String> = mockk(relaxed = true)
        every { reducer.reduce(any(), any()) } answers {
            val action = firstArg<IAction>()
            val currentState = secondArg<String>()
            when (action) {
                is ActionWithName -> currentState + action.name
                else -> currentState
            }
        }

        val middleware: IMiddleware<String> = object : IMiddleware<String> {
            override suspend fun process(action: IAction, state: String, processor: IProcessor<String>) {
                when ((action as ActionWithName).name) {
                    "Trigger" -> {
                        processor.reduce(action)
                        processor.schedule(ActionWithName("A"))
                    }

                    "A" -> {
                        processor.reduce(action)
                        processor.schedule(ActionWithName("B"))
                    }

                    "B" -> {
                        processor.reduce(action)
                        processor.schedule(ActionWithName("C"))
                    }

                    else -> processor.reduce(action)
                }
            }
        }

        val stateManager = createStateManager(reducer, this, middleware)

        stateManager.perform(ActionWithName("Trigger"))
        advanceUntilIdle()

        // All nested scheduled actions should execute: Trigger -> A -> B -> C
        assertEquals("TriggerABC", stateManager.listen().value)
    }

    @Test
    fun `Empty queue - reduction with no scheduled actions`() = runTest {
        val reducer: IReducer<String> = mockk(relaxed = true)
        every { reducer.reduce(any(), any()) } returns "newState"

        val middleware: IMiddleware<String> = object : IMiddleware<String> {
            override suspend fun process(action: IAction, state: String, processor: IProcessor<String>) {
                processor.reduce(action)
                // No scheduling
            }
        }

        val stateManager = createStateManager(reducer, this, middleware)

        stateManager.perform(TriggerAction)
        advanceUntilIdle()

        assertEquals("newState", stateManager.listen().value)
    }

    @Test
    fun `Concurrent reductions with scheduling`() = runTest {
        val reducer: IReducer<Int> = mockk(relaxed = true)
        every { reducer.reduce(any(), any()) } answers { secondArg<Int>() + 1 }

        val middleware: IMiddleware<Int> = object : IMiddleware<Int> {
            override suspend fun process(action: IAction, state: Int, processor: IProcessor<Int>) {
                if (action is TriggerAction) {
                    processor.reduce(action)
                    processor.schedule(ScheduledAction)
                } else {
                    processor.reduce(action)
                }
            }
        }

        val stateManager = createStateManager(reducer, this, middleware)

        // Perform multiple actions concurrently
        stateManager.perform(TriggerAction)
        stateManager.perform(TriggerAction)
        stateManager.perform(TriggerAction)
        advanceUntilIdle()

        // Each TriggerAction should increment by 1, and each ScheduledAction should increment by 1
        // Total: 3 triggers + 3 scheduled = 6 increments
        assertEquals(6, stateManager.listen().value)
    }

    @Test
    fun `Scheduled actions execute in FIFO order`() = runTest {
        val executionOrder = mutableListOf<String>()

        val reducer = object : IReducer<String> {
            override fun reduce(action: IAction, state: String): String {
                return ""
            }
        }

        val middleware: IMiddleware<String> = object : IMiddleware<String> {
            override suspend fun process(action: IAction, state: String, processor: IProcessor<String>) {
                when (action) {
                    is TriggerAction -> {
                        executionOrder.add("Trigger")
                        processor.reduce(action)
                        processor.schedule(ActionWithName("First"))
                        processor.schedule(ActionWithName("Second"))
                    }

                    is ActionWithName -> {
                        executionOrder.add(action.name)
                        processor.reduce(action)
                    }

                    else -> processor.reduce(action)
                }
            }
        }

        val stateManager = createStateManager(reducer, this, middleware)

        stateManager.perform(TriggerAction)
        advanceUntilIdle()

        // Verify FIFO ordering: Trigger was processed, then First, then Second
        assertEquals(listOf("Trigger", "First", "Second"), executionOrder)
    }

    object TriggerAction : IAction
    object ScheduledAction : IAction
    object SideEffectAction : IAction

    object ActionWithSideEffect : IAction, ISideEffectAction {
        override val sideEffect: IAction = SideEffectAction
    }

    data class ActionWithName(val name: String) : IAction

    private fun createStateManager(
        reducer: IReducer<String>,
        scope: TestScope,
        vararg middleware: IMiddleware<String>
    ) =
        object : StateManager<String>("", scope, middleware.asList().toMutableList()) {
            override val reducer: IReducer<String> = reducer
        }

    private fun createStateManager(
        reducer: IReducer<Int>,
        scope: TestScope,
        vararg middleware: IMiddleware<Int>
    ) =
        object : StateManager<Int>(0, scope, middleware.asList().toMutableList()) {
            override val reducer: IReducer<Int> = reducer
        }
}
