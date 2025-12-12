package dev.amaro.sonic

import kotlin.reflect.KClass

/**
 * Convenience middleware that only lets specific action types reach [DirectMiddleware].
 *
 * Useful when you want to reuse DirectMiddleware behavior (e.g., dispatching follow-up actions)
 * but restrict it to a subset of actions without adding guards inside the base class.
 *
 * Example:
 * ```kotlin
 * val middleware = ConditionedDirectMiddleware<NoteState>(Action.DeleteNote::class)
 * manager.addMiddleware(middleware)
 * ```
 */
class ConditionedDirectMiddleware<T>(
    private vararg val actions: KClass<*>
) : DirectMiddleware<T>() {
    override suspend fun process(action: IAction, state: T, processor: IProcessor<T>) {
        if (actions.contains(action::class)) {
            super.process(action, state, processor)
        }
    }
}
