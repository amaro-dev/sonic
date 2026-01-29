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

5. **Scheduling actions from middleware**

   The `IProcessor.schedule()` method allows middleware to queue actions that execute **after** the current state
   reduction completes. This is useful for follow-up actions that depend on the updated state.

   ```kotlin
   class ValidationMiddleware : IMiddleware<FormState> {
       override suspend fun process(action: IAction, state: FormState, processor: IProcessor<FormState>) {
           when (action) {
               is Action.Submit -> {
                   processor.reduce(action)  // Apply state update immediately
                   processor.schedule(Action.ValidateSubmission)  // Queue follow-up
               }
               is Action.ValidateSubmission -> {
                   // Runs AFTER Submit completes, sees updated state
                   val isValid = state.form.isComplete && state.form.hasEmail
                   if (isValid) {
                       processor.reduce(Action.MarkValid)
                   } else {
                       processor.reduce(Action.ShowValidationError("Form incomplete"))
                   }
               }
               else -> processor.perform(action)
           }
       }
   }
   ```

   **Key characteristics:**
   - **Execution order**: State update → scheduled actions (FIFO) → agents → UI render
   - **Thread-safe**: Uses `Mutex` + `ConcurrentLinkedQueue` for safe concurrent middleware access
   - **Re-enters middleware**: Scheduled actions go through `perform()`, allowing middleware interception
   - **Use cases**: Derived actions, post-update validation, chained operations

6. **When to use schedule() vs perform() vs reduce()**

   Choose the right `IProcessor` method based on when and how the action should execute:

   | Method                 | When to Use                                      | Execution Timing                 | Middleware Interaction |
      |------------------------|--------------------------------------------------|----------------------------------|------------------------|
   | `processor.perform()`  | Continue through middleware pipeline             | Immediate, re-enters middleware  | Intercepted by all middleware |
   | `processor.reduce()`   | Skip middleware, apply reduction now             | Immediate, bypasses middleware   | Skips middleware entirely |
   | `processor.schedule()` | Defer until after current reduction completes    | After state update, before agents | Re-enters middleware via perform() |

   **Examples:**

   ```kotlin
   // Use perform() to let other middleware intercept
   override suspend fun process(action: IAction, state: T, processor: IProcessor<T>) {
       if (action is Action.LogEntry) {
           log(action.message)
           processor.perform(action) // Continue to next middleware/reducer
       }
   }

   // Use reduce() to bypass middleware (e.g., validation failures)
   override suspend fun process(action: IAction, state: T, processor: IProcessor<T>) {
       if (action is Action.AddItem && action.item.price < 0) {
           processor.reduce(Action.ValidationError("Price must be positive"))
           return // Don't continue the original action
       }
       processor.perform(action)
   }

   // Use schedule() for follow-up actions depending on state changes
   override suspend fun process(action: IAction, state: T, processor: IProcessor<T>) {
       if (action is Action.DeleteUser) {
           processor.reduce(action) // Delete user first
           processor.schedule(Action.CleanupUserData) // Then cleanup (sees updated state)
       }
   }
   ```

## Tips

- Keep middleware focused: one responsibility per class makes testing and reuse easy.
- Prevent loops in agents by checking whether the condition actually changed before dispatching.
- When composing multiple agents, order matters only if they rely on shared side effects. Each receives the same state
  snapshot produced by the reducer.
- Use `schedule()` to avoid re-entrancy issues: If middleware needs to dispatch follow-up actions after state updates,
  `schedule()` ensures the current reduction completes before the next action executes.

## Next Steps

- Pair agents with `docs/features/RESULT_HANDLING.md` to auto-clear banners.
- Combine middleware with dispatcher guidance from `docs/features/DISPATCHERS.md` when running on non-Android platforms.
