package dev.amaro.sonic

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
 * 5. Agent must prevent infinite dispatch loops by checking conditions
 *
 * THREAD SAFETY:
 * - process() runs on StateManager's dispatcher (single-threaded)
 * - No external synchronization needed
 * - Access to stateManager.listen().value is thread-safe
 *
 * ERROR HANDLING:
 * - Exceptions propagate to UI observer
 * - Agent should handle errors gracefully and return state
 *
 * EXAMPLE - Result Clearing Agent:
 * ```kotlin
 * class ResultClearingAgent<T>(
 *     private val stateManager: IStateManager<T>,
 *     private val config: ResultClearingConfig,
 *     private val extractResult: (T) -> Status?,
 *     private val createClearAction: () -> IAction
 * ) : IStateAgent<T> {
 *     private var lastResult: Status? = null
 *     private var clearJob: Job? = null
 *
 *     override suspend fun process(state: T): T {
 *         val result = extractResult(state)
 *
 *         // Loop prevention: only dispatch if result changed
 *         if (result != lastResult) {
 *             clearJob?.cancel()
 *             lastResult = result
 *
 *             if (result != null) {
 *                 clearJob = stateManager.scopedPerform {
 *                     val delay = when (result) {
 *                         is Status.Running -> return@scopedPerform IAction.NOOP
 *                         is Status.Success -> config.successClearDelayMs
 *                         is Status.Failure -> config.errorClearDelayMs
 *                     }
 *                     delay(delay)
 *
 *                     // Verify result hasn't changed before clearing
 *                     if (extractResult(stateManager.listen().value) == result) {
 *                         createClearAction()
 *                     } else {
 *                         IAction.NOOP  // Result changed, don't clear
 *                     }
 *                 }
 *             }
 *         }
 *
 *         return state  // Never modify state
 *     }
 * }
 * ```
 */
interface IStateAgent<T> {
    /**
     * Process state before it's delivered to UI.
     *
     * This suspend function is called for:
     * - Initial state (when listen() first emits)
     * - Every state update from the reducer
     * - State updates triggered by agent dispatches
     *
     * Agent can:
     * - Observe state and make decisions
     * - Dispatch actions via stateManager.scopedPerform()
     * - Perform async operations (don't block indefinitely)
     *
     * Agent MUST:
     * - Return state unmodified (same or equal object)
     * - Prevent infinite dispatch loops via conditions
     * - Handle exceptions gracefully
     * - Cancel previous jobs if needed (via Job reference returned by scopedPerform)
     *
     * @param state The state produced by the reducer.
     *              This is guaranteed to be the result of reduce(),
     *              not a modified state.
     *
     * @return The state to deliver to UI.
     *         MUST be same object or structurally equal (no modifications).
     *         If agent modifies state, it breaks the unidirectional contract.
     *
     * @throws CancellationException if StateManager scope is cancelled.
     *         This is expected and should be allowed to propagate.
     *
     * @throws Exception If agent fails unexpectedly.
     *         Exception propagates to UI observer.
     *         Consider catching and logging in agent implementation.
     *
     * LOOP PREVENTION PATTERN:
     * To prevent infinite dispatch, use a condition that becomes false
     * after dispatch:
     *
     * ```kotlin
     * if (state.hasNewResult) {  // True initially
     *     stateManager.reduce(Action.ClearResult)
     *     // After reduce: state.result = null
     *     // Next process() call: hasNewResult = false, no dispatch
     * }
     * ```
     *
     * CANCELLATION:
     * - If StateManager scope is cancelled, all agent operations stop
     * - Any delay() or async operation will raise CancellationException
     * - This is normal and expected lifecycle behavior
     *
     * MULTIPLE AGENTS:
     * - All agents see the SAME original state (the one from reducer)
     * - If Agent A dispatches, Agent B doesn't immediately see changes
     * - Agent B sees effects of Agent A's dispatch in the NEXT state update
     *
     * TIMING:
     * - Agents run AFTER reducer completes
     * - Agents run BEFORE state reaches UI observer
     * - All agent processing is sequential (single-threaded)
     */
    suspend fun process(state: T): T
}
