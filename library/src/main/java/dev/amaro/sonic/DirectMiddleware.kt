package dev.amaro.sonic

/**
 * Directly reduces any action. Useful for applications that doesn't need any access to time
 * consuming APIs or heavy calculations
 */
class DirectMiddleware<T> : IMiddleware<T> {
    override fun process(action: IAction, state: T, processor: IProcessor<T>) {
        processor.reduce(action)
    }
}