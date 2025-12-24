package dev.amaro.sonic.compose

import androidx.compose.runtime.compositionLocalOf
import dev.amaro.sonic.IStateManager

/**
 * Wraps a StateManager for use in CompositionLocal.
 * Allows child composables to access the state manager without prop drilling.
 *
 * @param T The state type managed by the StateManager
 * @property manager The IStateManager instance to provide to descendants
 */
data class StateManagerContext<T>(
    val manager: IStateManager<T>
)

/**
 * CompositionLocal to provide StateManager to all descendant composables.
 *
 * Provides a way for Compose components to access a StateManager without
 * passing it through multiple levels of composable parameters.
 *
 * Usage in screen:
 * ```kotlin
 * @Composable
 * fun MyScreen(manager: MyStateManager) {
 *     CompositionLocalProvider(LocalStateManager provides StateManagerContext(manager)) {
 *         // All children can access manager via LocalStateManager.current
 *         ChildComponent()
 *         AnotherComponent()
 *     }
 * }
 * ```
 *
 * Usage in child composable:
 * ```kotlin
 * @Composable
 * fun ChildComponent() {
 *     val context = LocalStateManager.current as? StateManagerContext<MyState> ?: return
 *
 *     // Component independently selects the data it needs
 *     val myData by context.manager.listen()
 *         .selectDistinct { it.someField }
 *         .collectAsState()
 * }
 * ```
 *
 * Benefits:
 * - No prop drilling through multiple levels of components
 * - Each component independently selects what data it needs
 * - Only recomposes when the selected data changes
 * - Idiomatic Compose pattern
 */
val LocalStateManager = compositionLocalOf<StateManagerContext<*>?> { null }
