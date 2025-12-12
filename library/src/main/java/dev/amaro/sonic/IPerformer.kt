package dev.amaro.sonic

/**
 * Functional interface exposed to renderers so they can dispatch new actions
 * without holding a reference to the entire [StateManager].
 *
 * Typically provided as an anonymous object inside `bindState`, but you can wrap
 * it for testing or headless renderers.
 */
interface IPerformer<T> {
    fun perform(action: IAction)
}
