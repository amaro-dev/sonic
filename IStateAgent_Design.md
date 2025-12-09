# IStateAgent Interface Design - Complete Specification

## Overview

`IStateAgent<T>` is a framework interface that allows **optional, composable state processors** to intercept and
transform state before it reaches UI. Agents are a core architectural concept (alongside Middleware) for extending
Sonic's behavior.

**Key principle**: Agents are **state-in, state-out** processors. They observe state changes and can dispatch actions to
trigger new state updates.

---

## Interface Definition

```kotlin
/**
 * A state agent intercepts state updates and can trigger actions.
 *
 * Agents are called AFTER the reducer produces new state and BEFORE
 * the state is delivered to the UI through listen().
 *
 * Use cases:
 * - Auto-clearing results/status after delays
 * - Logging state changes for analytics/debugging
 * - Enforcing state constraints (e.g., "if X is set, Y must be cleared")
 * - Side effects based on state transitions
 * - Performance monitoring
 *
 * Key contract:
 * 1. Agent receives state AFTER reducer runs (guaranteed)
 * 2. Agent can dispatch actions via IStateManager
 * 3. Agent must return state (may be same or modified)
 * 4. Agent must not block (respect cancellation)
 * 5. Agent dispatch may trigger new state, causing re-processing
 *
 * IMPORTANT: Agents must prevent infinite dispatch loops.
 * See "Dispatch Safety" section below.
 */
interface IStateAgent<T> {
    /**
     * Process state before it's delivered to UI.
     *
     * Called when:
     * - Initial state is created
     * - Reducer produces new state
     * - Any action completes (including those dispatched by agents)
     *
     * @param state The state produced by reducer (never null)
     * @return The state to deliver to UI (typically same as input)
     *
     * @throws Exception If agent fails, exception propagates to UI observer.
     *                   Consider catching and handling in agent.
     *
     * Thread safety: Called on StateManager's dispatcher (single-threaded)
     */
    suspend fun process(state: T): T
}
```

---

## Usage Pattern

```kotlin
// Define agent
class ResultClearingAgent<T>(
    private val stateManager: IStateManager<T>,
    private val config: ResultClearingConfig,
    private val extractResult: (T) -> ResultInfo?,
    private val createClearAction: () -> IAction
) : IStateAgent<T> {
    private var currentClearJob: Job? = null
    private var lastResult: ResultInfo? = null

    override suspend fun process(state: T): T {
        val result = extractResult(state)

        // Only process if result changed
        if (result != lastResult) {
            currentClearJob?.cancel()
            lastResult = result

            if (result != null) {
                // Schedule clearing
                currentClearJob = scope.launch {
                    delay(getDelay(result))
                    if (extractResult(stateManager.listen().value) == result) {
                        stateManager.reduce(createClearAction())
                    }
                }
            }
        }

        return state  // Return unmodified state
    }
}

// Wire agents into flow
stateManager.listen()
    .withAgent(ResultClearingAgent(...))
    .withAgent(LoggingAgent(...))
    .collectAsState()
```

---

## Edge Cases & Solutions

### Edge Case 1: Infinite Dispatch Loop

**Problem**:

```kotlin
class BadAgent : IStateAgent<AppState> {
    override suspend fun process(state: AppState): T {
        stateManager.reduce(Action.SomeAction)  // Dispatch
        // This causes:
        // 1. Reducer produces new state
        // 2. Agent.process() called again with new state
        // 3. Agent dispatches again
        // 4. Infinite loop
        return state
    }
}
```

**Solution: Condition-Based Dispatch**

Agents **must** check if dispatch is needed:

```kotlin
class SafeAgent : IStateAgent<AppState> {
    override suspend fun process(state: AppState): AppState {
        // Only dispatch if specific condition met
        if (state.needsCleaning && state.result != null) {
            stateManager.reduce(Action.ClearResult)
        }
        return state
    }
}
```

**Mechanism**: Agent observes state, dispatches ONLY if a specific condition is met that the action will CHANGE. Once
state has the desired property, agent doesn't dispatch.

**Example - Result Clearing**:

