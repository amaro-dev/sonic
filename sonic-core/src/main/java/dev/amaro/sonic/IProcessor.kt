package dev.amaro.sonic

/**
 * Bridge object passed to middleware so they can either continue the action chain
 * or commit state changes directly.
 *
+ [perform] re-enters the middleware pipeline, eventually reaching the reducer.
+ [reduce] skips middleware and applies the reducer immediately (commonly used
 *   for validation or derived actions created inside middleware).
 */
interface IProcessor<T> {
    fun perform(action: IAction)
    fun reduce(action: IAction)
}
