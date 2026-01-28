package dev.amaro.sonic

import org.junit.Assert.assertEquals
import org.junit.Test

class CompositeReducerTest {

    // Test state and actions
    data class TestState(
        val counter: Int = 0,
        val text: String = "",
        val flag: Boolean = false
    )

    sealed class TestAction : IAction {
        object Increment : TestAction()
        object Decrement : TestAction()
        data class SetText(val text: String) : TestAction()
        object ToggleFlag : TestAction()
    }

    // Test reducers
    class CounterReducer : IReducer<TestState> {
        override fun reduce(action: IAction, currentState: TestState): TestState {
            return when (action) {
                is TestAction.Increment -> currentState.copy(counter = currentState.counter + 1)
                is TestAction.Decrement -> currentState.copy(counter = currentState.counter - 1)
                else -> currentState
            }
        }
    }

    class TextReducer : IReducer<TestState> {
        override fun reduce(action: IAction, currentState: TestState): TestState {
            return when (action) {
                is TestAction.SetText -> currentState.copy(text = action.text)
                else -> currentState
            }
        }
    }

    class FlagReducer : IReducer<TestState> {
        override fun reduce(action: IAction, currentState: TestState): TestState {
            return when (action) {
                is TestAction.ToggleFlag -> currentState.copy(flag = !currentState.flag)
                else -> currentState
            }
        }
    }

    @Test
    fun `constructor with varargs creates composite reducer`() {
        val reducer = CompositeReducer(
            CounterReducer(),
            TextReducer(),
            FlagReducer()
        )

        val initialState = TestState()
        val result = reducer.reduce(TestAction.Increment, initialState)

        assertEquals(1, result.counter)
    }

    @Test
    fun `constructor with list creates composite reducer`() {
        val reducers = listOf(
            CounterReducer(),
            TextReducer(),
            FlagReducer()
        )
        val reducer = CompositeReducer(reducers)

        val initialState = TestState()
        val result = reducer.reduce(TestAction.Increment, initialState)

        assertEquals(1, result.counter)
    }

    @Test
    fun `reduces action through all reducers sequentially`() {
        val reducer = CompositeReducer(
            CounterReducer(),
            TextReducer(),
            FlagReducer()
        )

        val initialState = TestState()
        val result1 = reducer.reduce(TestAction.Increment, initialState)
        val result2 = reducer.reduce(TestAction.SetText("hello"), result1)
        val result3 = reducer.reduce(TestAction.ToggleFlag, result2)

        assertEquals(1, result3.counter)
        assertEquals("hello", result3.text)
        assertEquals(true, result3.flag)
    }

    @Test
    fun `each reducer receives output of previous reducer`() {
        // Reducer that doubles the counter
        class DoublerReducer : IReducer<TestState> {
            override fun reduce(action: IAction, currentState: TestState): TestState {
                return when (action) {
                    is TestAction.Increment -> currentState.copy(counter = currentState.counter * 2)
                    else -> currentState
                }
            }
        }

        // Order matters: CounterReducer adds 1, then DoublerReducer doubles it
        val reducer = CompositeReducer(
            CounterReducer(),  // counter becomes 1
            DoublerReducer()   // counter becomes 2
        )

        val initialState = TestState(counter = 0)
        val result = reducer.reduce(TestAction.Increment, initialState)

        assertEquals(2, result.counter)
    }

    @Test
    fun `order of reducers affects final state when modifying same property`() {
        class SetCounterTo5 : IReducer<TestState> {
            override fun reduce(action: IAction, currentState: TestState): TestState {
                return when (action) {
                    is TestAction.Increment -> currentState.copy(counter = 5)
                    else -> currentState
                }
            }
        }

        // First order: Increment then SetTo5
        val reducer1 = CompositeReducer(
            CounterReducer(),  // counter becomes 1
            SetCounterTo5()    // counter becomes 5 (overrides)
        )

        // Second order: SetTo5 then Increment
        val reducer2 = CompositeReducer(
            SetCounterTo5(),   // counter becomes 5
            CounterReducer()   // counter becomes 6 (increments from 5)
        )

        val initialState = TestState(counter = 0)
        val result1 = reducer1.reduce(TestAction.Increment, initialState)
        val result2 = reducer2.reduce(TestAction.Increment, initialState)

        assertEquals(5, result1.counter)
        assertEquals(6, result2.counter)
    }

    object UnknownAction : IAction

    @Test
    fun `returns unchanged state when no reducer handles action`() {
        val reducer = CompositeReducer(
            CounterReducer(),
            TextReducer(),
            FlagReducer()
        )

        val initialState = TestState(counter = 10, text = "test", flag = true)

        // UnknownAction is not handled by any reducer
        val result = reducer.reduce(UnknownAction, initialState)

        assertEquals(initialState, result)
    }

    @Test
    fun `handles empty reducer list`() {
        val reducer = CompositeReducer<TestState>()

        val initialState = TestState(counter = 5)
        val result = reducer.reduce(TestAction.Increment, initialState)

        // With no reducers, state should remain unchanged
        assertEquals(initialState, result)
    }

    @Test
    fun `single reducer in composite behaves like standalone reducer`() {
        val compositeReducer = CompositeReducer(CounterReducer())
        val standaloneReducer = CounterReducer()

        val initialState = TestState()
        val compositeResult = compositeReducer.reduce(TestAction.Increment, initialState)
        val standaloneResult = standaloneReducer.reduce(TestAction.Increment, initialState)

        assertEquals(standaloneResult, compositeResult)
    }

    @Test
    fun `multiple actions are processed correctly`() {
        val reducer = CompositeReducer(
            CounterReducer(),
            TextReducer(),
            FlagReducer()
        )

        var state = TestState()
        state = reducer.reduce(TestAction.Increment, state)
        state = reducer.reduce(TestAction.Increment, state)
        state = reducer.reduce(TestAction.SetText("world"), state)
        state = reducer.reduce(TestAction.ToggleFlag, state)
        state = reducer.reduce(TestAction.Decrement, state)

        assertEquals(1, state.counter)
        assertEquals("world", state.text)
        assertEquals(true, state.flag)
    }

    @Test
    fun `reducers only modify state for their handled actions`() {
        val reducer = CompositeReducer(
            CounterReducer(),
            TextReducer()
        )

        val initialState = TestState(counter = 5, text = "initial", flag = true)
        val result = reducer.reduce(TestAction.SetText("modified"), initialState)

        // Counter and flag should remain unchanged
        assertEquals(5, result.counter)
        assertEquals("modified", result.text)
        assertEquals(true, result.flag)
    }
}
