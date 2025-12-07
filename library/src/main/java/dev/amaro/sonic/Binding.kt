package dev.amaro.sonic

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Binds a StateManager's state emissions to a renderer using a provided coroutine scope.
 *
 * This is the core primitive for connecting state management to UI across all platforms.
 * The binding respects the provided scope's lifetime - when the scope is cancelled,
 * the binding automatically stops collecting.
 *
 * ## Platforms
 *
 * **Android (XML/Compose)**: Use [lifecycleScope][androidx.lifecycle.lifecycleScope] for lifecycle-bound collection.
 * **Desktop (Compose)**: Create explicit [CoroutineScope] and override [dispatcher] to [Dispatchers.Default].
 * **CLI/Headless**: Use [runBlocking] with [Dispatchers.Default].
 *
 * ## Dispatcher Selection
 *
 * The default dispatcher is [Dispatchers.Main.immediate], which is appropriate for Android.
 *
 * For non-Android platforms, override the dispatcher:
 * - **Desktop**: Use [Dispatchers.Default]
 * - **CLI**: Use [Dispatchers.Default] or [Dispatchers.IO]
 *
 * Using [Dispatchers.Main] on non-Android platforms will crash at runtime.
 *
 * ## Examples
 *
 * **Android XML Fragment**:
 * ```kotlin
 * class NotesFragment : Fragment(), IRenderer<NoteState> {
 *     private val manager: NoteStateManager by inject()
 *
 *     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *         super.onViewCreated(view, savedInstanceState)
 *         bindState(manager, this, viewLifecycleOwner.lifecycleScope)
 *     }
 *
 *     override fun render(state: NoteState, performer: IPerformer<NoteState>) {
 *         // Update UI imperatively
 *     }
 * }
 * ```
 *
 * **Android Jetpack Compose**:
 * ```kotlin
 * @Composable
 * fun NotesScreen(appGraph: AppGraph = LocalAppGraph.current) {
 *     val state by appGraph.manager.listen().collectAsState()
 *     MyContent(state, appGraph.manager::perform)
 * }
 * ```
 *
 * **Desktop Compose**:
 * ```kotlin
 * fun main() {
 *     val scope = CoroutineScope(Dispatchers.Default + Job())
 *     val manager = AppStateManager(...)
 *     bindState(
 *         manager,
 *         DesktopRenderer(),
 *         scope,
 *         Dispatchers.Default  // Critical: override default
 *     )
 * }
 * ```
 *
 * **CLI/Headless**:
 * ```kotlin
 * fun main() = runBlocking {
 *     val manager = AppStateManager(...)
 *     bindState(
 *         manager,
 *         ConsoleRenderer(),
 *         this,  // runBlocking provides scope
 *         Dispatchers.Default  // Critical: never use Main
 *     )
 * }
 * ```
 *
 * @param T The state type
 * @param manager The [IStateManager] providing state emissions and action handling
 * @param renderer The [IRenderer] receiving state updates
 * @param scope The [CoroutineScope] that controls the binding's lifetime.
 *              Scope cancellation automatically stops rendering.
 * @param dispatcher The [CoroutineDispatcher] for rendering.
 *                   Defaults to [Dispatchers.Main.immediate] for Android compatibility.
 *                   Override to [Dispatchers.Default] for non-Android platforms (CLI, Desktop).
 * @return A [Job] representing the binding. Can be cancelled explicitly, but scope cancellation is preferred.
 *
 * @see IStateManager
 * @see IRenderer
 */
fun <T> bindState(
    manager: IStateManager<T>,
    renderer: IRenderer<T>,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
): Job {
    return scope.launch(dispatcher) {
        manager.listen().collect { state ->
            renderer.render(state, object : IPerformer<T> {
                override fun perform(action: IAction) {
                    manager.perform(action)
                }
            })
        }
    }
}
