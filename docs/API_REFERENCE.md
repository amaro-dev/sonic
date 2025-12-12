# API Reference (High-Level)

## Overview

This catalog lists the primary classes and interfaces shipped in `library/src/main/java/dev/amaro/sonic`, along with
their intent and the documentation section that explains them in depth.

## Key Points

- Use this as a quick lookup table when navigating the codebase.
- Each entry references the feature doc that teaches real-world usage.

## Prerequisites

- Familiarity with the core concepts in `docs/CONCEPTS.md`.

## Reference Table

| File                                                    | Description                                         | See Also                         |
|---------------------------------------------------------|-----------------------------------------------------|----------------------------------|
| `IAction.kt`                                            | Marker interface for user/business intents.         | Concepts §1                      |
| `IReducer.kt`                                           | Pure function to transform state.                   | Concepts §2                      |
| `IStateManager.kt` / `StateManager.kt`                  | Core state container with middleware support.       | Concepts §3; features/binding.md |
| `IMiddleware.kt`                                        | Action interceptor contract.                        | features/middleware_agents.md    |
| `DirectMiddleware.kt`, `ConditionedDirectMiddleware.kt` | Ready-made middleware implementations.              | features/middleware_agents.md    |
| `IRenderer.kt`, `IPerformer.kt`                         | UI binding interfaces.                              | features/binding.md              |
| `Binding.kt`                                            | `bindState` helper for lifecycle-safe rendering.    | features/binding.md              |
| `StateBinding.kt`                                       | Value/performer wrapper for Compose previews or DI. | features/binding.md              |
| `IProcessor.kt`, `Processor.kt`                         | Internal bridge between middleware and reducer.     | Concepts §3                      |
| `ResultInfo.kt`                                         | Unified success/failure model.                      | features/result_handling.md      |
| `ResultBuilder.kt`                                      | Fluent DSL for building `ResultInfo`.               | features/result_handling.md      |
| `ResultClearingAgent.kt`, `ResultClearingConfig.kt`     | Auto-clearing agent and config.                     | features/result_handling.md      |
| `IStatusContainer.kt`                                   | Optional mixin exposing `resultInfo`.               | features/result_handling.md      |
| `IStateAgent.kt`                                        | State reaction contract.                            | features/middleware_agents.md    |
| `Extensions.kt`                                         | Flow helpers (`collectOn`, `withAgent`, etc.).      | features/middleware_agents.md    |
| `StateSelectors.kt`                                     | `selectDistinct` extensions for Flow/StateFlow.     | features/selectors.md            |

## Next Steps

- When you modify or add APIs, update this table so the map stays accurate.
- Use the referenced feature docs for complete usage guidance.
