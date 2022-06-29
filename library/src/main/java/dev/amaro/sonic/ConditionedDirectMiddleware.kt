package dev.amaro.sonic

import kotlin.reflect.KClass


class ConditionedDirectMiddleware<T>(
    private vararg val actions: KClass<*>
): DirectMiddleware<T>() {
    override fun process(action: IAction, state: T, processor: IProcessor<T>) {
        if (actions.contains(action::class)) {
            super.process(action, state, processor)
        }
    }
}