```kotlin
override suspend fun process(state: AppState): AppState {
    val result = extractResult(state)

    // Dispatch ONLY if result is new (timestamp changed)
    if (result != null && result != lastResult) {
        scheduleClearing()
    }

    // Once result is cleared (null), agent won't dispatch again
    // because result will be null
    return state
}
```

**Test for this**:

```kotlin
@Test
fun `Agent does not dispatch if same result exists`() = runTest {
    val state1 = AppState(result = Success(...))
    val state2 = AppState(result = Success(...))  // Same result

    agent.process(state1)
    agent.process(state2)

    // Should not have dispatched on state2 because result didn't change
    verify { stateManager.reduce(any()) } // Called once, not twice
}
```

---

### Edge Case 2: Agent Scope Cancellation

**Problem**: What if StateManager scope is cancelled while agent is processing?

```kotlin
override suspend fun process(state: T): T {
    delay(5000)  // Long operation
    // What if scope.cancel() happens here?
    return state
}
```

**Solution: Structured Concurrency**

Agents run in StateManager's scope, which is cancelled when StateManager is disposed. The `delay()` will throw
`CancellationException`, which is the correct behavior.

```kotlin
override suspend fun process(state: T): T {
    try {
        delay(5000)
    } catch (e: CancellationException) {
        // Scope is being cancelled, allow it to propagate
        throw e
    } catch (e: Exception) {
        // Handle other errors gracefully
        logger.error("Agent failed", e)
        // Return state unchanged - UI gets last known good state
    }
    return state
}
```

**Documentation**: Agents should document that they're cancellation-safe and inherit StateManager lifecycle.

---

### Edge Case 3: Agent Modifying State

**Problem**: Can agent modify state before returning?

```kotlin
override suspend fun process(state: AppState): T {
    return state.copy(timestamp = System.currentTimeMillis())  // Modify?
}
```

**Decision**: **NO - Agents should NOT modify state.**

**Reasons**:

1. State should only change via reducer
2. Modifications bypass reducer logic
3. Multiple agents could conflict
4. Breaks the UI contract ("state is reduced")

**Solution**: Enforce in documentation and type system

```kotlin
/**
 * Process state before it's delivered to UI.
 *
 * IMPORTANT: Agents should NOT modify state.
 * To change state, dispatch an action via stateManager.reduce()
 * which will trigger reducer and create new state.
 *
 * Returning modified state will confuse observers and break
 * the unidirectional flow contract.
 */
suspend fun process(state: T): T
```

**Test for this**:

```kotlin
@Test
fun `Agent does not modify state`() = runTest {
    val state = AppState(count = 5)
    val result = agent.process(state)

    // Assert no modifications
    assertEquals(state.count, result.count)
    assertEquals(state, result)  // Same object or equal
}
```

---

### Edge Case 4: Multiple Agents - Order Dependency

**Problem**: Agent order matters

```kotlin
val flow = stateManager.listen()
    .withAgent(AgentA)  // Clears result after 3s
    .withAgent(AgentB)  // Logs result
    .collectAsState()

// vs.

val flow = stateManager.listen()
    .withAgent(AgentB)  // Logs result (sees original)
    .withAgent(AgentA)  // Clears result after 3s
    .collectAsState()
```

**Both flows see the SAME state** - they process sequentially and both see original state. Agent order doesn't affect
what state they process, only the order they run.

```kotlin
fun <T> Flow<T>.withAgent(agent: IStateAgent<T>): Flow<T> =
    transform { state ->
        emit(agent.process(state))  // Agent A processes, returns state
    }

// Next: .withAgent(AgentB) receives same state
```

**But**: If agents dispatch actions, order matters:

```kotlin
.withAgent(AgentA)  // Dispatches Action.Clear
.withAgent(AgentB)  // When AgentB runs, did Clear execute?
```

**Solution**: Document that agents see state **at the moment flow processing started**, not state from dispatches by
earlier agents.

