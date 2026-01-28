package dev.amaro.sonic.app.samples.multireducer

import dev.amaro.sonic.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Sample demonstrating multi-reducer patterns in Sonic.
 *
 * This example shows a shopping cart application with three distinct domains:
 * - User Authentication (login/logout)
 * - Shopping Cart (add/remove items)
 * - Notifications (display messages)
 *
 * The implementation uses:
 * 1. SliceReducer for type-safe domain separation
 * 2. CompositeReducer to combine all domain reducers
 * 3. Builder DSL for ergonomic composition
 */
object ShoppingCartExample {

    // ========== State Structure ==========

    /**
     * Application state split into clear domain slices.
     * Each slice is independent and managed by its own reducer.
     */
    data class AppState(
        val user: UserState = UserState(),
        val cart: CartState = CartState(),
        val notifications: NotificationState = NotificationState()
    )

    data class UserState(
        val username: String? = null,
        val isLoggedIn: Boolean = false
    )

    data class CartState(
        val items: List<CartItem> = emptyList(),
        val total: Double = 0.0
    )

    data class NotificationState(
        val messages: List<String> = emptyList(),
        val count: Int = 0
    )

    data class CartItem(
        val id: String,
        val name: String,
        val price: Double
    )

    // ========== Actions ==========

    sealed class UserAction : IAction {
        data class Login(val username: String) : UserAction()
        object Logout : UserAction()
    }

    sealed class CartAction : IAction {
        data class AddItem(val item: CartItem) : CartAction()
        data class RemoveItem(val itemId: String) : CartAction()
        object ClearCart : CartAction()
    }

    sealed class NotificationAction : IAction {
        data class Show(val message: String) : NotificationAction()
        object Clear : NotificationAction()
    }

    // ========== Domain Reducers ==========

    /**
     * Reducer handling only user authentication state.
     * Type-safe: Can only access UserState, not CartState or NotificationState.
     */
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

    /**
     * Reducer handling only shopping cart state.
     * Type-safe: Can only access CartState.
     */
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

    /**
     * Reducer handling only notification state.
     * Type-safe: Can only access NotificationState.
     */
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

    // ========== StateManager Implementations ==========

    /**
     * Example 1: Using CompositeReducer with SliceReducer (Verbose)
     *
     * This approach explicitly creates SliceReducer instances for each domain.
     * Best for: Maximum clarity and understanding of the pattern.
     */
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

    /**
     * Example 2: Using sliceReducer builder (Concise)
     *
     * This approach uses the sliceReducer() builder function for less boilerplate.
     * Best for: Production code where readability is important.
     */
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

    /**
     * Example 3: Using DSL builder (Most Ergonomic)
     *
     * This approach uses the buildCompositeReducer {} DSL for maximum ergonomics.
     * Best for: Complex apps with many slices or mixed reducer types.
     */
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

    // ========== Example Usage ==========

    /**
     * Demonstrates basic usage of the multi-reducer StateManager.
     */
    fun demonstrateUsage() {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val manager = ConciseStateManager(scope)

        // User logs in
        manager.perform(UserAction.Login("alice"))

        // Add items to cart
        manager.perform(
            CartAction.AddItem(
                CartItem("1", "Laptop", 999.99)
            )
        )
        manager.perform(
            CartAction.AddItem(
                CartItem("2", "Mouse", 29.99)
            )
        )

        // Show notification
        manager.perform(NotificationAction.Show("Items added to cart"))

        // Current state contains all updates across domains:
        // - user.username = "alice", user.isLoggedIn = true
        // - cart.items = [Laptop, Mouse], cart.total = 1029.98
        // - notifications.count = 1, notifications.messages = ["Items added to cart"]
    }

    // ========== Comparison: Before vs After ==========

    /**
     * Before: Monolithic reducer handling all domains (Anti-pattern for large apps)
     */
    class MonolithicReducer : IReducer<AppState> {
        override fun reduce(action: IAction, currentState: AppState): AppState {
            return when (action) {
                // User actions
                is UserAction.Login -> currentState.copy(
                    user = currentState.user.copy(
                        username = action.username,
                        isLoggedIn = true
                    )
                )

                is UserAction.Logout -> currentState.copy(user = UserState())

                // Cart actions
                is CartAction.AddItem -> {
                    val newItems = currentState.cart.items + action.item
                    currentState.copy(
                        cart = currentState.cart.copy(
                            items = newItems,
                            total = newItems.sumOf { it.price }
                        )
                    )
                }

                is CartAction.RemoveItem -> {
                    val newItems = currentState.cart.items.filter { it.id != action.itemId }
                    currentState.copy(
                        cart = currentState.cart.copy(
                            items = newItems,
                            total = newItems.sumOf { it.price }
                        )
                    )
                }

                is CartAction.ClearCart -> currentState.copy(cart = CartState())

                // Notification actions
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
}
