package dev.amaro.sonic

import kotlinx.coroutines.flow.MutableStateFlow

interface IStateManager<T> {
    fun listen(): MutableStateFlow<T>
    fun process(action: IAction)
}