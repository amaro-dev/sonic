# Sample Walkthrough

## Overview

The `app` module contains three sample workflows that showcase different Sonic capabilities. Use this guide to
understand what each sample teaches and how to run it.

## Key Points

- Samples live under `app/src/main/java/dev/amaro/sonic/app/samples`.
- All samples reuse the same Sonic modules; only the UI layer changes.
- Running them locally is the fastest way to verify your Sonic installation.

## Prerequisites

- Android Studio with an emulator or device for the Android samples.
- JDK 17+ for CLI or Desktop runs.
- `./gradlew assembleDebug` succeeds.

## Samples

1. **Calculator (`samples/calculator/`)**
    - Demonstrates: basic `StateManager`, `IMiddleware`, CLI renderer, Android Activity, and Compose Activity.
    - Files of interest: `Calculator.kt`, `ActivityRenderer.kt`, `ComposeRenderer.kt`, `TerminalRenderer.kt`.
    - Run:
        - Android Activity: `./gradlew :app:installDebug` → launch `MainActivity`.
        - Compose Activity: launch `ComposeActivity`.
        - CLI: execute `./gradlew :app:run` with the `mainClass` pointing to the terminal renderer.

2. **Converter (`samples/converter/`)**
    - Demonstrates: multi-middleware pipelines, server-side style validation, and `ResultInfo` usage.
    - Files: `Converter.kt`, `FirstFragment.kt`, `SecondFragment.kt`.
    - Run: `./gradlew :app:installDebug` → open “Converter” launcher icon (defined in `AndroidManifest.xml`).

3. **Notes (`samples/notes/`)**
    - Demonstrates: storage-backed CRUD, navigation middleware (`Navigator.kt`), `ResultInfo` + `ResultClearingAgent`,
      and Jetpack Compose selectors.
    - XML flow: launch `NoteActivity` (View-based screens).
    - Compose showcase: launch `NoteComposeActivity` to see `NoteComposeRoot`, CompositionLocals, selectors, and
      auto-clearing result banners.

## Next Steps

- Use these samples as blueprints when bootstrapping new features.
- Cross-reference with `docs/features/*.md` to understand which concepts each sample exercises.
- When modifying the modules, update samples alongside the docs to keep parity.
