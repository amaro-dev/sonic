package dev.amaro.sonic

import kotlinx.coroutines.flow.StateFlow

interface IStateManager<T> {
    fun listen(): StateFlow<T>
    fun process(action: IAction)
}