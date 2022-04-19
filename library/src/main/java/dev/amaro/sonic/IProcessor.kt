package dev.amaro.sonic

interface IProcessor<T> {
    fun perform(action: IAction)
    fun reduce(action: IAction)
}