```kotlin
/**
 * All agents in a flow see the SAME state (the one produced by reducer).
 *
 * If Agent A dispatches an action, it doesn't change the state that
 * Agent B sees. Both see the original state.
 *
 * Agent B might see effects of Agent A's dispatch in the NEXT state update.
 *
 * Example:
 * State: count=1
 * AgentA: sees count=1, dispatches Increment
 * AgentB: sees count=1 (not 2 - Increment hasn't run yet)
 * Later: Increment processes, count becomes 2
 * Next state: count=2
 * Both agents see count=2
 */
```

---

### Edge Case 5: Agent Exception Handling

**Problem**: What if agent throws?

```kotlin
override suspend fun process(state: T): T {
    if (someCondition) {
        throw IllegalStateException("Invalid state!")
    }
    return state
}
```

**Solution: Exceptions propagate to UI observer**

```kotlin
stateManager.listen()
    .withAgent(MyAgent())
    .collectAsState()
    // If agent throws, exception bubbles here
```

**Best practice**: Agents should handle errors gracefully

```kotlin
override suspend fun process(state: T): T {
    try {
        // Agent logic
    } catch (e: Exception) {
        logger.error("Agent failed, returning state unchanged", e)
        // Return unmodified state - UI continues with last good state
    }
    return state
}
```

**Tests**:

```kotlin
@Test
fun `Agent exception propagates to observer`() = runTest {
    val badAgent = object : IStateAgent<String> {
        override suspend fun process(state: String) =
            throw Exception("Test error")
    }

    val flow = flowOf("state").withAgent(badAgent)

    assertThrows<Exception> {
        flow.single()  // Collecting throws
    }
}

@Test
fun `Agent that handles exception returns state`() = runTest {
    val gracefulAgent = object : IStateAgent<String> {
        override suspend fun process(state: String) {
            try {
                throw Exception("Test error")
            } catch (e: Exception) {
                // Handle gracefully
            }
            return state
        }
    }

    val flow = flowOf("state").withAgent(gracefulAgent)

    val result = flow.single()
    assertEquals("state", result)
}
```

---

### Edge Case 6: Agent Accessing Outdated StateManager State

**Problem**:

```kotlin
override suspend fun process(state: AppState): AppState {
    delay(1000)  // Long delay
    val currentManagerState = stateManager.listen().value
    // currentManagerState might be different from `state` parameter!
    return state
}
```

**This is INTENTIONAL behavior**, not a bug.

**Explanation**:

- `state` parameter = state when reducer completed
- `stateManager.listen().value` = current live state (may have changed due to other actions)

**Use case**: Agent might want to know current manager state to make decisions:

```kotlin
override suspend fun process(state: AppState): AppState {
    // Delay, then check if another action changed state
    delay(5000)

    val currentState = stateManager.listen().value
    if (currentState.result == state.result) {
        // Result unchanged, safe to clear
        stateManager.reduce(Action.ClearResult)
    }
    // else: result changed, don't clear

    return state
}
```

**Document this clearly**:

```kotlin
/**
 * Parameter 'state': The state produced when agent.process() was called
 * stateManager.listen().value: The CURRENT state (may differ from parameter)
 *
 * Use 'state' for: Observing what state triggered this agent
 * Use 'listen().value' for: Checking if something else has changed state
 */
```

---

### Edge Case 7: Fast Successive State Updates

**Problem**:

```
T0: State update → Agent.process() called
T1: Before Agent.process() completes, another action fires
T2: New state → Agent.process() called again
T3: First Agent.process() completes
T4: Second Agent.process() completes
```

**This is safe** because:

1. Agent.process() is suspend function (not blocking)
2. Each call is independent
3. StateManager queues actions sequentially

**Example - Result Clearing**:

```
T0: Success result arrives
    Agent schedules clear job (5000ms)
T1000: User initiates new operation
    New Success result (different timestamp)
    Agent.process() called again
    → Cancels previous clear job
    → Schedules new clear job (5000ms)
T6000: Clear fires (not T5000, because timer restarted)
```

**Tests**:

```kotlin
@Test
fun `Rapid state updates reset agent timer`() = runTest {
    val state1 = AppState(result = Success(timestamp = 1000))
    val state2 = AppState(result = Success(timestamp = 2000))

    agent.process(state1)
    advanceTimeBy(2000)

    // Before clear fires (at 5000), new state arrives
    agent.process(state2)

    advanceTimeBy(3000)  // Now at T5000 from T0, but T3000 from state2

    // First clear should have been cancelled
    // Only second clear should fire
    verify { stateManager.reduce(any()) } // Once, not twice
}
```

