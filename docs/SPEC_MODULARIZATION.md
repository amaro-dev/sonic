# Sonic Modularization Spec

## Objective

- Split the existing monolithic `library` module into clear Gradle modules so consumers only depend on the Sonic
  features they need while keeping a shared foundational core for Android Views, Compose, Desktop, and CLI.
- Publish every module to Maven Central and provide a BoM that pins compatible versions for adopters who want the full
  toolkit.

## Target Modules

1. `sonic-core`
    - Contracts and engine for the unidirectional data loop (`IAction`, `IReducer`, `IStateManager`, `StateManager`,
      middleware interfaces and defaults, processor utilities).
    - Depends on Kotlin + `kotlinx-coroutines-core` only.
2. `sonic-binding`
    - Rendering contracts and UI-agnostic helpers: `IRenderer`, `IPerformer`, `bindState`, Flow extensions (
      `collectOn*`, `withAgent*`), `StateBinding`, `StateSelectors`, documentation-aligned utilities.
    - Depends on `sonic-core`.
3. `sonic-result`
    - The result/status toolkit: `ResultInfo`, `ResultBuilder`, `success`/`failure` helpers, `ResultClearingAgent`,
      `ResultClearingConfig`, `IStatusContainer`, plus accompanying tests.
    - Depends on `sonic-core` and coroutines for the agent’s scheduling.
4. `sonic-compose`
    - Compose-only integrations such as `StateManagerContext`, `LocalStateManager`, future `remember*` helpers, and any
      compose-specific adapters now located under `app/compose`.
    - Depends on `sonic-binding` (and transitively on `sonic-core`) plus Compose runtime artifacts.
5. `sonic-bom`
    - A Bill of Materials referencing the four modules above so projects can import aligned versions with
      `platform("dev.amaro.sonic:sonic-bom:<version>")`.

## Deliverables

- New module entries in `settings.gradle` and Gradle build logic for each module + BoM, including detekt/publishing
  configuration.
- Source files moved into the module that matches their responsibility while keeping package names stable where possible
  to minimize migration pain.
- Updated Gradle dependencies so:
    - `sonic-binding` → `sonic-core`
    - `sonic-result` → `sonic-core`
    - `sonic-compose` → `sonic-binding`
    - `sonic-bom` declares the coordinates for all publishable artifacts.
- Sample `app` module and tests re-wired to the new module graph without referencing the old monolith.
- Documentation updates (README, Getting Started, Concepts, API reference, feature guides) describing the new artifacts,
  BoM usage, and guidance on which module to choose for each scenario.
- Release notes or changelog entry highlighting the modularization for v1.0 consumers.

## Acceptance Criteria

- `./gradlew assemble` (and relevant sample tasks) succeed using the new module structure.
- No module imports implementation details from another module beyond declared dependencies.
- Public API compatibility is preserved as much as possible; any breaking namespace moves are documented with migration
  tips.
- Unit tests execute from their new module homes.
- Maven Central publication configuration covers every module plus the BoM, aligned with existing publishing
  conventions.

## Approach

1. Create empty Gradle modules for `sonic-core`, `sonic-binding`, `sonic-result`, `sonic-compose`, and the `sonic-bom`
   project.
2. Gradually move source sets into the appropriate module, adjusting imports only when cross-module access changes.
3. Update the sample `app` and tests to depend on the new artifacts.
4. Add BoM definitions referencing the published coordinates of each Sonic artifact.
5. Refresh documentation and release notes with installation instructions, module descriptions, and BoM usage examples.
6. Validate the entire build, run sample apps, and ensure publication metadata is correct.

## Task Breakdown

1. **Bootstrap Modules & Build Logic**
    - Update `settings.gradle` to include the new module names plus the BoM project.
    - Scaffold `build.gradle(.kts)` files for each module, mirroring shared configuration (plugins, Kotlin options,
      detekt, publishing hooks) and declaring placeholder dependencies.
    - Ensure each module has source/test directories so later moves don’t fail Gradle sync.

2. **Restructure Core APIs into `sonic-core`**
    - Relocate contracts and the state engine (`IAction`, `IReducer`, `IStateManager`, `IMiddleware`, `IProcessor`,
      `StateManager`, `Processor`, `DirectMiddleware`, `ConditionedDirectMiddleware`, etc.) into the `sonic-core` source
      set.
    - Verify imports across the repo now point to the new module; adjust Gradle dependencies (sample app, other modules)
      to compile.
    - Confirm `sonic-core` has no Android or Compose dependencies beyond coroutines.

3. **Populate `sonic-binding` with Rendering Helpers**
    - Move rendering interfaces (`IRenderer`, `IPerformer`), binding utilities (`Binding`, `StateBinding`,
      `SonicUiState` typealias), Flow helpers (`collectOn*`, `withAgent*`), and selector extensions into this module.
    - Add a dependency on `sonic-core` and expose the API for other modules/apps.
    - Update usages throughout the repo—including docs—to reference the new module when discussing binding utilities.

4. **Create `sonic-result` Module**
    - Transfer the result/status toolkit files (`ResultInfo`, `ResultBuilder`, helper functions, `ResultClearingAgent`,
      `ResultClearingConfig`, `IStatusContainer`) plus unit tests into `sonic-result`.
    - Wire module dependencies (`sonic-result` → `sonic-core`) and update references in app code.
    - Ensure documentation for result handling points to the new artifact.

5. **Extract Compose Integrations into `sonic-compose`**
    - Move `StateManagerContext`, `LocalStateManager`, and any other Compose-only helpers from `app/compose` (and docs)
      into a new `sonic-compose` module.
    - Add dependencies on `sonic-binding` and Compose runtime libraries; update the sample Compose screens to import
      from the module rather than `app/`.
    - Document the module’s scope (Compose-specific helpers only).

6. **Define `sonic-bom`**
    - Add a BoM project that publishes `platform` coordinates listing the aligned versions of `sonic-core`,
      `sonic-binding`, `sonic-result`, and `sonic-compose`.
    - Configure publishing metadata so the BoM reaches Maven Central alongside the other artifacts.
    - Provide doc snippets showing how to consume the BoM (`implementation(platform("...:sonic-bom:<version>"))`).

7. **Update Sample App & Tests**
    - Adjust the `app` module’s Gradle dependencies to pull from the new modules instead of the old `library`.
    - Fix imports in sample source files and unit tests to reflect the new module package arrangements.
    - Run relevant app targets (XML, Compose, CLI) to ensure runtime behavior is unchanged.

8. **Documentation & Release Notes**
    - Revise README, Getting Started, Concepts, API Reference, and feature guides with updated dependency instructions (
      module-specific artifacts and BoM example).
    - Add a release note/changelog entry summarizing the modularization and migration guidance for existing consumers.
    - Mention the module purposes (when to use each) to help adopters choose dependencies.

9. **Validation & Publishing Prep**
    - Execute `./gradlew assemble` (plus sample runs) to ensure the multi-module graph builds cleanly.
    - Run unit tests now spread across modules to confirm coverage still applies.
    - Validate publishing tasks for all artifacts, including the BoM, prior to the v1.0 release.

## Open Questions / Follow-Ups

- Confirm final Maven coordinates (group/artifact IDs) for each module and the BoM before publishing.
- Decide whether future platform-specific helpers (e.g., Desktop, CLI) warrant their own modules once implementations
  exist.
