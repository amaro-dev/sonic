# Middleware & State Agents

## Overview

Middleware (`IMiddleware`) intercepts actions before they reach the reducer, while state agents (`IStateAgent`) observe
emitted states and can dispatch follow-up actions. Together they let you keep reducers pure, isolate side effects, and
add cross-cutting behaviors such as logging or result clearing.

## Key Points

- Middleware runs in the order they are added to `StateManager` (`addMiddleware`).
- Agents run on the manager’s dispatcher after the reducer produces a new state.
- `Extensions.kt` provides helpers (`withAgent`, `withAgents`, `collectOn`, `collectOnDefault`) for composing agents
  into Flow pipelines.

## When to Use

- **Middleware**: choose middleware when you need to validate, throttle, log, or block an action **before** the reducer
  mutates state.
- **Agents**: reach for agents when you want to trigger analytics, navigation, result clearing, or chained actions *
  *after** the new state is emitted.

## Prerequisites

- Understanding of the state loop from `docs/CONCEPTS.md`.
- A `StateManager` instance you can customize.

## Workflow

1. **Creating middleware**
   ```kotlin
   class LoggingMiddleware<T>(
       private val logger: (IAction) -> Unit
   ) : IMiddleware<T> {
       override suspend fun process(action: IAction, state: T, processor: IProcessor<T>) {
           logger(action)
           processor.perform(action) // continue the chain
       }
   }

   class ValidationMiddleware : IMiddleware<NoteState> {
       override suspend fun process(action: IAction, state: NoteState, processor: IProcessor<NoteState>) {
           if (action is Action.AddNote && action.note.title.isBlank()) {
               processor.reduce(
                   Action.LoadFailed(
                       Status.Failure(code = "VALIDATION", message = "Title can't be empty")
                   )
               )
           } else {
               processor.perform(action)
           }
       }
   }
   ```
    - Register in your manager’s init block: `addMiddleware(LoggingMiddleware(::println))`.

2. **Conditionally intercepting**
    - Use `DirectMiddleware` or `ConditionedDirectMiddleware` for simple “if action is X then do Y” cases without
      writing a full class.

3. **Implementing state agents**
   ```kotlin
   class AnalyticsAgent(
       private val analytics: AnalyticsService
   ) : IStateAgent<NoteState> {
       private var lastCount = -1

       override suspend fun process(state: NoteState): NoteState {
           if (state.notes.size != lastCount) {
               lastCount = state.notes.size
               analytics.track("note_count", mapOf("count" to lastCount))
           }
           return state
       }
   }
   ```
    - Attach via Flow helpers:
   ```kotlin
   manager.listen()
       .withAgents(
           StatusClearingAgent(...),
           AnalyticsAgent(analytics)
       )
       .collect { state -> renderer.render(state, performer) }
   ```

4. **Dispatching from agents**
    - Use `stateManager.scopedPerform { action }` inside an agent for async operations (see `StatusClearingAgent` for
      the canonical pattern with cancellation + verification).

## Tips

- Keep middleware focused: one responsibility per class makes testing and reuse easy.
- Prevent loops in agents by checking whether the condition actually changed before dispatching.
- When composing multiple agents, order matters only if they rely on shared side effects. Each receives the same state
  snapshot produced by the reducer.

## Next Steps

- Pair agents with `docs/features/RESULT_HANDLING.md` to auto-clear banners.
- Combine middleware with dispatcher guidance from `docs/features/DISPATCHERS.md` when running on non-Android platforms.
