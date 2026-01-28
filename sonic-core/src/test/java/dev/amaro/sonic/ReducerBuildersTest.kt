package dev.amaro.sonic

import org.junit.Assert.assertEquals
import org.junit.Test

class ReducerBuildersTest {

    // Test state and actions
    data class AppState(
        val user: UserState = UserState(),
        val settings: SettingsState = SettingsState()
    )

    data class UserState(
        val name: String = "",
        val age: Int = 0
    )

    data class SettingsState(
        val theme: String = "light",
        val notifications: Boolean = true
    )

    sealed class UserAction : IAction {
        data class SetName(val name: String) : UserAction()
        data class SetAge(val age: Int) : UserAction()
    }

    sealed class SettingsAction : IAction {
        data class SetTheme(val theme: String) : SettingsAction()
        object ToggleNotifications : SettingsAction()
    }

    // Reducers
    class UserReducer : IReducer<UserState> {
        override fun reduce(action: IAction, currentState: UserState): UserState {
            return when (action) {
                is UserAction.SetName -> currentState.copy(name = action.name)
                is UserAction.SetAge -> currentState.copy(age = action.age)
                else -> currentState
            }
        }
    }

    class SettingsReducer : IReducer<SettingsState> {
        override fun reduce(action: IAction, currentState: SettingsState): SettingsState {
            return when (action) {
                is SettingsAction.SetTheme -> currentState.copy(theme = action.theme)
                is SettingsAction.ToggleNotifications -> currentState.copy(
                    notifications = !currentState.notifications
                )

                else -> currentState
            }
        }
    }

    @Test
    fun `sliceReducer builder creates working slice reducer`() {
        val reducer = sliceReducer(
            get = { state: AppState -> state.user },
            set = { state, user -> state.copy(user = user) },
            reducer = UserReducer()
        )

        val initialState = AppState()
        val result = reducer.reduce(UserAction.SetName("Alice"), initialState)

        assertEquals("Alice", result.user.name)
    }

    @Test
    fun `sliceReducer builder with multiple reducers via composite`() {
        val reducer = CompositeReducer(
            sliceReducer(
                get = { state: AppState -> state.user },
                set = { state, user -> state.copy(user = user) },
                reducer = UserReducer()
            ),
            sliceReducer(
                get = { state: AppState -> state.settings },
                set = { state, settings -> state.copy(settings = settings) },
                reducer = SettingsReducer()
            )
        )

        var state = AppState()
        state = reducer.reduce(UserAction.SetName("Bob"), state)
        state = reducer.reduce(UserAction.SetAge(30), state)
        state = reducer.reduce(SettingsAction.SetTheme("dark"), state)
        state = reducer.reduce(SettingsAction.ToggleNotifications, state)

        assertEquals("Bob", state.user.name)
        assertEquals(30, state.user.age)
        assertEquals("dark", state.settings.theme)
        assertEquals(false, state.settings.notifications)
    }

    @Test
    fun `buildCompositeReducer DSL creates composite with slice reducers`() {
        val reducer = buildCompositeReducer<AppState> {
            slice(
                get = { it.user },
                set = { state, user -> state.copy(user = user) },
                reducer = UserReducer()
            )
            slice(
                get = { it.settings },
                set = { state, settings -> state.copy(settings = settings) },
                reducer = SettingsReducer()
            )
        }

        var state = AppState()
        state = reducer.reduce(UserAction.SetName("Charlie"), state)
        state = reducer.reduce(SettingsAction.SetTheme("blue"), state)

        assertEquals("Charlie", state.user.name)
        assertEquals("blue", state.settings.theme)
    }

    // Global reducer that operates on full AppState
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
    fun `buildCompositeReducer DSL can mix regular and slice reducers`() {

        val reducer = buildCompositeReducer<AppState> {
            // Add global reducer
            add(GlobalReducer())

            // Add slice reducers
            slice(
                get = { it.user },
                set = { state, user -> state.copy(user = user) },
                reducer = UserReducer()
            )
            slice(
                get = { it.settings },
                set = { state, settings -> state.copy(settings = settings) },
                reducer = SettingsReducer()
            )
        }

        var state = AppState()
        state = reducer.reduce(UserAction.SetName("Diana"), state)
        state = reducer.reduce(SettingsAction.SetTheme("green"), state)

        assertEquals("Diana", state.user.name)
        assertEquals("green", state.settings.theme)

        // Reset all
        state = reducer.reduce(GlobalAction.ResetAll, state)

        assertEquals("", state.user.name)
        assertEquals(0, state.user.age)
        assertEquals("light", state.settings.theme)
        assertEquals(true, state.settings.notifications)
    }

