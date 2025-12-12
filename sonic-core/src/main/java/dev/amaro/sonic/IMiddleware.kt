package dev.amaro.sonic

/**
 * Intercepts actions **before** they reach the reducer.
 *
 * Middleware can:
 * - Short-circuit actions (do nothing).
 * - Dispatch alternative actions via [IProcessor.perform].
 * - Commit state changes directly via [IProcessor.reduce].
 * - Run arbitrary side effects (network, storage, logging).
 *
+ The chain executes in the order middleware were added to the [StateManager].
 *
 * Example logging middleware:
 * ```kotlin
 * class LoggingMiddleware<T>(
 *     private val log: (IAction) -> Unit
 * ) : IMiddleware<T> {
 *     override suspend fun process(action: IAction, state: T, processor: IProcessor<T>) {
 *         log(action)
 *         processor.perform(action) // Continue
 *     }
 * }
 * ```
 */
interface IMiddleware<T> {
    suspend fun process(action: IAction, state: T, processor: IProcessor<T>)
}
