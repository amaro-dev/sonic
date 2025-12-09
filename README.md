# Sonic - MVI Framework for Any Platform

A lightweight, composable state management framework for building apps in Kotlin. Use the same pattern on Android (
Views/Compose), Desktop (Compose), or CLI. No inheritance, no boilerplate, no platform lock-in.

**Status**: Phase 1 Complete · v0.6.0 · [Apache 2.0](LICENSE)

---

## Key Features

- ✅ **Composition over Inheritance** - Use `bindState()` function, not `Screen` class
- ✅ **Multi-Platform First** - Works identically on Android, Desktop, and CLI
- ✅ **Zero Framework Lock-in** - Works with your own UI (Fragment, Activity, Compose, Custom Views)
- ✅ **Explicit Lifecycle** - Control coroutine scope explicitly, no hidden magic
- ✅ **Easy Testing** - Just implement `IRenderer<T>`, no mocking complexity
- ✅ **Minimal Boilerplate** - State, Action, Reducer, Done. No extra layers.

---

## Quick Start by Platform

### Android Fragment

**New approach (v0.6+)**:

```kotlin
class NoteListFragment : Fragment(), IRenderer<NoteListState> {
    private val manager: NoteStateManager by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // One line: bind manager to renderer with lifecycle scope
        bindState(manager, this, viewLifecycleOwner.lifecycleScope)
    }

    override fun render(state: NoteListState, performer: IPerformer<NoteListState>) {
        // Update UI - lifecycle cleanup is automatic
        binding.notesList.adapter = NoteAdapter(state.notes)
        if (state.error != null) {
            showError(state.error)
        }
    }
}
```

**Key points**:

- ✅ No custom `Screen` subclass
- ✅ Lifecycle scope handles cleanup automatically
- ✅ Works with view binding or findViewById

---

### Android Activity

```kotlin
class MainActivity : AppCompatActivity(), IRenderer<AppState> {
    private val manager: AppStateManager by inject()
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Bind state manager to this activity's UI
        bindState(manager, this, lifecycleScope)
    }

    override fun render(state: AppState, performer: IPerformer<AppState>) {
        binding.title.text = state.title
        binding.button.setOnClickListener {
            performer.perform(Action.ButtonClicked)
        }
    }
}
```

---

### Android Jetpack Compose

```kotlin
@Composable
fun NoteListScreen(appGraph: AppGraph = LocalAppGraph.current) {
    val binding = rememberStateBinding(appGraph.noteManager)

    Column(modifier = Modifier.fillMaxSize()) {
        Text(binding.value.title, style = MaterialTheme.typography.headlineLarge)

        LazyColumn {
            items(binding.value.notes) { note ->
                NoteRow(
                    note = note,
                    onToggle = { binding.perform(Action.ToggleNote(note)) },
                    onDelete = { binding.perform(Action.DeleteNote(note)) }
                )
            }
        }
    }
}

@Composable
fun rememberStateBinding(manager: IStateManager<NoteListState>): StateBinding<NoteListState> {
    val state by manager.listen().collectAsState()
    return StateBinding(state, manager::perform)
}
```

---

### Desktop Compose

```kotlin
fun main() = application {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val manager = AppStateManager(initialState = AppState(), scope = scope)

    // CRITICAL: Use Dispatchers.Default on Desktop (not Main)
    bindState(manager, DesktopRenderer(), scope, Dispatchers.Default)

    Window(onCloseRequest = ::exitApplication) {
        // Compose UI here
        AppContent(manager)
    }
}

class DesktopRenderer : IRenderer<AppState> {
    override fun render(state: AppState, performer: IPerformer<AppState>) {
        println("App state: ${state.title}")
    }
}
```

**⚠️ Important**: Desktop doesn't have `Dispatchers.Main`. Always override to `Dispatchers.Default`.

---

### CLI/Headless

```kotlin
fun main() = runBlocking {
    val manager = AppStateManager(initialState = AppState(), scope = this)

    bindState(
        manager,
        ConsoleRenderer(),
        this,
        Dispatchers.Default  // Critical: not Main
    )

    delay(Long.MAX_VALUE)  // Keep alive
}

class ConsoleRenderer : IRenderer<AppState> {
    override fun render(state: AppState, performer: IPerformer<AppState>) {
        println(">>> State: ${state.title}")
        print("> ")
        val input = readLine() ?: return
        when (input) {
            "inc" -> performer.perform(Action.Increment)
            "quit" -> System.exit(0)
        }
    }
}
```

---

## Architecture Comparison

| Aspect          | Old (Screen)                 | New (bindState)                                  |
|-----------------|------------------------------|--------------------------------------------------|
| **Pattern**     | Inheritance                  | Composition                                      |
| **Core Class**  | `Screen<T>` base class       | `bindState()` function                           |
| **Setup**       | Extend Screen, magic happens | Implement IRenderer, call bindState()            |
| **Lifecycle**   | Hidden in Screen base class  | Explicit: you pass the scope                     |
| **Platforms**   | Android-focused              | All platforms equal (Android/Desktop/CLI)        |
| **Testing**     | Mock base class (hard)       | Implement IRenderer (easy)                       |
| **Flexibility** | Locked to Screen pattern     | Works with Fragment/Activity/Compose/Custom View |
| **Boilerplate** | Higher                       | Lower                                            |

