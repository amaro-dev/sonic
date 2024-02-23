package dev.amaro.sonic

interface IReducer<T> {
    fun reduce(action: IAction, currentState: T): T
}