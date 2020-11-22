package dev.amaro.sonic

interface IMiddleware<T> {
    fun process(action: IAction, state: T, processor: IProcessor<T>)
}

class DirectMiddleware<T> : IMiddleware<T> {
    override fun process(action: IAction, state: T, processor: IProcessor<T>) {
        processor.reduce(action)
    }
}