package dev.amaro.sonic

/**
 * A clean data structure pairing a state value with an action performer.
 *
 * This is useful for Compose where state and callbacks are passed together,
 * and for ViewModel patterns where the state value is separated from the performer.
 *
 * StateBinding provides a clear, simple interface for transporting state and action
 * dispatch methods together. It is immutable and works well with reactive patterns.
 *
 * ## Example (Compose)
 * ```kotlin
 * @Composable
 * fun MyScreen(appGraph: AppGraph = LocalAppGraph.current) {
 *     val binding = rememberStateBinding(appGraph.manager)  // Phase 2 helper
 *
 *     Text(binding.value.title)
 *     Button(onClick = { binding.perform(Action.Clicked) })
 * }
 * ```
 *
 * ## Example (ViewModel)
 * ```kotlin
 * class MyViewModel(private val manager: MyStateManager) : ViewModel() {
 *     val state = manager.listen()
 *
 *     fun perform(action: IAction) = manager.perform(action)
 *
 *     // Then in Compose:
 *     // val binding = StateBinding(viewModel.state.value, viewModel::perform)
 * }
 * ```
 *
 * @param T The state type
 * @property value The current state value
 * @property perform A function to perform actions. Typically manager.perform()
 */
data class StateBinding<T>(
    val value: T,
    val perform: (IAction) -> Unit
)

/**
 * Backward compatibility alias for StateBinding.
 *
 * Use [StateBinding] in new code. This alias is maintained for existing code
 * that may have adopted the older name.
 *
 * @deprecated Use StateBinding instead. Cleaner semantics.
 */
@Deprecated(
    message = "Use StateBinding instead. Cleaner semantics.",
    replaceWith = ReplaceWith("StateBinding"),
    level = DeprecationLevel.WARNING
)
typealias SonicUiState<T> = StateBinding<T>
