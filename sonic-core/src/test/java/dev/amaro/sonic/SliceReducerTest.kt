package dev.amaro.sonic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SliceReducerTest {

    // Multi-domain state structure
    data class AppState(
        val user: UserState = UserState(),
        val cart: CartState = CartState(),
        val notifications: NotificationState = NotificationState()
    )

    data class UserState(
        val username: String = "",
        val isLoggedIn: Boolean = false
    )

    data class CartState(
        val items: List<String> = emptyList(),
        val total: Double = 0.0
    )

    data class NotificationState(
        val count: Int = 0,
        val messages: List<String> = emptyList()
    )

    // Actions
    sealed class UserAction : IAction {
        data class Login(val username: String) : UserAction()
        object Logout : UserAction()
    }

    sealed class CartAction : IAction {
        data class AddItem(val item: String, val price: Double) : CartAction()
        object ClearCart : CartAction()
    }

    sealed class NotificationAction : IAction {
        data class AddNotification(val message: String) : NotificationAction()
        object ClearNotifications : NotificationAction()
    }

    // Slice reducers
    class UserReducer : IReducer<UserState> {
        override fun reduce(action: IAction, currentState: UserState): UserState {
            return when (action) {
                is UserAction.Login -> currentState.copy(
                    username = action.username,
                    isLoggedIn = true
                )

                is UserAction.Logout -> currentState.copy(
                    username = "",
                    isLoggedIn = false
                )

                else -> currentState
            }
        }
    }

    class CartReducer : IReducer<CartState> {
        override fun reduce(action: IAction, currentState: CartState): CartState {
            return when (action) {
                is CartAction.AddItem -> currentState.copy(
                    items = currentState.items + action.item,
                    total = currentState.total + action.price
                )

                is CartAction.ClearCart -> currentState.copy(
                    items = emptyList(),
                    total = 0.0
                )

                else -> currentState
            }
        }
    }

    class NotificationReducer : IReducer<NotificationState> {
        override fun reduce(action: IAction, currentState: NotificationState): NotificationState {
            return when (action) {
                is NotificationAction.AddNotification -> currentState.copy(
                    count = currentState.count + 1,
                    messages = currentState.messages + action.message
                )

                is NotificationAction.ClearNotifications -> currentState.copy(
                    count = 0,
                    messages = emptyList()
                )

                else -> currentState
            }
        }
    }

    @Test
    fun `slice reducer modifies only its slice`() {
        val userSliceReducer = SliceReducer(
            selector = { state: AppState -> state.user },
            updater = { state, user -> state.copy(user = user) },
            reducer = UserReducer()
        )

        val initialState = AppState(
            user = UserState(),
            cart = CartState(items = listOf("existing-item"), total = 10.0),
            notifications = NotificationState(count = 5)
        )

        val result = userSliceReducer.reduce(UserAction.Login("alice"), initialState)

        // User slice modified
        assertEquals("alice", result.user.username)
        assertEquals(true, result.user.isLoggedIn)

        // Other slices unchanged
        assertEquals(listOf("existing-item"), result.cart.items)
        assertEquals(10.0, result.cart.total, 0.001)
        assertEquals(5, result.notifications.count)
    }

    @Test
    fun `slice reducer with composite reducer handles multiple domains`() {
        val reducer = CompositeReducer(
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

        var state = AppState()
        state = reducer.reduce(UserAction.Login("bob"), state)
        state = reducer.reduce(CartAction.AddItem("laptop", 999.99), state)
        state = reducer.reduce(NotificationAction.AddNotification("Welcome!"), state)

        assertEquals("bob", state.user.username)
        assertEquals(true, state.user.isLoggedIn)
        assertEquals(listOf("laptop"), state.cart.items)
        assertEquals(999.99, state.cart.total, 0.001)
        assertEquals(1, state.notifications.count)
        assertEquals(listOf("Welcome!"), state.notifications.messages)
    }

    @Test
    fun `returns original state when slice unchanged (reference equality)`() {
        val userSliceReducer = SliceReducer(
            selector = { state: AppState -> state.user },
            updater = { state, user -> state.copy(user = user) },
            reducer = UserReducer()
        )

        val initialState = AppState(user = UserState(username = "charlie", isLoggedIn = true))

        // Action not handled by UserReducer
        val result = userSliceReducer.reduce(CartAction.AddItem("book", 15.0), initialState)

        // Should return original state instance (no copy performed)
        assertSame(initialState, result)
    }

    @Test
    fun `slice reducer handles actions targeting different slices`() {
        val cartSliceReducer = SliceReducer(
            selector = { state: AppState -> state.cart },
            updater = { state, cart -> state.copy(cart = cart) },
            reducer = CartReducer()
        )

        var state = AppState()
        state = cartSliceReducer.reduce(CartAction.AddItem("phone", 799.99), state)
        state = cartSliceReducer.reduce(CartAction.AddItem("case", 29.99), state)

        assertEquals(listOf("phone", "case"), state.cart.items)
        assertEquals(829.98, state.cart.total, 0.01)
    }

    data class NestedState(
        val outer: OuterState = OuterState()
    )

    data class OuterState(
        val inner: InnerState = InnerState()
    )

    data class InnerState(
        val value: Int = 0
    )

    sealed class InnerAction : IAction {
        object Increment : InnerAction()
    }

    class InnerReducer : IReducer<InnerState> {
        override fun reduce(action: IAction, currentState: InnerState): InnerState {
            return when (action) {
                is InnerAction.Increment -> currentState.copy(value = currentState.value + 1)
                else -> currentState
            }
        }
    }

    @Test
    fun `slice reducer with nested state updates correctly`() {
        // Slice reducer targeting nested inner state
        val innerSliceReducer = SliceReducer(
            selector = { state: NestedState -> state.outer.inner },
            updater = { state, inner ->
                state.copy(outer = state.outer.copy(inner = inner))
            },
            reducer = InnerReducer()
        )

        val initialState = NestedState()
        val result = innerSliceReducer.reduce(InnerAction.Increment, initialState)

        assertEquals(1, result.outer.inner.value)
    }

    // Shared action processed by multiple slices
    object ResetAction : IAction

    class ResetUserReducer : IReducer<UserState> {
        override fun reduce(action: IAction, currentState: UserState): UserState {
            return when (action) {
                is ResetAction -> UserState()
                else -> currentState
            }
        }
    }

    class ResetCartReducer : IReducer<CartState> {
        override fun reduce(action: IAction, currentState: CartState): CartState {
            return when (action) {
                is ResetAction -> CartState()
                else -> currentState
            }
        }
    }

    @Test
    fun `multiple slice reducers process same action independently`() {

        val reducer = CompositeReducer(
            SliceReducer(
                selector = { state: AppState -> state.user },
                updater = { state, user -> state.copy(user = user) },
                reducer = ResetUserReducer()
            ),
            SliceReducer(
                selector = { state: AppState -> state.cart },
                updater = { state, cart -> state.copy(cart = cart) },
                reducer = ResetCartReducer()
            )
        )

        val initialState = AppState(
            user = UserState(username = "david", isLoggedIn = true),
            cart = CartState(items = listOf("item1", "item2"), total = 50.0),
            notifications = NotificationState(count = 10)
        )

        val result = reducer.reduce(ResetAction, initialState)

        // User and cart reset
        assertEquals("", result.user.username)
        assertEquals(false, result.user.isLoggedIn)
        assertEquals(emptyList<String>(), result.cart.items)
        assertEquals(0.0, result.cart.total, 0.001)

        // Notifications unchanged (no reducer for this slice)
        assertEquals(10, result.notifications.count)
    }

    @Test
    fun `selector extracts correct slice`() {
        val selector: (AppState) -> UserState = { it.user }
        val state = AppState(user = UserState(username = "eve", isLoggedIn = true))

        val slice = selector(state)

        assertEquals("eve", slice.username)
        assertEquals(true, slice.isLoggedIn)
    }

    @Test
    fun `updater merges slice back correctly`() {
        val updater: (AppState, UserState) -> AppState = { state, user -> state.copy(user = user) }
        val state = AppState(
            user = UserState(username = "old", isLoggedIn = false),
            cart = CartState(items = listOf("item"), total = 10.0)
        )
        val newUserState = UserState(username = "new", isLoggedIn = true)

        val result = updater(state, newUserState)

        assertEquals("new", result.user.username)
        assertEquals(true, result.user.isLoggedIn)
        assertEquals(listOf("item"), result.cart.items) // Cart unchanged
    }

    // Global reducer that operates on full state
    class GlobalReducer : IReducer<AppState> {
        override fun reduce(action: IAction, currentState: AppState): AppState {
            return when (action) {
                is GlobalAction.ResetAll -> AppState()
                else -> currentState
            }
        }
    }

    sealed class GlobalAction : IAction {
        object ResetAll : GlobalAction()
    }

    @Test
    fun `slice reducer is composable with other reducers`() {

        val reducer = CompositeReducer(
            GlobalReducer(),
            SliceReducer(
                selector = { state: AppState -> state.user },
                updater = { state, user -> state.copy(user = user) },
                reducer = UserReducer()
            )
        )

        var state = AppState(
            user = UserState(username = "frank", isLoggedIn = true)
        )
        state = reducer.reduce(GlobalAction.ResetAll, state)

        // Global reducer reset entire state
        assertEquals("", state.user.username)
        assertEquals(false, state.user.isLoggedIn)
    }
}