---

## Core Concepts

### IStateManager\<T\>

The state holder and action processor. Emits state changes as a Flow.

```kotlin
val manager: IStateManager<MyState> = MyStateManager(...)
manager.listen()  // Returns Flow<MyState>
manager.perform(Action.DoSomething)  // Process action
```

### IRenderer\<T\>

Your UI that responds to state changes. Implement once per screen/view.

```kotlin
class MyFragment : Fragment(), IRenderer<MyState> {
    override fun render(state: MyState, performer: IPerformer<MyState>) {
        // Update UI based on state
        // Call performer.perform(action) for user interactions
    }
}
```

### StateBinding\<T\>

A simple data class that pairs state with an action dispatcher. Useful for Compose.

```kotlin
data class StateBinding<T>(
    val value: T,
    val perform: (IAction) -> Unit
)
```

### bindState()

The core function that connects a manager to a renderer using a coroutine scope.

```kotlin
bindState(
    manager: IStateManager<T>,
    renderer: IRenderer<T>,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
): Job
```

When the scope is cancelled, rendering automatically stops.

---

## Why bindState() is Better

### 1. Composition Over Inheritance

You own your class hierarchy. Framework doesn't dictate your base class.

```kotlin
// ✅ Works with Fragment
class MyFragment : Fragment(), IRenderer<MyState> { }

// ✅ Works with Activity
class MainActivity : AppCompatActivity(), IRenderer<MyState> { }

// ✅ Works with Compose
@Composable
fun MyScreen() { }

// ✅ Works with custom View
class MyCustomView : FrameLayout(context), IRenderer<MyState> { }
```

### 2. Explicit Lifecycle Control

You control the scope - the framework respects it.

```kotlin
// Android Fragment: Auto-cleanup when fragment destroyed
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    bindState(manager, this, viewLifecycleOwner.lifecycleScope)
}

// Desktop: You manage scope explicitly
val scope = CoroutineScope(Dispatchers.Default + Job())
bindState(manager, renderer, scope, Dispatchers.Default)
// Later: scope.cancel()
```

### 3. Platform-Agnostic

Same pattern, different dispatcher settings per platform.

| Platform   | Dispatcher                             | Notes                              |
|------------|----------------------------------------|------------------------------------|
| Android UI | `Dispatchers.Main.immediate` (default) | Must update UI on main thread      |
| Desktop    | `Dispatchers.Default`                  | No main thread on desktop          |
| CLI        | `Dispatchers.Default`                  | Non-blocking alternative available |

### 4. Easy Testing

No inheritance mocking - just implement IRenderer in tests.

```kotlin
@Test
fun `myFragment renders state`() = runTest {
    val manager = FakeStateManager()
    val renderer = FakeRenderer()

    bindState(manager, renderer, this, Dispatchers.Default)
    advanceUntilIdle()

    assertEquals(expectedState, renderer.lastRenderedState)
}
```

### 5. No Hidden Boilerplate

What you see is what you get. No hidden lifecycle.

```kotlin
// That's it:
// 1. Implement IRenderer<T>
// 2. Call bindState() once
// 3. Write render() method
```

---

## Platform-Specific Guides

For detailed patterns, examples, and best practices per platform, see:

