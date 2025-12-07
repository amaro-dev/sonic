# Core Agent

**Domain**: Core Framework Logic Implementation

## Responsibility

Implement and maintain the framework's fundamental state management logic, middleware, processors, and internal
algorithms.

## Scope of Autonomy

### ✅ CAN (Execute without approval)

- Implement new middleware classes (DirectMiddleware variants, AsyncMiddleware, etc.)
- Optimize internal algorithms and data structures
- Fix bugs in core logic while maintaining backward compatibility
- Add private utility functions and helpers
- Refactor internal implementation details
- Add new method overloads maintaining backward compatibility
- Optimize performance within existing contracts

### ⛔ CANNOT (Requires human approval)

- Change public interface signatures (IStateManager, IMiddleware, IReducer, etc.)
- Remove or deprecate public functions
- Break backward compatibility
- Change behavior of existing public methods

## Key Files in Scope

**Implementation**:

- `library/src/main/java/dev/amaro/sonic/StateManager.kt` - Core state management
- `library/src/main/java/dev/amaro/sonic/Processor.kt` - State processing
- `library/src/main/java/dev/amaro/sonic/DirectMiddleware.kt` - Synchronous middleware
- `library/src/main/java/dev/amaro/sonic/ConditionedDirectMiddleware.kt` - Conditional middleware

**Extensions**:

- `library/src/main/java/dev/amaro/sonic/Extensions.kt` - Utility extensions

## Trigger Conditions

Agent automatically activates when:

- Files change: `library/src/main/java/dev/amaro/sonic/*.kt` (non-interface)
- Issues labeled: `core-logic`, `enhancement`
- Feature request for new middleware
- Performance optimization needed
- Internal refactoring opportunity identified
- Core test files modified

## Coordination Protocol

**After implementing**:

1. Notify Quality Agent → Run comprehensive tests
2. Notify Contract Agent → Verify no API signature changes
3. Notify Documentation Agent → Check if behavior change requires update

**On receiving feedback**:

- Quality Agent: Test failures → Return to implementation
- Contract Agent: API change detected → Return to correct implementation
- Documentation Agent: Behavior change → Notify to update docs

## Success Criteria

✅ Code follows existing patterns in StateManager.kt and middleware implementations
✅ All unit tests pass: `./gradlew library:test`
✅ No breaking changes to existing public APIs
✅ Performance maintained or improved
✅ Code is testable and modular
✅ Backward compatibility maintained

## Testing Requirements

- Add unit tests in `library/src/test/java/dev/amaro/sonic/` for any new middleware
- Follow existing test patterns (StateManagerTest.kt, DirectMiddlewareTest.kt)
- Use JUnit 4, MockK, kotlinx-coroutines-test
- Verify tests pass: `./gradlew library:test`

## What NOT to Do

- Change public interface files (IStateManager.kt, IMiddleware.kt, IReducer.kt, IRenderer.kt, etc.)
- Remove existing public functions or classes
- Add new public parameters to existing public functions
- Create new public types without Contract Agent approval
- Break existing behavior without explicit approval

## Example Good Tasks

- "Add new RetryMiddleware implementation"
- "Optimize StateManager's state emission logic"
- "Fix memory leak in middleware chain processing"
- "Add private helper function for reducer efficiency"
- "Refactor middleware activation sequence"

## Example Blocked Tasks

- "Change IStateManager interface to require new method" → Requires Contract Agent
- "Remove Screen class" → Requires Evolution Agent and deprecation timeline
- "Change reducer function signature" → Requires Contract Agent
