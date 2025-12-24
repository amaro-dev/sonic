# Getting Started with Sonic

## Overview

This guide walks through the minimum steps required to adopt Sonic in a new feature: add the dependency, define the core
state manager, connect it to a renderer, and verify everything using the included samples. It targets developers who
have never touched the project before and want a reproducible checklist from clone to a working screen.

## Key Points

- Use Gradle to depend on the Sonic modules you need (`sonic-core`, `sonic-binding`, `sonic-result`, `sonic-compose`) or
  the published artifacts (preferably via the BoM).
- Every Sonic feature starts with a `StateManager`, a `Reducer`, and an `IRenderer`.
- `bindState` wires state updates to UI code and respects the coroutine scope you provide.
- The `app` module contains runnable samples (XML and Compose) that prove your toolchain is set up correctly.

## Prerequisites

- JDK 17+, Android Studio Giraffe+ (or IntelliJ IDEA) with Android SDK 34 installed.
- Gradle 8.x and Kotlin 1.9.x configured for your project.
- Basic knowledge of Kotlin coroutines and Flow.

## Workflow

1. **Install the dependency**
   ```kotlin
   // build.gradle.kts (module)
   dependencies {
       implementation(platform("dev.amaro.sonic:bom:0.6.0"))
       implementation("dev.amaro.sonic:core")
       implementation("dev.amaro.sonic:binding")
       implementation("dev.amaro.sonic:result")
       implementation("dev.amaro.sonic:compose")
       // or, when working inside this repo, depend on the matching modules directly
   }
   ```
   Sync the project to download the artifacts.

2. **Declare state, actions, and reducer**
   ```kotlin
   data class CounterState(val value: Int = 0)

   sealed class CounterAction : IAction {
       object Increment : CounterAction()
   }

   class CounterManager(scope: CoroutineScope) :
       StateManager<CounterState>(CounterState(), scope) {
       override val reducer = IReducer<CounterState> { action, current ->
           when (action) {
               CounterAction.Increment -> current.copy(value = current.value + 1)
               else -> current
           }
       }
   }
   ```

3. **Bind Sonic to your UI**
   ```kotlin
   class CounterFragment : Fragment(), IRenderer<CounterState> {
       private val manager by lazy {
           CounterManager(viewLifecycleOwner.lifecycleScope)
       }

       override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
           bindState(manager, this, viewLifecycleOwner.lifecycleScope)
       }

       override fun render(state: CounterState, performer: IPerformer<CounterState>) {
           textView.text = state.value.toString()
           button.setOnClickListener { performer.perform(CounterAction.Increment) }
       }
   }
   ```
   For Compose screens, collect state with `manager.listen()` and call `manager.perform(...)` directly.

4. **Verify with the sample app**
   ```bash
   ./gradlew :app:installDebug   # or :app:run for Desktop/CLI samples
   ```
   Launch either the legacy Notes flow (`NoteActivity`) or the Compose showcase (`NoteComposeActivity`). You should see
   persisted notes and the result banner updating in response to actions.

## Next Steps

- Read `docs/CONCEPTS.md` to internalize the Sonic vocabulary.
- Use `docs/features/*.md` to apply advanced helpers (binding patterns, result handling, selectors, middleware/agents,
  dispatcher tuning).
- Follow `docs/SAMPLE_WALKTHROUGH.md` to map those concepts onto running apps, and keep the README handy for high-level
  context.
