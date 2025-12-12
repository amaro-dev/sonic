# Result Handling

## Overview

Sonic’s result suite (`ResultInfo`, `ResultBuilder`, `ResultClearingAgent`, `IStatusContainer`) standardizes how
features expose successes and failures to the UI without ad-hoc enums or brittle timers.

## Key Points

- `ResultInfo.Success` and `.Failure` capture metadata such as source, timestamp, retry-ability, and exceptions.
- `ResultBuilder` provides fluent builders (`success(...)`, `failure(...)`).
- `ResultClearingAgent` clears transient statuses after configurable delays.
- Implement `IStatusContainer` on your state when you want type-safe extraction of `resultInfo`.

## Prerequisites

- Reducer/manager set up per `docs/CONCEPTS.md`.
- Optional: Compose selectors (`docs/features/SELECTORS.md`) if you want fine-grained subscriptions.

## Workflow

1. **Emit structured results**
   ```kotlin
   class NoteStateManager(...): StateManager<NoteState>(NoteState(), scope) {
       override val reducer = IReducer<NoteState> { action, current ->
           when (action) {
               is Action.Save -> current.copy(
                   notes = current.notes + action.note,
                   result = success("LOCAL")
                       .metadata("operation", "save")
                       .buildSuccess()
               )
               is Action.SaveFailed -> current.copy(
                   result = failure("STORAGE", action.message)
                       .retryable(true)
                       .buildFailure()
               )
               is Action.ClearResult -> current.copy(result = null)
               else -> current
           }
       }
   }
   ```

2. **Integrate with UI**
   ```kotlin
   val result by manager.listen()
       .selectDistinct { it.result }
       .collectAsState(null)

   when (val info = result) {
       is ResultInfo.Success -> Text("✓ ${info.metadata["operation"]} via ${info.source}")
       is ResultInfo.Failure -> Text("✗ ${info.code}: ${info.message}")
   }
   ```

3. **Auto-clear with ResultClearingAgent**
   ```kotlin
   LaunchedEffect(manager) {
       manager.listen()
           .withAgent(
               ResultClearingAgent(
                   stateManager = manager,
                   config = ResultClearingConfig(
                       successClearDelayMs = 2000,
                       errorClearDelayMs = 5000,
                       clearOnlyRetryableErrors = false
                   ),
                   extractResult = { it.result },
                   createClearAction = { Action.ClearResult }
               )
           )
           .collect { /* no-op */ }
   }
   ```

4. **Optional: IStatusContainer**
   ```kotlin
   data class NoteState(
       val notes: List<Note> = emptyList(),
       override val resultInfo: ResultInfo? = null
   ) : IStatusContainer
   ```
    - Makes it easy for shared agents/utilities to extract results without knowing state shape.

## Tips

- Reuse the builder helpers (`success(...)`, `failure(...)`) to keep reducer code concise.
- Keep `Action.ClearResult` (or equivalent) simple so agents can dispatch it safely.
- Combine with selectors so only the banner recomposes when results change.

## Next Steps

- Continue with `docs/features/SELECTORS.md` to keep Compose screens efficient.
- Pair result handling with middleware/agents (`docs/features/MIDDLEWARE_AGENTS.md`) for analytics/logging side effects.