- **[Android (XML/View-based)](docs/PLATFORM_GUIDES.md#android-xmlview-based)** - Fragment, Activity patterns with
  lifecycle management
- **[Android Jetpack Compose](docs/PLATFORM_GUIDES.md#android-jetpack-compose)** - StateBinding patterns,
  collectAsState(), previews
- **[Desktop Compose](docs/PLATFORM_GUIDES.md#desktop-compose)** - Explicit scope, dispatcher override, window lifecycle
- **[CLI/Headless](docs/PLATFORM_GUIDES.md#cliheadless-applications)** - runBlocking, console rendering, input handling

Each guide includes:

- ✅ Core pattern explanation
- ✅ Complete working examples
- ✅ Key points to remember
- ✅ Common pitfalls and solutions
- ✅ Troubleshooting

---

## Migrating from Screen (v0.6+)

The old `Screen` class approach is deprecated but still works. You have time to migrate:

| Version  | Status                                         | Timeline       |
|----------|------------------------------------------------|----------------|
| **v0.6** | `@Deprecated(WARNING)` - Safe to migrate       | Now - 6 months |
| **v1.0** | `@Deprecated(ERROR)` - Escape hatch available  | 6-12 months    |
| **v1.5** | Removed from sonic-core, moved to sonic-legacy | 12+ months     |

**For detailed migration steps**, see [MIGRATION_GUIDE.md](docs/MIGRATION_GUIDE.md)

Quick migration example:

```kotlin
// OLD: Screen-based
class NoteListScreen(manager: NoteStateManager) : Screen<NoteListState>(manager) {
    override fun render(state: NoteListState, performer: IPerformer<NoteListState>) {
        updateUI(state)
    }
}

// NEW: bindState-based
class NoteListFragment : Fragment(), IRenderer<NoteListState> {
    private val manager: NoteStateManager by inject()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindState(manager, this, viewLifecycleOwner.lifecycleScope)  // ← Add this line
    }

    override fun render(state: NoteListState, performer: IPerformer<NoteListState>) {
        updateUI(state)  // ← Same code as before
    }
}
```

**Key insight**: The `render()` method is identical. Only the setup changes.

Migration time estimate: **< 1 hour per screen**

---

## Sample Applications

Three complete sample apps demonstrating the new approach:

### Calculator Sample

**Location**: `app/src/main/java/dev/amaro/sonic/app/samples/calculator/`

Demonstrates:

- ✅ Android Activity with state binding
- ✅ Jetpack Compose UI
- ✅ Terminal/CLI renderer
- ✅ Multiple renderer implementations from same manager

### Currency Converter Sample

**Location**: `app/src/main/java/dev/amaro/sonic/app/samples/converter/`

Demonstrates:

- ✅ Android Fragment pattern
- ✅ Spinner/UI binding
- ✅ State management across fragments
- ✅ Flow-based input collection

### Notes Sample

**Location**: `app/src/main/java/dev/amaro/sonic/app/samples/notes/`

Demonstrates:

- ✅ Complete CRUD operations
- ✅ RecyclerView rendering
- ✅ Navigation between screens
- ✅ Persistent state management
- ✅ Custom View with IRenderer

All samples use the new `bindState()` approach, not deprecated `Screen` class.

---

## Installation

Add Sonic to your project:

```gradle
dependencies {
    implementation 'dev.amaro:sonic:0.6.0'
}
```

**For Android projects** (most common):

```gradle
dependencies {
    implementation 'dev.amaro:sonic:0.6.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1'
}
```

**For Desktop/CLI projects**:

```gradle
dependencies {
    implementation 'dev.amaro:sonic:0.6.0'
    // Android coroutines dispatcher not needed
}
```

---

## API Quick Reference

### bindState() - Core Function

```kotlin
fun <T> bindState(
    manager: IStateManager<T>,
    renderer: IRenderer<T>,
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
): Job
```

**Parameters**:

- `manager`: State manager providing state emissions and action handling
- `renderer`: Implementation of IRenderer to receive state updates
- `scope`: CoroutineScope controlling binding lifetime (cancelling scope stops rendering)
- `dispatcher`: Where to render (default `Main.immediate` for Android, override to `Default` for Desktop/CLI)

**Returns**: Job representing the binding

---

### StateBinding\<T\> - State + Action Carrier

```kotlin
data class StateBinding<T>(
    val value: T,           // Current state
    val perform: (IAction) -> Unit  // Action dispatcher
)
```

Useful in Compose for passing both state and callbacks to composables.

---

### IRenderer\<T\> - Your UI Interface

```kotlin
interface IRenderer<T> {
    fun render(state: T, performer: IPerformer<T>)
}
```

Implement this interface in your Fragment, Activity, Composable, or custom View.

---

### IStateManager\<T\> - State Provider

```kotlin
interface IStateManager<T> {
    fun listen(): Flow<T>
    fun perform(action: IAction)
}
```

Created by extending `StateManager<T>` base class.

---

## Documentation

| Document                                                | Purpose                                          | Audience                 |
|---------------------------------------------------------|--------------------------------------------------|--------------------------|
| **[MIGRATION_GUIDE.md](docs/MIGRATION_GUIDE.md)**       | Step-by-step migration from Screen to bindState  | Existing users upgrading |
| **[PLATFORM_GUIDES.md](docs/PLATFORM_GUIDES.md)**       | Detailed platform-specific patterns              | All developers           |
| **[MODERNIZATION_PLAN.md](docs/MODERNIZATION_PLAN.md)** | Full architecture rationale and Phase 2+ roadmap | Tech leads, maintainers  |

API Documentation: [KDoc comments in source code](library/src/main/java/dev/amaro/sonic/)

---

## Key Dispatcher Selection Guide

The most common mistake when starting on Desktop or CLI is using the wrong dispatcher. Here's the quick guide:

**Android** → Use `Dispatchers.Main.immediate` (default, safe)

```kotlin
bindState(manager, renderer, scope)  // Main.immediate is default
```

**Desktop Compose** → MUST override to `Dispatchers.Default`

```kotlin
bindState(manager, renderer, scope, Dispatchers.Default)  // ⚠️ Critical
```

**CLI/Headless** → Use `Dispatchers.Default`

```kotlin
bindState(manager, renderer, scope, Dispatchers.Default)
```

**Why**: Android requires UI updates on the main thread. Desktop and CLI have no main thread concept.

---

## License

Sonic is licensed under the [Apache License 2.0](LICENSE)

## Contributing

Contributions welcome! Please see [Contributing Guidelines](CONTRIBUTING.md)

## Status

- **Current version**: 0.6.0
- **Status**: Phase 1 Complete (bindState foundation, Screen deprecation)
- **Next**: Phase 2 (Rich error handling, middleware helpers like Retry and Cache)