---

### Edge Case 8: Agent Receiving Initial State

**Problem**: Does agent run for initial state?

```kotlin
val manager = StateManager(initialState = AppState())
val flow = manager.listen()
    .withAgent(MyAgent())
    .collectAsState()

// Does MyAgent.process(initialState) get called?
```

**Answer: YES**

**Rationale**:

- Initial state is still a state
- Agent might need to initialize internal structures
- Consistent behavior - agent sees all states

**Example**:

```kotlin
override suspend fun process(state: AppState): AppState {
    val result = extractResult(state)

    // First call: result is null (initial state)
    // lastResult is null → They're equal, no scheduling
    // But agent tracks that it's seen initial state

    if (result != lastResult) {
        // On second state update with non-null result
        // This condition is true, scheduling starts
    }

    lastResult = result
    return state
}
```

**Document this**:

```kotlin
/**
 * Agent is called for initial state AND all subsequent state updates.
 *
 * This allows agents to:
 * - Initialize internal state tracking (e.g., lastResult = null)
 * - Inspect initial state and decide if action needed
 * - Establish invariants (e.g., "if X exists, Y must be set")
 */
```

---

### Edge Case 9: Agent Accessing Private StateManager Fields

**Problem**: Agent might need access to StateManager internals

```kotlin
class ResultClearingAgent(
    private val stateManager: IStateManager<T>
) : IStateAgent<T> {
    override suspend fun process(state: T): T {
        // Need scope for launching clear job
        // But IStateManager doesn't expose scope!
        val scope = ???  // Error - no way to access
    }
}
```

**Solution: StateManager must expose required internals**

Update IStateManager:

```kotlin
interface IStateManager<T> {
    fun listen(): StateFlow<T>
    fun perform(action: IAction)

    // NEW: Expose scope for agents and internal use
    val scope: CoroutineScope  // protected, but accessible to agents
}
```

Or: Create internal interface for agents:

```kotlin
interface IAgentStateManager<T> : IStateManager<T> {
    // Agent-specific methods
    val scope: CoroutineScope
    fun reduce(action: IAction)  // Lower-level reduce, not just perform
}

// Result clearing agent accepts this:
class ResultClearingAgent<T>(
    private val stateManager: IAgentStateManager<T>,
    ...
)
```

**Decision**: Expose minimal necessary internals on IStateManager:

```kotlin
interface IStateManager<T> {
    fun listen(): StateFlow<T>
    fun perform(action: IAction)
    fun reduce(action: IAction)  // NEW - for agents

    // Or as property
    val scope: CoroutineScope  // NEW - needed for agent timers
}
```

---

## Complete Interface with Safeguards

