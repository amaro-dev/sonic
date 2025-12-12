# Binding Sonic to UI

## Overview

`bindState()` connects any `IStateManager<T>` to an `IRenderer<T>` within a coroutine scope you control. This document
explains how to apply the helper across Android Views, Jetpack Compose, Desktop Compose, and CLI environments, and when
to use `StateBinding`.

## Key Points

- `bindState` lives in `Binding.kt` and requires a manager, renderer, scope, and optional dispatcher.
- `StateBinding` (from `StateBinding.kt`) wraps value + performer for Compose previews or DI wiring.
- You own the lifecycle: cancelling the provided scope (or the returned job) stops rendering.

## Prerequisites

- Read `docs/CONCEPTS.md` for `IStateManager`, `IRenderer`, and `IPerformer`.
- A coroutine scope that reflects your UI’s lifetime (e.g., `viewLifecycleOwner.lifecycleScope`).

## Workflow

1. **Android Fragment / Activity**
   ```kotlin
   class NoteListFragment : Fragment(), IRenderer<NoteState> {
       private val manager by inject<NoteStateManager>()

       override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
           bindState(manager, this, viewLifecycleOwner.lifecycleScope)
       }

       override fun render(state: NoteState, performer: IPerformer<NoteState>) {
           list.adapter = NoteAdapter(state.notes)
           fab.setOnClickListener { performer.perform(Action.NewNote) }
       }
   }
   ```
    - Dispatcher defaults to `Dispatchers.Main.immediate`, which is correct for Android UI.

2. **Jetpack Compose**
   ```kotlin
   @Composable
   fun NotesScreen(manager: NoteStateManager) {
       val state by manager.listen().collectAsState()
       LazyColumn { items(state.notes) { Text(it.title) } }
   }

   @Composable
   fun rememberStateBinding(manager: NoteStateManager): StateBinding<NoteState> {
       val state by manager.listen().collectAsState()
       return StateBinding(state, manager::perform)
   }
   ```
    - Use `StateBinding` when you need to pass state + performer as a single object.

3. **Desktop Compose**
   ```kotlin
   val desktopScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
   val job = bindState(
       manager = desktopManager,
       renderer = DesktopRenderer(),
       scope = desktopScope,
       dispatcher = Dispatchers.Default // No Main on desktop
   )
   ```
    - Cancel `desktopScope` in your window’s `onCloseRequest`.

4. **CLI / Headless**
   ```kotlin
   fun main() = runBlocking {
       val manager = CounterManager(this)
       bindState(manager, ConsoleRenderer(), this, Dispatchers.Default)
       awaitCancellation()
   }
   ```

## Tips

- Reuse scopes rather than spawning new ones per binding.
- Always pair `bindState` with the renderer’s lifecycle callbacks (e.g., fragments: `onViewCreated` / `onDestroyView`).
- When multiple renderers observe the same manager (e.g., Compose + XML), each gets its own binding call.

## Next Steps

- Explore `docs/features/RESULT_HANDLING.md` to display result banners alongside your bound UI.
- For Compose-heavy screens, pair binding with selectors (`docs/features/SELECTORS.md`).