    @Test
    fun `buildCompositeReducer with only slice reducers`() {
        val reducer = buildCompositeReducer<AppState> {
            slice(
                get = { it.user },
                set = { state, user -> state.copy(user = user) },
                reducer = UserReducer()
            )
        }

        val initialState = AppState()
        val result = reducer.reduce(UserAction.SetAge(25), initialState)

        assertEquals(25, result.user.age)
    }

    @Test
    fun `buildCompositeReducer with no reducers returns unchanged state`() {
        val reducer = buildCompositeReducer<AppState> {
            // Empty builder
        }

        val initialState = AppState(
            user = UserState(name = "Eve", age = 40)
        )
        val result = reducer.reduce(UserAction.SetName("Frank"), initialState)

        // No reducers, so state unchanged
        assertEquals("Eve", result.user.name)
        assertEquals(40, result.user.age)
    }

    @Test
    fun `buildCompositeReducer DSL order matters`() {
        class DoubleAgeReducer : IReducer<UserState> {
            override fun reduce(action: IAction, currentState: UserState): UserState {
                return when (action) {
                    is UserAction.SetAge -> currentState.copy(age = action.age * 2)
                    else -> currentState
                }
            }
        }

        // First UserReducer, then DoubleAgeReducer
        val reducer1 = buildCompositeReducer<AppState> {
            slice(
                get = { it.user },
                set = { state, user -> state.copy(user = user) },
                reducer = UserReducer()
            )
            slice(
                get = { it.user },
                set = { state, user -> state.copy(user = user) },
                reducer = DoubleAgeReducer()
            )
        }

        // First DoubleAgeReducer, then UserReducer
        val reducer2 = buildCompositeReducer<AppState> {
            slice(
                get = { it.user },
                set = { state, user -> state.copy(user = user) },
                reducer = DoubleAgeReducer()
            )
            slice(
                get = { it.user },
                set = { state, user -> state.copy(user = user) },
                reducer = UserReducer()
            )
        }

        val initialState = AppState()
        val result1 = reducer1.reduce(UserAction.SetAge(10), initialState)
        val result2 = reducer2.reduce(UserAction.SetAge(10), initialState)

        // reducer1: UserReducer sets age to 10, DoubleAgeReducer doubles it to 20
        assertEquals(20, result1.user.age)

        // reducer2: DoubleAgeReducer doubles to 20, UserReducer sets to 10 (overrides)
        assertEquals(10, result2.user.age)
    }

    @Test
    fun `buildCompositeReducer handles multiple regular reducers`() {
        class Reducer1 : IReducer<AppState> {
            override fun reduce(action: IAction, currentState: AppState): AppState {
                return when (action) {
                    is UserAction.SetName -> currentState.copy(
                        user = currentState.user.copy(name = action.name)
                    )

                    else -> currentState
                }
            }
        }

        class Reducer2 : IReducer<AppState> {
            override fun reduce(action: IAction, currentState: AppState): AppState {
                return when (action) {
                    is SettingsAction.SetTheme -> currentState.copy(
                        settings = currentState.settings.copy(theme = action.theme)
                    )

                    else -> currentState
                }
            }
        }

        val reducer = buildCompositeReducer<AppState> {
            add(Reducer1())
            add(Reducer2())
        }

        var state = AppState()
        state = reducer.reduce(UserAction.SetName("Grace"), state)
        state = reducer.reduce(SettingsAction.SetTheme("purple"), state)

        assertEquals("Grace", state.user.name)
        assertEquals("purple", state.settings.theme)
    }

    // Test with deep nesting
    data class Root(val container: Container = Container())
    data class Container(val leaf: Leaf = Leaf())
    data class Leaf(val value: Int = 0)

    sealed class LeafAction : IAction {
        object Increment : LeafAction()
    }

    class LeafReducer : IReducer<Leaf> {
        override fun reduce(action: IAction, currentState: Leaf): Leaf {
            return when (action) {
                is LeafAction.Increment -> currentState.copy(value = currentState.value + 1)
                else -> currentState
            }
        }
    }

    @Test
    fun `sliceReducer with complex selector and updater`() {

        val reducer = sliceReducer(
            get = { state: Root -> state.container.leaf },
            set = { state, leaf -> state.copy(container = state.container.copy(leaf = leaf)) },
            reducer = LeafReducer()
        )

        val initialState = Root()
        val result = reducer.reduce(LeafAction.Increment, initialState)

        assertEquals(1, result.container.leaf.value)
    }
}
