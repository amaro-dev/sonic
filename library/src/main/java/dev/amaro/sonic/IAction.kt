package dev.amaro.sonic

/**
 * Marker interface for every intent flowing through Sonic's unidirectional loop.
 *
 * Actions describe **what** happened (user input, lifecycle event, middleware
 * callback), not **how** to handle it. Prefer sealed classes per feature so the compiler
 * can exhaustively check reducers and middleware.
 *
 * Example:
 * ```kotlin
 * sealed class Action : IAction {
 *     object Load : Action()
 *     data class Toggle(val id: String) : Action()
 * }
 * ```
 *
 * @see ISideEffectAction for actions that wrap follow-up work.
 */
interface IAction

/**
 * Extension of [IAction] that instructs the [StateManager] to dispatch a secondary action
 * immediately after the reducer finishes.
 *
 * Use this sparingly for simple follow-up work; complex chains belong in middleware or agents.
 */
interface ISideEffectAction : IAction {
    val sideEffect: IAction
}
