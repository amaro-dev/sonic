package dev.amaro.sonic

import kotlinx.coroutines.flow.StateFlow

interface IStateManager<T> {
    fun listen(): StateFlow<T>
    fun perform(action: IAction)
}