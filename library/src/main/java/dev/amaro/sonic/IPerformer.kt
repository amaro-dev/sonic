package dev.amaro.sonic

interface IPerformer<T> {
    fun perform(action: IAction)
}