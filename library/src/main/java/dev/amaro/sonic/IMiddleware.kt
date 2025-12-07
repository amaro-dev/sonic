package dev.amaro.sonic

interface IMiddleware<T> {
   suspend fun process(action: IAction, state: T, processor: IProcessor<T>)
}