```kotlin
/**
 * A state agent processes state before it reaches UI.
 *
 * Agents provide a composable way to:
 * - React to state changes (without modifying them)
 * - Dispatch actions to trigger new states
 * - Perform side effects based on state
 * - Enforce state constraints
 *
 * CONTRACT:
 * 1. Agent receives state AFTER reducer runs
 * 2. Agent may dispatch actions via IStateManager
 * 3. Agent must NOT modify state (return same or equal object)
 * 4. Agent must respect cancellation (CancellationException)
 * 5. Agent must prevent infinite dispatch loops
 *
 * THREAD SAFETY:
 * - Agent.process() runs on StateManager's dispatcher (single-threaded)
 * - No external synchronization needed
 * - Access to stateManager.listen().value is thread-safe
 *
 * ERROR HANDLING:
 * - Exceptions propagate to UI observer
 * - Agent should handle errors gracefully, return state
 *
 * EXAMPLES:
 *
 * ```kotlin
 * // Simple state observer
 * class LoggingAgent<T> : IStateAgent<T> {
 *     override suspend fun process(state: T): T {
 *         println("New state: $state")
 *         return state
 *     }
 * }
 *
 * // Agent that dispatches actions
 * class ResultClearingAgent<T>(
 *     private val stateManager: IStateManager<T>,
 *     private val config: ResultClearingConfig
 * ) : IStateAgent<T> {
 *     private var lastResult: ResultInfo? = null
 *
 *     override suspend fun process(state: T): T {
 *         val result = extractResult(state)
 *
 *         if (result != lastResult) {  // Loop prevention
 *             lastResult = result
 *
 *             if (result != null) {
 *                 stateManager.scope.launch {
 *                     delay(getDelay(result))
 *                     if (extractResult(stateManager.listen().value) == result) {
 *                         stateManager.reduce(createClearAction())
 *                     }
 *                 }
 *             }
 *         }
 *
 *         return state  // Never modify
 *     }
 * }
 * ```

*/
interface IStateAgent<T> {
/**
* Process state before it's delivered to UI.
*
* This is a suspend function, allowing agents to:
* - Perform async operations (but shouldn't block indefinitely)
* - Dispatch actions and wait for them to complete
* - Access external resources
*
* @param state The state produced by the reducer. This state object
* or an equal object must be returned.
*
* @return The state to deliver to UI. MUST be same object or
* structurally equal to input (agent must not modify).
*
* @throws CancellationException if StateManager scope is cancelled.
* This is expected and should propagate.
*
* @throws Exception If agent fails, exception propagates to UI observer.
* Consider catching and logging in agent.
*
* TIMING:
* - Called AFTER reducer runs with new state
* - Called BEFORE state reaches UI observer
* - Multiple agents run sequentially, each sees same original state
*
* LOOP PREVENTION:
* - Agent may dispatch actions via stateManager.reduce()
* - This creates new state, which triggers agent.process() again
* - Agent MUST include condition to prevent infinite dispatch:
*   ```kotlin
     *   if (stateChanged && shouldDispatch(state)) {
     *       stateManager.reduce(...)  // Only if condition met
     *   }
     *   ```
*/
suspend fun process(state: T): T
}

```

---

## Flow Extension Functions

```kotlin
/**
 * Add a single agent to the state flow.
 *
 * The agent is called for every state emission, before state reaches UI.
 *
 * Example:
 * ```kotlin
 * stateManager.listen()
 *     .withAgent(ResultClearingAgent(...))
 *     .collectAsState()
 * ```

*/
fun <T> Flow<T>.withAgent(agent: IStateAgent<T>): Flow<T> =
transform { state ->
try {
emit(agent.process(state))
} catch (e: Exception) {
// Let exception propagate to observer
throw e
}
}

/**

* Add multiple agents to the state flow.
*
* Agents are chained in order. Each agent receives same original state,
* but agents run sequentially.
*
* Example:
* ```kotlin
* stateManager.listen()
*     .withAgents(
*         ResultClearingAgent(...),
*         LoggingAgent(),
*         AnalyticsAgent(...)
*     )
*     .collectAsState()
* ```

*/
fun <T> Flow<T>.withAgents(vararg agents: IStateAgent<T>): Flow<T> =
agents.fold(this) { flow, agent -> flow.withAgent(agent) }

```

---

## Summary of Design Principles

1. **Single Responsibility**: Agent processes state, returns state
2. **No State Modification**: State changes only via reducer
3. **Composition**: Multiple agents can stack naturally
4. **Loop Prevention**: Agent must check conditions before dispatch
5. **Error Handling**: Exceptions propagate, agent handles gracefully
6. **Thread Safety**: Runs on StateManager dispatcher (no locks needed)
7. **Lifecycle**: Inherits StateManager scope cancellation
8. **Observable**: Transparent in flow chain - you see agents in code

---

## Testing Strategy

**Unit Tests for Agent**:
- Agent processes state without modification
- Agent prevents infinite loops
- Agent exception handling
- Multiple state transitions with timers
- Concurrent state updates reset timers

**Integration Tests with StateManager**:
- Agent receives all state updates (initial + subsequent)
- Agent can dispatch actions and see results
- Multiple agents stack correctly
- Scope cancellation cancels agent operations

**Flow Extension Tests**:
- withAgent works with collect
- withAgents chains correctly
- Exceptions propagate
- State flows through unchanged