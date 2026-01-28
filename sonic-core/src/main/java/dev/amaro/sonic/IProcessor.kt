package dev.amaro.sonic

/**
 * Bridge object passed to middleware so they can either continue the action chain
 * or commit state changes directly.
 *
+ [perform] re-enters the middleware pipeline, eventually reaching the reducer.
+ [reduce] skips middleware and applies the reducer immediately (commonly used
 *   for validation or derived actions created inside middleware).
+ [schedule] queues an action to execute after the next reduction completes.
 */
interface IProcessor<T> {
    fun perform(action: IAction)
    fun reduce(action: IAction)

    /**
     * Schedule an action to execute after the next reduction completes.
     *
     * Scheduled actions are queued and executed in FIFO order after the
     * reducer produces the next state. They re-enter the middleware pipeline
     * via [perform], allowing middleware to intercept them.
     *
     * **Execution Order:** state update → scheduled actions → side effects
     *
     * **Use Cases:**
     * - Dispatch follow-up actions based on reduction results
     * - Derived actions that depend on state changes
     * - Validation actions that check post-reduction state
     *
     * **Example:**
     * ```kotlin
     * override suspend fun process(action: IAction, state: T, processor: IProcessor<T>) {
     *     if (action is ValidateAction) {
     *         processor.reduce(action)
     *         processor.schedule(CheckValidationResult)
     *     } else {
     *         processor.perform(action)
     *     }
     * }
     * ```
     */
    fun schedule(action: IAction)
}
