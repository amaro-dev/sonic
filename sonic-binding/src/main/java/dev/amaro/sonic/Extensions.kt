package dev.amaro.sonic

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

fun <T> Flow<T>.collectOnDefault(
    scope: CoroutineScope,
    action: suspend (T) -> Unit
): Job = collectOn(scope, Dispatchers.Default, action)


fun <T> Flow<T>.collectOn(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher,
    action: suspend (T) -> Unit
): Job = scope.launch(dispatcher) {
    collect { action(it) }
}

/**
 * Add a single state agent to the flow.
 *
 * The agent is called for every state emission, before state reaches UI.
 * Agent processes state and returns it (potentially triggering new state updates).
 *
 * Example:
 * ```kotlin
 * stateManager.listen()
 *     .withAgent(ResultClearingAgent(stateManager, config))
 *     .collectAsState()
 * ```
 *
 * @param agent The state agent to process each state emission
 * @return A new flow with the agent processing each state
 */
fun <T> Flow<T>.withAgent(agent: IStateAgent<T>): Flow<T> =
    transform { state ->
        emit(agent.process(state))
    }

/**
 * Add multiple state agents to the flow.
 *
 * Agents are chained in order. Each agent receives the same original state
 * (the one produced by reducer), but agents run sequentially.
 *
 * If Agent A dispatches an action, Agent B doesn't immediately see the
 * resulting state change. Both agents see the state as it was when
 * processing began. Agent B will see changes in the NEXT state update.
 *
 * Example:
 * ```kotlin
 * stateManager.listen()
 *     .withAgents(
 *         ResultClearingAgent(stateManager, config),
 *         LoggingAgent(),
 *         AnalyticsAgent()
 *     )
 *     .collectAsState()
 * ```
 *
 * @param agents Variable number of state agents to chain
 * @return A new flow with all agents processing each state sequentially
 */
fun <T> Flow<T>.withAgents(vararg agents: IStateAgent<T>): Flow<T> =
    agents.fold(this) { flow, agent -> flow.withAgent(agent) }