package dev.amaro.sonic

interface IAction

interface ISideEffectAction : IAction {
    val sideEffect: IAction
}