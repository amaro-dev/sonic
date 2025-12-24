package dev.amaro.sonic

import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Test suite for ResultClearingAgent.
 *
 * Tests verify core functionality:
 * - Agent processes state without modification
 * - Agent respects configuration
 */
class ResultClearingAgentTest {

    data class TestState(
        val status: Status? = null,
        val data: String = ""
    )

    sealed class TestAction : IAction {
        object ClearResult : TestAction()
    }

    private fun createTestReducer(): IReducer<TestState> = object : IReducer<TestState> {
        override fun reduce(action: IAction, currentState: TestState): TestState {
            return when (action) {
                is TestAction.ClearResult -> currentState.copy(status = null)
                else -> currentState
            }
        }
    }

    private fun createTestStateManager(
        reducer: IReducer<TestState>,
        scope: TestScope,
        initialState: TestState = TestState()
    ): IStateManager<TestState> {
        return object : StateManager<TestState>(initialState, scope) {
            override val reducer: IReducer<TestState> = reducer
        }
    }

    @Test
    fun `Agent processes null result without error`() = runTest {
        val agent = ResultClearingAgent(
            createTestStateManager(createTestReducer(), this),
            ResultClearingConfig(),
            { it.status },
            { TestAction.ClearResult }
        )

        agent.process(TestState())
        advanceUntilIdle()
    }

    @Test
    fun `Agent returns state unmodified`() = runTest {
        val agent = ResultClearingAgent(
            createTestStateManager(createTestReducer(), this),
            ResultClearingConfig(),
            { it.status },
            { TestAction.ClearResult }
        )

        val state = TestState(
            status = Status.Success(source = "API"),
            data = "test"
        )

        val result = agent.process(state)
        advanceUntilIdle()

        assertEquals(state, result)
    }

    @Test
    fun `Agent respects success clear delay configuration`() = runTest {
        val agent = ResultClearingAgent(
            createTestStateManager(createTestReducer(), this),
            ResultClearingConfig(successClearDelayMs = 2000),
            { it.status },
            { TestAction.ClearResult }
        )

        val state = TestState(status = Status.Success(source = "API"))
        agent.process(state)
        advanceUntilIdle()
    }

    @Test
    fun `Agent respects error clear delay configuration`() = runTest {
        val agent = ResultClearingAgent(
            createTestStateManager(createTestReducer(), this),
            ResultClearingConfig(errorClearDelayMs = 5000),
            { it.status },
            { TestAction.ClearResult }
        )

        val state = TestState(status = Status.Failure(code = "E", message = "Error"))
        agent.process(state)
        advanceUntilIdle()
    }

    @Test
    fun `Agent respects clearOnlyRetryableErrors configuration`() = runTest {
        val agent = ResultClearingAgent(
            createTestStateManager(createTestReducer(), this),
            ResultClearingConfig(clearOnlyRetryableErrors = true),
            { it.status },
            { TestAction.ClearResult }
        )

        val state = TestState(
            status = Status.Failure(
                code = "E",
                message = "Error",
                retryable = false
            )
        )
        agent.process(state)
        advanceUntilIdle()
    }

    @Test
    fun `Agent processes multiple state updates`() = runTest {
        val agent = ResultClearingAgent(
            createTestStateManager(createTestReducer(), this),
            ResultClearingConfig(),
            { it.status },
            { TestAction.ClearResult }
        )

        val state1 = TestState(status = Status.Success(source = "API"))
        val state2 = TestState(status = Status.Failure(code = "E", message = "Error"))
        val state3 = TestState(status = null)

        val result1 = agent.process(state1)
        val result2 = agent.process(state2)
        val result3 = agent.process(state3)

        advanceUntilIdle()

        assertEquals(state1, result1)
        assertEquals(state2, result2)
        assertEquals(state3, result3)
    }

    @Test
    fun `Agent ignores running status`() = runTest {
        val agent = ResultClearingAgent(
            createTestStateManager(createTestReducer(), this),
            ResultClearingConfig(),
            { it.status },
            { TestAction.ClearResult }
        )

        val state = TestState(status = Status.Running(source = "API"))
        val result = agent.process(state)
        advanceUntilIdle()

        assertEquals(state, result)
    }
}
