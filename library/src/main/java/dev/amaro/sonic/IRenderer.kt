package dev.amaro.sonic

/**
 * Contract implemented by UI components (Fragments, Activities, Compose functions,
 * CLI renderers) to receive state updates plus an [IPerformer] for dispatching actions.
 *
 * Sonic calls [render] on the dispatcher you configured via `bindState`; keep it fast
 * and side-effect free (UI work only).
 *
 * For headless tests or pipelines that only care about middleware/agents, use [Nothing],
 * a no-op renderer.
 */
interface IRenderer<T> {
    fun render(state: T, performer: IPerformer<T>)

    /**
     * No-op renderer useful for tests or background managers that do not drive a UI.
     */
    class Nothing<T> : IRenderer<T> {
        override fun render(state: T, performer: IPerformer<T>) = Unit
    }
}
