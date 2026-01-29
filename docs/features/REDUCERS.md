# Reducer Composition Patterns

## Overview

As applications grow, monolithic reducers handling dozens of action types across multiple feature domains become
difficult to maintain and test. Sonic provides `CompositeReducer` and `SliceReducer` to split large reducers into
smaller, domain-specific reducers with type-safe boundaries. This pattern enables teams to work on independent features
without merge conflicts and makes testing individual domains straightforward.

## Key Points

- `CompositeReducer` chains multiple reducers sequentially using a fold operation
- `SliceReducer` enables type-safe domain separation by mapping a parent reducer to a specific state slice
- Builder DSLs (`sliceReducer()`, `buildCompositeReducer {}`) reduce boilerplate and improve readability
- Order matters in composition: each reducer receives the output of the previous reducer
- Each slice reducer only accesses its specific domain, preventing accidental cross-domain mutations

## When to Use

**Use CompositeReducer/SliceReducer when:**

- Your app has 3+ distinct feature domains (user, cart, notifications, settings, etc.)
- Your monolithic reducer exceeds 100 lines
- Multiple teams work on different features simultaneously
- You want to test feature reducers independently

**Use SliceReducer specifically when:**

- Your state is structured as a composite data class with clear properties
- You need compile-time guarantees that reducers don't modify wrong state slices
- You want to prevent cross-domain coupling

**Avoid when:**

- Your app has a single, simple domain (premature abstraction)
- Your entire reducer is under 50 lines
- State changes frequently require coordinated updates across domains (though middleware can help here)

## Prerequisites

- Understanding of `IReducer` interface (see [CONCEPTS.md](../CONCEPTS.md) Section 2)
- StateManager with composite state structure (e.g., `AppState` with multiple properties)
- Familiarity with Kotlin higher-order functions and lambdas
- Basic knowledge of immutable state updates

## Workflow

### 1. Problem: The Monolithic Reducer Anti-Pattern

As applications grow, a single reducer handling all domains becomes unwieldy:

```kotlin
class MonolithicReducer : IReducer<AppState> {
    override fun reduce(action: IAction, currentState: AppState): AppState {
        return when (action) {
            // User actions (lines 271-279)
            is UserAction.Login -> currentState.copy(
                user = currentState.user.copy(
                    username = action.username,
                    isLoggedIn = true
                )
            )
            is UserAction.Logout -> currentState.copy(user = UserState())

            // Cart actions (lines 281-302)
            is CartAction.AddItem -> {
                val newItems = currentState.cart.items + action.item
                currentState.copy(
                    cart = currentState.cart.copy(
                        items = newItems,
                        total = newItems.sumOf { it.price }
                    )
                )
            }
            is CartAction.RemoveItem -> { /* ... */
            }
            is CartAction.ClearCart -> currentState.copy(cart = CartState())

            // Notification actions (lines 304-312)
            is NotificationAction.Show -> currentState.copy(
                notifications = currentState.notifications.copy(
                    messages = currentState.notifications.messages + action.message,
                    count = currentState.notifications.count + 1
                )
            )
            is NotificationAction.Clear -> currentState.copy(notifications = NotificationState())

            else -> currentState
        }
    }
}
```

**Problems:**

- Single file grows to 300+ lines as features are added
- Nested `.copy()` calls are error-prone and hard to read
- Testing requires mocking entire `AppState` even for single-domain tests
- Merge conflicts when multiple developers work on different features
- No compile-time protection against modifying wrong state slices

### 2. Solution: Domain Separation with SliceReducer

Split the monolithic reducer into focused domain reducers, each handling only its state slice:

```kotlin
// User domain: Only sees UserState
class UserReducer : IReducer<UserState> {
    override fun reduce(action: IAction, currentState: UserState): UserState {
        return when (action) {
            is UserAction.Login -> currentState.copy(
                username = action.username,
                isLoggedIn = true
            )
            is UserAction.Logout -> UserState() // Reset to default
            else -> currentState
        }
    }
}

// Cart domain: Only sees CartState
class CartReducer : IReducer<CartState> {
    override fun reduce(action: IAction, currentState: CartState): CartState {
        return when (action) {
            is CartAction.AddItem -> {
                val newItems = currentState.items + action.item
                currentState.copy(
                    items = newItems,
                    total = newItems.sumOf { it.price }
                )
            }
            is CartAction.RemoveItem -> {
                val newItems = currentState.items.filter { it.id != action.itemId }
                currentState.copy(
                    items = newItems,
                    total = newItems.sumOf { it.price }
                )
            }
            is CartAction.ClearCart -> CartState()
            else -> currentState
        }
    }
}

// Notification domain: Only sees NotificationState
class NotificationReducer : IReducer<NotificationState> {
    override fun reduce(action: IAction, currentState: NotificationState): NotificationState {
        return when (action) {
            is NotificationAction.Show -> currentState.copy(
                messages = currentState.messages + action.message,
                count = currentState.count + 1
            )
            is NotificationAction.Clear -> NotificationState()
            else -> currentState
        }
    }
}
```

**Benefits:**

- Each reducer is small (10-30 lines), focused, and easy to understand
- Type safety: `UserReducer` cannot accidentally modify `CartState`
- Independent testing: Test `UserReducer` with only `UserState` fixtures
- No merge conflicts: Teams work on separate reducer files
- Cleaner `.copy()` syntax without deep nesting

### 3. Pattern 1: Explicit SliceReducer (Verbose)

Wire up domain reducers using explicit `SliceReducer` instances:

```kotlin
class VerboseStateManager(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : StateManager<AppState>(AppState(), scope) {

    override val reducer: IReducer<AppState> = CompositeReducer(
        SliceReducer(
            selector = { state: AppState -> state.user },
            updater = { state, user -> state.copy(user = user) },
            reducer = UserReducer()
        ),
        SliceReducer(
            selector = { state: AppState -> state.cart },
            updater = { state, cart -> state.copy(cart = cart) },
            reducer = CartReducer()
        ),
        SliceReducer(
            selector = { state: AppState -> state.notifications },
            updater = { state, notifications -> state.copy(notifications = notifications) },
            reducer = NotificationReducer()
        )
    )
}
```

**Key Components:**

- `selector`: Extracts the state slice from `AppState` (e.g., `state.user`)
- `updater`: Returns new `AppState` with updated slice (e.g., `state.copy(user = newUser)`)
- `reducer`: Domain-specific reducer handling the slice

**Best for:** Learning the pattern and understanding how `SliceReducer` works internally.

### 4. Pattern 2: sliceReducer() Builder (Concise)

Use the `sliceReducer()` builder function for cleaner syntax:

```kotlin
class ConciseStateManager(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : StateManager<AppState>(AppState(), scope) {

    override val reducer: IReducer<AppState> = CompositeReducer(
        sliceReducer<AppState, UserState>(
            get = { it.user },
            set = { state, user -> state.copy(user = user) },
            reducer = UserReducer()
        ),
        sliceReducer<AppState, CartState>(
            get = { it.cart },
            set = { state, cart -> state.copy(cart = cart) },
            reducer = CartReducer()
        ),
        sliceReducer<AppState, NotificationState>(
            get = { it.notifications },
            set = { state, notifications -> state.copy(notifications = notifications) },
            reducer = NotificationReducer()
        )
    )
}
```

**Improvements over Pattern 1:**

- `get`/`set` naming is more intuitive than `selector`/`updater`
- Type parameters inferred from reducer type
- Less visual noise

**Best for:** Production code where readability and maintainability matter.

### 5. Pattern 3: buildCompositeReducer DSL (Most Ergonomic)

Use the `buildCompositeReducer {}` DSL for maximum ergonomics:

```kotlin
class DSLStateManager(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : StateManager<AppState>(AppState(), scope) {

    override val reducer: IReducer<AppState> = buildCompositeReducer {
        slice<UserState>(
            get = { it.user },
            set = { state, user -> state.copy(user = user) },
            reducer = UserReducer()
        )
        slice<CartState>(
            get = { it.cart },
            set = { state, cart -> state.copy(cart = cart) },
            reducer = CartReducer()
        )
        slice<NotificationState>(
            get = { it.notifications },
            set = { state, notifications -> state.copy(notifications = notifications) },
            reducer = NotificationReducer()
        )
    }
}
```

