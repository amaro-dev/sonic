# Dispatcher & Scope Guidance

## Overview

Sonic never hides threads from you—the dispatcher you pass into `bindState` (or use while collecting) determines where
rendering and agents run. This guide documents the recommended dispatcher/scope combinations per platform and how to
avoid common pitfalls.

## Key Points

- Android UI must use `Dispatchers.Main.immediate` (the default).
- Desktop Compose and CLI do not provide a Main dispatcher; use `Dispatchers.Default`.
- Always tie scopes to real lifecycles (activities, fragments, windows, custom supervisors).

## Prerequisites

- Comfortable with Kotlin coroutines and structured concurrency.
- Familiar with `bindState` from `docs/features/BINDING.md`.

## Workflow

1. **Android (Fragments/Activities)**
   ```kotlin
   bindState(
       manager = manager,
       renderer = this,
       scope = viewLifecycleOwner.lifecycleScope // or lifecycleScope for activities
   )
   ```
    - Default dispatcher is correct; Sonic ensures rendering happens on Main.
    - Cancelled automatically when lifecycle scope ends (`onDestroyView` or `onDestroy`).

2. **Android Compose (Accompanist/Activities)**
    - Use `rememberCoroutineScope()` for one-off bindings or rely on `collectAsState()` which already uses Main.
    - When launching agents with `LaunchedEffect`, the default Compose scope is also Main.

3. **Desktop Compose**
   ```kotlin
   val desktopScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
   val job = bindState(
       manager = manager,
       renderer = DesktopRenderer(),
       scope = desktopScope,
       dispatcher = Dispatchers.Default
   )
   window.onCloseRequest = {
       job.cancel()
       desktopScope.cancel()
   }
   ```
    - Never rely on `Dispatchers.Main`; it throws.

4. **CLI / Headless**
   ```kotlin
   fun main() = runBlocking {
       val manager = CounterManager(this)
       bindState(manager, ConsoleRenderer(), this, Dispatchers.Default)
       awaitCancellation()
   }
   ```
    - `runBlocking` scope owns the binding. Cancel it when terminating.

5. **Compose Multiplatform targets**
    - Mirror Desktop guidance: create explicit scopes per window/screen and pass `Dispatchers.Default`.

## Tips

- Prefer `SupervisorJob()` when creating custom scopes so one failing child doesn’t cancel all renderers.
- When background work needs IO, keep it inside middleware/agents using `withContext(Dispatchers.IO)`; do not change the
  binding dispatcher.
- Label scopes (e.g., `CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName("SonicDesktop"))`) to
  simplify debugging.

## Next Steps

- Review `docs/features/BINDING.md` for concrete binding snippets.
- Combine this guidance with the middleware/agent patterns in `docs/features/MIDDLEWARE_AGENTS.md` when launching side
  effects.
