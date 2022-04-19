package dev.amaro.sonic

interface IMiddleware<T> {
    fun process(action: IAction, state: T, processor: IProcessor<T>)
}