**DSL Benefits:**

- Most concise and readable
- Clear visual hierarchy with indented `slice()` calls
- Easy to add/remove slices
- Natural Kotlin idiom

**Best for:** Complex apps with many slices or when mixing slice reducers with standalone reducers.

### 6. Understanding CompositeReducer Execution

`CompositeReducer` applies reducers sequentially using a fold operation:

```kotlin
// Conceptual implementation
class CompositeReducer<S>(
    private vararg val reducers: IReducer<S>
) : IReducer<S> {
    override fun reduce(action: IAction, currentState: S): S {
        return reducers.fold(currentState) { state, reducer ->
            reducer.reduce(action, state)
        }
    }
}
```

**Execution Flow:**

1. Start with `currentState`
2. Pass `currentState` to `reducers[0]`, get `result1`
3. Pass `result1` to `reducers[1]`, get `result2`
4. Pass `result2` to `reducers[2]`, get `result3`
5. Return `result3` as final state

**Why Order Matters:**

```kotlin
// Example: UserAction.Login might trigger notification
buildCompositeReducer {
    slice<UserState>(/* ... */, UserReducer())
    slice<NotificationState>(/* ... */, NotificationReducer())
}

// When Login is dispatched:
// 1. UserReducer runs: Sets user.isLoggedIn = true
// 2. NotificationReducer runs: Could react to isLoggedIn flag if needed
```

In practice, order rarely matters for slice reducers since each handles distinct action types. However, if you have
cross-cutting reducers (e.g., a logging reducer that reacts to all actions), place them last.

### 7. Testing Composed Reducers

**Test Individual Domain Reducers:**

```kotlin
@Test
fun `UserReducer handles login action`() {
    val reducer = UserReducer()
    val initialState = UserState()

    val result = reducer.reduce(
        UserAction.Login("alice"),
        initialState
    )

    assertEquals("alice", result.username)
    assertTrue(result.isLoggedIn)
}

@Test
fun `CartReducer calculates total correctly`() {
    val reducer = CartReducer()
    val initialState = CartState()

    val result = reducer.reduce(
        CartAction.AddItem(CartItem("1", "Laptop", 999.99)),
        initialState
    )

    assertEquals(1, result.items.size)
    assertEquals(999.99, result.total, 0.01)
}
```

**Test Composed Reducer Integration:**

```kotlin
@Test
fun `Composed reducer handles multiple domain actions`() {
    val composedReducer = buildCompositeReducer<AppState> {
        slice<UserState>(
            get = { it.user },
            set = { state, user -> state.copy(user = user) },
            reducer = UserReducer()
        )
        slice<CartState>(
            get = { it.cart },
            set = { state, cart -> state.copy(cart = cart) },
            reducer = CartReducer()
        )
    }

    var state = AppState()

    // Apply user action
    state = composedReducer.reduce(UserAction.Login("alice"), state)
    assertEquals("alice", state.user.username)

    // Apply cart action
    state = composedReducer.reduce(
        CartAction.AddItem(CartItem("1", "Laptop", 999.99)),
        state
    )
    assertEquals(1, state.cart.items.size)

    // Verify domains don't interfere
    assertEquals("alice", state.user.username) // User state unchanged
}
```

### 8. Migration Guide: From Monolithic to Composed

**Step 1: Identify State Domains**

Analyze your `AppState` and group properties by feature:

```kotlin
// Before
data class AppState(
    val username: String?,
    val isLoggedIn: Boolean,
    val cartItems: List<CartItem>,
    val cartTotal: Double,
    val notifications: List<String>
)

// After: Group into domains
data class AppState(
    val user: UserState,
    val cart: CartState,
    val notifications: NotificationState
)
```

**Step 2: Extract Domain State Classes**

Create focused state classes for each domain:

```kotlin
data class UserState(
    val username: String? = null,
    val isLoggedIn: Boolean = false
)

data class CartState(
    val items: List<CartItem> = emptyList(),
    val total: Double = 0.0
)

data class NotificationState(
    val messages: List<String> = emptyList()
)
```

**Step 3: Split Actions by Domain**

Group related actions into sealed classes:

