# State Selectors & CompositionLocal

## Overview

Selectors allow Compose components (or any Flow consumer) to subscribe to derived slices of state so only the relevant
UI recomposes. Sonic provides `selectDistinct` in `StateSelectors.kt` and a CompositionLocal helper (
`StateManagerContext`, `LocalStateManager`) in the app module.

## Key Points

- `selectDistinct` works on both `StateFlow<T>` and `Flow<T>` to prevent duplicate emissions when derived values haven’t
  changed.
- `LocalStateManager` keeps Compose hierarchies free of prop drilling.
- Filtering/transform logic stays in Kotlin code rather than duplicating data inside `State`.

## When to Use

- Use selectors when your state object is large but a component only needs a small slice, or when Compose recomposes too
  often due to whole-state observation.
- Use the CompositionLocal approach when you want deeply nested composables to access the manager without passing it
  through every parameter.

## Prerequisites

- A `StateManager` emitting updates (via `listen()`).
- Familiarity with Compose (for CompositionLocal examples).

## Workflow

1. **Provide the manager once**
   ```kotlin
   @Composable
   fun NoteComposeActivityContent(manager: NoteStateManager) {
       CompositionLocalProvider(LocalStateManager provides StateManagerContext(manager)) {
           NoteComposeRoot(manager)
       }
   }
   ```
   > `StateManagerContext` and `LocalStateManager` are part of the sample app, not the core artifact. Copy the helper
   below into your project (or adapt it) before using this pattern:
   ```kotlin
   data class StateManagerContext<T>(val manager: IStateManager<T>)

   val LocalStateManager = compositionLocalOf<StateManagerContext<*>?> { null }
   ```

2. **Subscribe to derived data**
   ```kotlin
   @Composable
   fun NoteListPanel() {
       val context = LocalStateManager.current as? StateManagerContext<NoteState> ?: return
       val displayedNotes by context.manager.listen()
           .selectDistinct { state ->
               if (state.showOnlyOpen) state.notes.filter { !it.done } else state.notes
           }
           .collectAsState(emptyList())

       LazyColumn { items(displayedNotes) { NoteRow(it) } }
   }
   ```

3. **Compute aggregates once**
   ```kotlin
   data class NoteStats(val total: Int, val completed: Int, val open: Int)

   val stats by context.manager.listen()
       .selectDistinct { state ->
           NoteStats(
               total = state.notes.size,
               completed = state.notes.count { it.done },
               open = state.notes.count { !it.done }
           )
       }
       .collectAsState(NoteStats(0, 0, 0))
   ```

4. **Chain selectors**
   ```kotlin
   val summary by context.manager.listen()
       .selectDistinct { it.notes }
       .selectDistinct { notes -> notes.count { !it.done } }
       .collectAsState(0)
   ```

## Tips

- Always guard `LocalStateManager.current` casts with `as?` to avoid crashes when previews don’t supply a manager.
- Avoid duplicating filtered lists inside `State`; selectors keep the source of truth single.
- Combine with result handling so banners only update when `result` changes.

## Next Steps

- Review `docs/features/BINDING.md` to see how selectors plug into bound renderers.
- Read `docs/features/MIDDLEWARE_AGENTS.md` if you need to orchestrate side effects based on selected state.
