# API Reference (High-Level)

## Overview

This catalog lists the primary classes and interfaces shipped across the Sonic modules (`sonic-core`, `sonic-binding`,
`sonic-result`, `sonic-compose`) under `*/src/main/java/dev/amaro/sonic`, along with their intent and the documentation
section that explains them in depth.

## Key Points

- Use this as a quick lookup table when navigating the codebase.
- Each entry references the feature doc that teaches real-world usage.

## Prerequisites

- Familiarity with the core concepts in `docs/CONCEPTS.md`.

## Reference Table

| File                                                    | Description                                              | See Also                                   |
|---------------------------------------------------------|----------------------------------------------------------|--------------------------------------------|
| `IAction.kt`                                            | Marker interface for user/business intents.              | Concepts §1                                |
| `IReducer.kt`                                           | Pure function to transform state.                        | Concepts §2                                |
| `IStateManager.kt` / `StateManager.kt`                  | Core state container with middleware support.            | Concepts §3; features/binding.md           |
| `IMiddleware.kt`                                        | Action interceptor contract.                             | features/middleware_agents.md              |
| `DirectMiddleware.kt`, `ConditionedDirectMiddleware.kt` | Ready-made middleware implementations.                   | features/middleware_agents.md              |
| `IRenderer.kt`, `IPerformer.kt`                         | UI binding interfaces.                                   | features/binding.md                        |
| `Binding.kt`                                            | `bindState` helper for lifecycle-safe rendering.         | features/binding.md                        |
| `StateBinding.kt`                                       | Value/performer wrapper for Compose previews or DI.      | features/binding.md                        |
| `IProcessor.kt`, `Processor.kt`                         | Internal bridge with perform/reduce/schedule methods.    | Concepts §6; features/middleware_agents.md |
| `Status.kt`                                             | Running/success/failure model.                           | features/result_handling.md                |
| `StatusClearingAgent.kt`, `ResultClearingConfig.kt`     | Auto-clearing agent and config.                          | features/result_handling.md                |
| `ResultActions.kt`                                      | Canonical result actions (includes `ClearStatus`).       | features/result_handling.md                |
| `IStatusContainer.kt`                                   | Optional mixin exposing `status`.                        | features/result_handling.md                |
| `IStateAgent.kt`                                        | State reaction contract.                                 | features/middleware_agents.md              |
| `Extensions.kt`                                         | Flow helpers (`collectOn`, `withAgent`, etc.).           | features/middleware_agents.md              |
| `StateSelectors.kt`                                     | `selectDistinct` extensions for Flow/StateFlow.          | features/selectors.md                      |
| `CompositionLocals.kt`                                  | `LocalStateManager` & `StateManagerContext` for Compose. | features/selectors.md                      |
| `CompositeReducer.kt`                                   | Combines multiple reducers using fold pattern.           | features/reducers.md                       |
| `SliceReducer.kt`                                       | Type-safe domain-specific reducer with selector/updater. | features/reducers.md                       |
| `ReducerBuilders.kt`                                    | DSL helpers: sliceReducer(), buildCompositeReducer().    | features/reducers.md                       |
| `ActionScheduler.kt`                                    | Internal FIFO queue for deferred actions.                | features/middleware_agents.md              |

## Next Steps

- When you modify or add APIs, update this table so the map stays accurate.
- Use the referenced feature docs for complete usage guidance.