```kotlin
// Before: All actions mixed together
sealed class Action : IAction {
    data class Login(val username: String) : Action()
    data class AddItem(val item: CartItem) : Action()
    data class ShowNotification(val message: String) : Action()
}

// After: Domain-separated actions
sealed class UserAction : IAction {
    data class Login(val username: String) : UserAction()
    object Logout : UserAction()
}

sealed class CartAction : IAction {
    data class AddItem(val item: CartItem) : CartAction()
    data class RemoveItem(val itemId: String) : CartAction()
}
```

**Step 4: Create Domain Reducers**

Extract each domain's logic into its own reducer:

```kotlin
class UserReducer : IReducer<UserState> {
    override fun reduce(action: IAction, currentState: UserState): UserState {
        return when (action) {
            is UserAction.Login -> currentState.copy(
                username = action.username,
                isLoggedIn = true
            )
            else -> currentState
        }
    }
}

// Repeat for CartReducer, NotificationReducer...
```

**Step 5: Compose with buildCompositeReducer**

Wire up domain reducers in your StateManager:

```kotlin
class AppStateManager(scope: CoroutineScope) : StateManager<AppState>(AppState(), scope) {
    override val reducer: IReducer<AppState> = buildCompositeReducer {
        slice<UserState>(
            get = { it.user },
            set = { state, user -> state.copy(user = user) },
            reducer = UserReducer()
        )
        slice<CartState>(
            get = { it.cart },
            set = { state, cart -> state.copy(cart = cart) },
            reducer = CartReducer()
        )
        slice<NotificationState>(
            get = { it.notifications },
            set = { state, notifications -> state.copy(notifications = notifications) },
            reducer = NotificationReducer()
        )
    }
}
```

**Step 6: Update Tests**

Migrate from full `AppState` fixtures to focused domain tests:

```kotlin
// Before: Required full AppState
@Test
fun `test user login`() {
    val state = AppState(
        username = null,
        isLoggedIn = false,
        cartItems = emptyList(), // Irrelevant to test
        cartTotal = 0.0,          // Irrelevant to test
        notifications = emptyList() // Irrelevant to test
    )
    // ...
}

// After: Only UserState needed
@Test
fun `test user login`() {
    val state = UserState()
    val result = UserReducer().reduce(UserAction.Login("alice"), state)
    assertTrue(result.isLoggedIn)
}
```

## Tips

- **Start explicit, refactor to DSL later**: Begin with Pattern 1 (explicit `SliceReducer`) to understand the mechanism,
  then refactor to Pattern 3 (DSL) for ergonomics
- **Each reducer handles only its domain actions**: A domain reducer should return `currentState` unchanged for actions
  outside its domain (the `else -> currentState` pattern)
- **Test individually before composing**: Write unit tests for each domain reducer before integration testing the
  composed reducer
- **Prefer many small reducers over few large ones**: If a domain reducer exceeds 50 lines, consider splitting it
  further (e.g., `CartReducer` → `CartItemsReducer` + `CartTotalReducer`)
- **Use descriptive state slice names**: `state.user` is clearer than `state.u`; avoid abbreviations
- **Avoid cross-domain dependencies**: If `CartReducer` needs to know `user.isLoggedIn`, consider middleware or
  restructure your domains
- **Order doesn't matter for independent slices**: Place slices in logical order (e.g., user, cart, notifications) for
  readability, not for functional reasons
- **CompositeReducer works with any IReducer**: You can mix `SliceReducer` with standalone reducers that operate on the
  full `AppState`

## Next Steps

- **Subscribing to state slices**: Learn how to observe specific state slices in the UI (
  see [SELECTORS.md](./SELECTORS.md))
- **Working code example**: Review the full implementation in [
  `ShoppingCartExample.kt`](../../app/src/main/java/dev/amaro/sonic/app/samples/multireducer/ShoppingCartExample.kt)
- **Middleware with composed reducers**: Understand how middleware interacts with composed state (
  see [MIDDLEWARE_AGENTS.md](./MIDDLEWARE_AGENTS.md))
- **Advanced patterns**: Explore conditional composition, dynamic reducer registration, and cross-cutting concerns in
  complex applications
